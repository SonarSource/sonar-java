/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.se;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.fest.assertions.Fail;
import org.sonar.java.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.sonar.java.se.Expectations.IssueAttribute.EFFORT_TO_FIX;
import static org.sonar.java.se.Expectations.IssueAttribute.END_COLUMN;
import static org.sonar.java.se.Expectations.IssueAttribute.END_LINE;
import static org.sonar.java.se.Expectations.IssueAttribute.FLOWS;
import static org.sonar.java.se.Expectations.IssueAttribute.MESSAGE;
import static org.sonar.java.se.Expectations.IssueAttribute.SECONDARY_LOCATIONS;
import static org.sonar.java.se.Expectations.IssueAttribute.START_COLUMN;

class Expectations {

  private static final Map<String, IssueAttribute> ATTRIBUTE_MAP = ImmutableMap.<String, IssueAttribute>builder()
    .put("message", MESSAGE)
    .put("effortToFix", EFFORT_TO_FIX)
    .put("sc", START_COLUMN)
    .put("startColumn", START_COLUMN)
    .put("el", END_LINE)
    .put("endLine", END_LINE)
    .put("ec", END_COLUMN)
    .put("endColumn", END_COLUMN)
    .put("secondary", SECONDARY_LOCATIONS)
    .put("flows", FLOWS)
    .build();


  enum IssueAttribute {
    MESSAGE(Function.identity()),
    START_COLUMN(Integer::valueOf),
    END_COLUMN(Integer::valueOf),
    END_LINE(LineRef::fromString, LineRef::toLine),
    EFFORT_TO_FIX(Double::valueOf),
    SECONDARY_LOCATIONS(multiValueAttribute(Integer::valueOf)),
    FLOWS(multiValueAttribute(Function.identity()))
    ;

    private Function<String, ?> setter;
    Function<Object, Object> getter = Function.identity();

    IssueAttribute(Function<String, ?> setter) {
      this.setter = setter;
    }

    IssueAttribute(Function<String, ?> setter, Function<Object, Object> getter) {
      this.setter = setter;
      this.getter = getter;
    }

    static <T> Function<String, List<T>> multiValueAttribute(Function<String, T> convert) {
      return (String input) -> Arrays.stream(input.split(",")).map(convert).collect(toList());
    }

    <T> T get(Map<IssueAttribute, Object> values) {
      Object rawValue = values.get(this);
      return rawValue == null ? null : (T) getter.apply(rawValue);
    }
  }

  abstract static class LineRef {
    abstract int getLine(int ref);

    static LineRef fromString(String input) {
      if (input.startsWith("+")) {
        return new RelativeLineRef(Integer.valueOf(input));
      } else {
        return new AbsoluteLineRef(Integer.valueOf(input));
      }
    }

    static int toLine(Object ref) {
      return ((LineRef) ref).getLine(0);
    }

    static class AbsoluteLineRef extends LineRef {
      final int line;

      public AbsoluteLineRef(int line) {
        this.line = line;
      }

      public int getLine(int ref) {
        return line;
      }
    }

    static class RelativeLineRef extends LineRef {
      final int offset;

      RelativeLineRef(int offset) {
        this.offset = offset;
      }

      @Override int getLine(int ref) {
        return ref + offset;
      }
    }
  }

  static class FlowComment {
    final String id;
    final int line;
    final Map<IssueAttribute, Object> attributes;

    public FlowComment(String id, int line, Map<IssueAttribute, Object> attributes) {
      this.id = id;
      this.line = line;
      this.attributes = Collections.unmodifiableMap(attributes);
    }

    <T> T get(IssueAttribute attribute) {
      return (T) attribute.get(attributes);
    }

    @Override
    public String toString() {
      return String.format("%d: flow@%s %s", line, id, attributes.toString());
    }
  }

  final Multimap<Integer, EnumMap<IssueAttribute, Object>> issues = ArrayListMultimap.create();
  final Multimap<String, FlowComment> flows = ArrayListMultimap.create();
  final boolean expectNoIssues;
  final String expectFileIssue;
  final Integer expectFileIssueOnLine;

  //initialized lazily in #containFlow
  private Map<ImmutableList<Integer>, String> flowLines = null;
  private Set<String> seenFlowIds = new HashSet<>();

  Expectations() {
    this(false, null, null);
  }

  Expectations(boolean expectNoIssues, @Nullable String expectFileIssue, @Nullable Integer expectFileIssueOnLine) {
    this.expectNoIssues = expectNoIssues;
    this.expectFileIssue = expectFileIssue;
    this.expectFileIssueOnLine = expectFileIssueOnLine;
  }

  Optional<String> containFlow(List<AnalyzerMessage> flow) {
    if (flowLines == null) {
      flowLines = computeFlowLines();
    }
    ImmutableList<Integer> actualLines = flowToLines(flow, AnalyzerMessage::getLine);
    Optional<String> flowId = Optional.ofNullable(flowLines.get(actualLines));
    flowId.ifPresent(id -> seenFlowIds.add(id));
    return flowId;
  }

  Set<String> unseenFlowIds() {
    return Sets.difference(flows.keySet(), seenFlowIds);
  }

  private Map<ImmutableList<Integer>, String> computeFlowLines() {
    return flows.asMap().entrySet().stream()
      .collect(Collectors.toMap(e -> flowToLines(e.getValue(), i -> i.line), Map.Entry::getKey));
  }

