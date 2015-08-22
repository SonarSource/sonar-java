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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.helpers.ExceptionMatcher;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import java.io.File;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Krzysztof Suszyński <krzysztof.suszynski@wavesoftware.pl>
 * @since 2015-08-22
 */
public class RegularExpressionSimpleCheckTest {

    private final RegularExpressionSimpleCheck check = new RegularExpressionSimpleCheck();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testNodesToVisit() {
        assertThat(check.nodesToVisit()).isNotNull();
        assertThat(check.nodesToVisit()).isEmpty();
    }

    @Test
    public void testSetCharset_NPE() {
        expectedException.expect(NullPointerException.class);
        Charset charset = null;
        check.setCharset(charset);
    }

    @Test
    public void testSetCharset() {
        check.setCharset(Charset.defaultCharset());
        assertThat(check).isNotNull();
    }

    @Test
    public void testInvalidPattern() {
        check.pattern = null;
        ExceptionMatcher matcher = ExceptionMatcher.createFor(AnalysisException.class);
        ExceptionMatcher cause = ExceptionMatcher.createFor(IllegalArgumentException.class)
                .expectMessage(containsString("Regular expression given must not be empty!"));
        matcher.expectCause(cause);
        expectedException.expect(matcher);
        run();
    }

    @Test
    public void testInvalidPattern2() {
        check.pattern = "Alice has a cat (qwerty";
        ExceptionMatcher matcher = ExceptionMatcher.createFor(AnalysisException.class);
        ExceptionMatcher cause = ExceptionMatcher.createFor(IllegalArgumentException.class)
                .expectMessage(containsString(check.pattern));
        matcher.expectCause(cause);
        expectedException.expect(matcher);
        run();
    }

    @Test
    public void testPositive() {
        check.pattern = "(\\.(?:log|trace|debug|info|warn|error)\\((?!.*(?:Eid|Cin)).*\\))";
        SourceFile file = run();
        CheckMessagesVerifier.verify(file.getCheckMessages())
                .next()
                .atLine(16)
                .withMessage("Text \".warn(\"A message\")\" matches given regular expression " +
                        "\"(\\.(?:log|trace|debug|info|warn|error)\\((?!.*(?:Eid|Cin)).*\\))\"")
                .next()
                .atLine(19)
                .withMessage("Text \".error(\"ddd\")\" matches given regular expression " +
                        "\"(\\.(?:log|trace|debug|info|warn|error)\\((?!.*(?:Eid|Cin)).*\\))\"")
                .next()
                .atLine(20)
                .withMessage("Text \".error(ex)\" matches given regular expression " +
                        "\"(\\.(?:log|trace|debug|info|warn|error)\\((?!.*(?:Eid|Cin)).*\\))\"")
                .noMore();
    }

    @Test
    public void testNegative() {
        check.pattern = "not found regex";
        check.invertMode = true;
        SourceFile file = run();
        CheckMessagesVerifier.verify(file.getCheckMessages())
                .next()
                .atLine(null)
                .withMessage("File do not contain requested text, that should match given regular expression " +
                        "\"not found regex\"")
                .noMore();
    }

    @Test
    public void testNegative2() {
        check.pattern = "@author\\s+[a-zA-Z]+\\s+[a-zA-Z]+\\s+\\<[a-z@.]+\\>";
        check.invertMode = true;
        SourceFile file = run();
        CheckMessagesVerifier.verify(file.getCheckMessages())
                .next()
                .noMore();
    }

    @Test
    public void testIterator() {
        expectedException.expect(UnsupportedOperationException.class);
        Matcher matcher = Pattern.compile(".*").matcher("");
        RegularExpressionSimpleCheck.MatchResultIterator iter = new RegularExpressionSimpleCheck.MatchResultIterator(matcher);
        iter.remove();
    }

    private SourceFile run() {
        return JavaAstScanner.scanSingleFile(
                new File("src/test/files/checks/RegularExpressionCheckTestFile.java"),
                new VisitorsBridge(check)
        );
    }

}