/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.java.checks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.CharsetAwareVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.RuleTemplate;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Krzysztof Suszyński <krzysztof.suszynski@wavesoftware.pl>
 * @since 2015-08-21
 */
@Rule(
        key = "RegularExpressionSimpleCheck",
        tags = {"custom"},
        name = "Regular expression check should be fulfilled",
        priority = Priority.MAJOR)
@RuleTemplate
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("10min")
public class RegularExpressionSimpleCheck extends SubscriptionBaseVisitor implements CharsetAwareVisitor {

    @RuleProperty(description = "Mandatory. The regular expession pattern to be search")
    protected String pattern = "";
    @RuleProperty(description = "Optional. If set rule will pass check if metch is not found. By default " +
            "this is false and possitive search is performed. Default: false")
    protected boolean invertMode = false;
    @RuleProperty(description = "Optional. The message that will be registered as issue if positive check fails")
    protected String issueMessage = "Text \"${" + MATCH_KEY + "}\" matches given regular expression \"${" + REGEX_KEY + "}\"";
    @RuleProperty(description = "Optional. The message that will be registered as issue if invert mode check fails")
    protected String invertModeIssueMessage = "File do not contain requested text, that should match given " +
            "regular expression \"${" + REGEX_KEY + "}\"";
    private Charset charset;

    private static final String REGEX_KEY = "REGEX";
    private static final String MATCH_KEY = "MATCH";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.emptyList();
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
        try {
            super.context = context;
            super.scanFile(context);
            if (context.getSemanticModel() != null) {
                visitContext(context);
            }
        } catch (IOException | PatternSyntaxException e) {
            throw new IllegalArgumentException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void setCharset(Charset charset) {
        this.charset = checkNotNull(charset);
    }

    private void visitContext(JavaFileScannerContext context) throws IOException, PatternSyntaxException {
        validatePattern();
        String content = readFile(context.getFile());
        Pattern patternObj = Pattern.compile(pattern);
        Matcher matcher = patternObj.matcher(content);
        Iterable<MatchResult> resultIterable = matches(matcher);
        List<MatchResult> results = Lists.newArrayList(resultIterable);
        boolean atLeastOne = results.iterator().hasNext();
        if (atLeastOne) {
            addIssuesOnFoundMatches(content, results);
        } else if (invertMode) {
            addIssueOnFile(Formatter.createFrom(invertModeIssueMessage).setValue(REGEX_KEY, pattern).format());
        }
    }

    private static Iterable<MatchResult> matches(final Matcher matcher) {
        return new Iterable<MatchResult>() {
            public Iterator<MatchResult> iterator() {
                return new MatchResultIterator(matcher);
            }
        };
    }

    private void validatePattern() {
        if (pattern == null || pattern.isEmpty()) {
            throw new PatternSyntaxException("Regular expression given must not be empty!", pattern, 0);
        }
    }

    private void addIssuesOnFoundMatches(String content, Iterable<MatchResult> resultIterable) {
        for (MatchResult matchResult : resultIterable) {
            int start = matchResult.start();
            String match = matchResult.group();
            int lineNo = getLineNumer(start, content);
            String message = Formatter.createFrom(issueMessage)
                    .setValue(MATCH_KEY, match)
                    .setValue(REGEX_KEY, pattern)
                    .format();
            addIssue(lineNo, message);
        }
    }

    private int getLineNumer(int start, String content) {
        String sub = content.substring(0, start);
        return sub.split("\n").length;
    }

    private String readFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new String(bytes, charset);
    }

    private static class Formatter {
        private final String format;
        private final Map<String, Object> map = Maps.newHashMap();

        public static Formatter createFrom(String format) {
            return new Formatter(format);
        }

        private Formatter(String format) {
            this.format = checkNotNull(format);
        }

        public Formatter setValue(String key, Object value) {
            this.map.put(checkNotNull(key).toUpperCase(), checkNotNull(value));
            return this;
        }

        public String format() {
            String message = format;
            Matcher matcher = Pattern.compile("\\$\\{([^\\}]+)\\}").matcher(format);
            Iterable<MatchResult> matches = matches(matcher);
            for (MatchResult match : matches) {
                String key = match.group(1).toUpperCase();
                String all = match.group(0);
                String value = map.containsKey(key) ? map.get(key).toString() : "";
                message = message.replace(all, value);
            }
            return message;
        }
    }

    protected static class MatchResultIterator implements Iterator<MatchResult> {
        // Keep a match around that supports any interleaving of hasNext/next calls.
        MatchResult pending;

        private final Matcher matcher;

        public MatchResultIterator(final Matcher matcher) {
            this.matcher = matcher;
        }

        public boolean hasNext() {
            // Lazily fill pending, and avoid calling find() multiple times if the
            // clients call hasNext() repeatedly before sampling via next().
            if (pending == null && matcher.find()) {
                pending = matcher.toMatchResult();
            }
            return pending != null;
        }

        public MatchResult next() {
            // Fill pending if necessary (as when clients call next() without
            // checking hasNext()), throw if not possible.
            if (!hasNext()) { throw new NoSuchElementException(); }
            // Consume pending so next call to hasNext() does a find().
            MatchResult next = pending;
            pending = null;
            return next;
        }

        /** Required to satisfy the interface, but unsupported. */
        public void remove() { throw new UnsupportedOperationException(); }
    }

}