  private static <T> ImmutableList<Integer> flowToLines(Collection<T> flow, ToIntFunction<T> toLineFunction) {
    return flow.stream()
      .mapToInt(toLineFunction)
      .sorted().boxed()
      .collect(collectingAndThen(toList(), ImmutableList::copyOf));
  }

  String flowToLines(String flowId) {
    return flows.get(flowId).stream().map(f -> String.valueOf(f.line)).collect(joining(","));
  }

  IssuableSubscriptionVisitor parser() {
    return new Parser();
  }

  private class Parser extends IssuableSubscriptionVisitor {
    private static final String NONCOMPLIANT_COMMENT = "// Noncompliant";
    private static final String FLOW_COMMENT = "// flow";

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      collectExpectedIssues(syntaxTrivia.comment(), syntaxTrivia.startLine());
    }

    private void collectExpectedIssues(String comment, int line) {
      if (comment.startsWith(NONCOMPLIANT_COMMENT)) {
        parseIssue(comment, line);
      }
      if (comment.startsWith(FLOW_COMMENT)) {
        parseFlow(comment, line);
      }
    }

    private void parseFlow(String comment, int line) {
      int atIdx = comment.indexOf('@');
      int followingSpaceIdx = comment.indexOf(' ', atIdx);
      String flowIdString = comment.substring(atIdx + 1, followingSpaceIdx == -1 ? comment.length() : followingSpaceIdx);
      String[] flowIds = flowIdString.split(",");
      String message = extractMessage(comment);
      Map<IssueAttribute, Object> attributes = extractAttributes(comment);
      attributes.put(MESSAGE, message);
      Arrays.stream(flowIds)
        .map(id -> new FlowComment(id, line, attributes))
        .forEach(f -> flows.put(f.id, f));
    }

    private void parseIssue(String comment, int line) {
      String cleanedComment = StringUtils.remove(comment, NONCOMPLIANT_COMMENT);

      EnumMap<IssueAttribute, Object> attr = new EnumMap<>(IssueAttribute.class);
      String expectedMessage = extractMessage(cleanedComment);
      if (StringUtils.isNotEmpty(expectedMessage)) {
        attr.put(MESSAGE, expectedMessage);
      }
      int expectedLine = line;
      String attributesSubstr = extractAttributes(comment, attr);

      cleanedComment = StringUtils.stripEnd(StringUtils.remove(StringUtils.remove(cleanedComment, "[[" + attributesSubstr + "]]"), "{{" + expectedMessage + "}}"), " \t");
      expectedLine = parseLineShifting(cleanedComment, expectedLine);
      updateEndLine(expectedLine, attr);
      issues.put(expectedLine, attr);
    }

    private String extractMessage(String cleanedComment) {
      return StringUtils.substringBetween(cleanedComment, "{{", "}}");
    }

    private int parseLineShifting(String cleanedComment, int expectedLine) {
      if (StringUtils.startsWith(cleanedComment, "@")) {
        final int lineAdjustment;
        final char firstChar = cleanedComment.charAt(1);
        final int endIndex = cleanedComment.indexOf(' ');
        if (endIndex == -1) {
          lineAdjustment = Integer.parseInt(cleanedComment.substring(2));
        } else {
          lineAdjustment = Integer.parseInt(cleanedComment.substring(2, endIndex));
        }
        if (firstChar == '+') {
          expectedLine += lineAdjustment;
        } else if (firstChar == '-') {
          expectedLine -= lineAdjustment;
        } else {
          Fail.fail("Use only '@+N' or '@-N' to shifts messages.");
        }
      }
      return expectedLine;
    }

    private Map<IssueAttribute, Object> extractAttributes(String comment) {
      final Map<Expectations.IssueAttribute, Object> attributes = new EnumMap<>(IssueAttribute.class);
      extractAttributes(comment, attributes);
      return attributes;
    }

    private String extractAttributes(String comment, Map<IssueAttribute, Object> attr) {
      String attributesSubstr = StringUtils.substringBetween(comment, "[[", "]]");
      if (StringUtils.isEmpty(attributesSubstr)) {
        return attributesSubstr;
      }
      Iterable<String> attributes = Splitter.on(";").split(attributesSubstr);
      for (String attribute : attributes) {
        String[] split = StringUtils.split(attribute, '=');
        if (split.length == 2 && ATTRIBUTE_MAP.containsKey(split[0])) {
          IssueAttribute issueAttribute = ATTRIBUTE_MAP.get(split[0]);
          Object value = issueAttribute.setter.apply(split[1]);
          attr.put(issueAttribute, value);
        } else {
          Fail.fail("// Noncompliant attributes not valid: " + attributesSubstr);
        }
      }
      return attributesSubstr;
    }

    private void updateEndLine(int expectedLine, EnumMap<IssueAttribute, Object> attr) {
      if (attr.containsKey(END_LINE)) {
        LineRef endLine = (LineRef) attr.get(END_LINE);
        if (endLine instanceof LineRef.RelativeLineRef) {
          attr.put(END_LINE, new LineRef.AbsoluteLineRef(endLine.getLine(expectedLine)));
        } else {
          Fail.fail("endLine attribute should be relative to the line and must be +N with N integer");
        }
      }
    }
  }

}
