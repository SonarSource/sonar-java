/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Fail;
import org.sonar.java.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.sonar.java.se.Expectations.IssueAttribute.EFFORT_TO_FIX;
import static org.sonar.java.se.Expectations.IssueAttribute.END_COLUMN;
import static org.sonar.java.se.Expectations.IssueAttribute.END_LINE;
import static org.sonar.java.se.Expectations.IssueAttribute.FLOWS;
import static org.sonar.java.se.Expectations.IssueAttribute.LINE;
import static org.sonar.java.se.Expectations.IssueAttribute.MESSAGE;
import static org.sonar.java.se.Expectations.IssueAttribute.ORDER;
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
    .put("order", ORDER)
    .build();

  enum IssueAttribute {
    LINE(Function.identity()),
    ORDER(Integer::valueOf),
    MESSAGE(Function.identity()),
    START_COLUMN(Integer::valueOf),
    END_COLUMN(Integer::valueOf),
    END_LINE(Parser.LineRef::fromString, Parser.LineRef::toLine),
    EFFORT_TO_FIX(Double::valueOf),
    SECONDARY_LOCATIONS(multiValueAttribute(Integer::valueOf)),
    FLOWS(multiValueAttribute(Function.identity()));

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
      return (String input) -> Strings.isNullOrEmpty(input) ? Collections.emptyList() : Arrays.stream(input.split(",")).map(convert).collect(toList());
    }

    <T> T get(Map<IssueAttribute, Object> values) {
      Object rawValue = values.get(this);
      return rawValue == null ? null : (T) getter.apply(rawValue);
    }
  }

  static class Issue extends EnumMap<IssueAttribute, Object> {

    private Issue() {
      super(IssueAttribute.class);
    }

    static Issue create() {
      return new Issue();
    }
  }

  static class FlowComment {
    final String id;
    final int line;
    final Map<IssueAttribute, Object> attributes;
    final int startColumn;

    public FlowComment(String id, int line, int startColumn, Map<IssueAttribute, Object> attributes) {
      this.id = id;
      this.line = line;
      this.startColumn = startColumn;
      this.attributes = Collections.unmodifiableMap(attributes);
    }

    int compareTo(FlowComment other) {
      if (this == other) {
        return 0;
      }
      Integer thisOrder = ORDER.get(attributes);
      Integer otherOrder = ORDER.get(other.attributes);
      if (thisOrder != null && otherOrder != null) {
        if(thisOrder.equals(otherOrder)) {
          Fail.fail("Same explicit ORDER=%s provided for two comments.\n%s\n%s", thisOrder, this, other);
        }
        return thisOrder.compareTo(otherOrder);
      }
      if (thisOrder == null && otherOrder == null) {
        int compareLines = Integer.compare(line, other.line);
        return compareLines != 0 ? compareLines : Integer.compare(startColumn, other.startColumn);
      }
      throw new AssertionError("Mixed explicit and implicit order in same flow.\n" + this + "\n" + other);
    }

    @CheckForNull
    String message() {
      return MESSAGE.get(attributes);
    }

    int line() {
      return line;
    }

    @Override
    public String toString() {
      return String.format("%d: flow@%s %s", line, id, attributes.toString());
    }
  }

  final Multimap<Integer, Issue> issues = ArrayListMultimap.create();
  final SortedSetMultimap<String, FlowComment> flows = TreeMultimap.create(String::compareTo, Collections.reverseOrder(FlowComment::compareTo));
  final boolean expectNoIssues;
  final String expectFileIssue;
  final Integer expectFileIssueOnLine;

  private Set<String> seenFlowIds = new HashSet<>();

  Expectations() {
    this(false, null, null);
  }

  Expectations(boolean expectNoIssues, @Nullable String expectFileIssue, @Nullable Integer expectFileIssueOnLine) {
    this.expectNoIssues = expectNoIssues;
    this.expectFileIssue = expectFileIssue;
    this.expectFileIssueOnLine = expectFileIssueOnLine;
  }


  Optional<String> containFlow(List<AnalyzerMessage> actual) {
    List<Integer> actualLines = flowToLines(actual, AnalyzerMessage::getLine);
    Set<String> expectedFlows = flows.keySet().stream()
      .filter(flowId -> !seenFlowIds.contains(flowId))
      .filter(flowId -> flowToLines(flows.get(flowId), FlowComment::line).equals(actualLines))
      .collect(Collectors.toSet());
    if (expectedFlows.isEmpty()) {
      return Optional.empty();
    }
    if (expectedFlows.size() == 1) {
      String flowId = Iterables.getOnlyElement(expectedFlows);
      seenFlowIds.add(flowId);
      return Optional.of(flowId);
    }
    // more than 1 flow with same lines, let's check messages
    List<String> actualMessages = actual.stream().map(AnalyzerMessage::getMessage).collect(toList());
    Optional<String> foundFlow = expectedFlows.stream().filter(flowId -> hasSameMessages(flowId, actualMessages)).findFirst();
    foundFlow.ifPresent(flowId -> seenFlowIds.add(flowId));
    return foundFlow;
  }

  private boolean hasSameMessages(String flowId, List<String> actualMessages) {
    List<String> expectedMessages = flows.get(flowId).stream().map(FlowComment::message).collect(toList());
    return expectedMessages.equals(actualMessages);
  }

  Set<String> unseenFlowIds() {
    return Sets.difference(flows.keySet(), seenFlowIds);
  }

  private static <T> List<Integer> flowToLines(Collection<T> flow, ToIntFunction<T> toLineFunction) {
    return flow.stream()
      .mapToInt(toLineFunction)
      .boxed()
      .collect(toList());
  }

  String flowToLines(String flowId) {
    return flows.get(flowId).stream().map(f -> String.valueOf(f.line)).collect(joining(","));
  }

  IssuableSubscriptionVisitor parser() {
    return new Parser(issues, flows);
  }

  @VisibleForTesting
  static class Parser extends IssuableSubscriptionVisitor {
    private static final Pattern NONCOMPLIANT_COMMENT = Pattern.compile("//\\s+Noncompliant");
    private static final Pattern FLOW_COMMENT = Pattern.compile("//\\s+flow");
    private static final Pattern SHIFT = Pattern.compile("Noncompliant@(\\S+)");
    private static final Pattern FLOW = Pattern.compile("flow@(?<ids>\\S+).*?(?=flow@)?");

    private final Multimap<String, FlowComment> flows;
    private final Multimap<Integer, Issue> issues;

    Parser(Multimap<Integer, Issue> issues, Multimap<String, FlowComment> flows) {
      this.issues = issues;
      this.flows = flows;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      // ignore whole commented lines
      if (syntaxTrivia.column() != 0) {
        collectExpectedIssues(syntaxTrivia.comment(), syntaxTrivia.startLine());
      }
    }

    @VisibleForTesting
    void collectExpectedIssues(String comment, int line) {
      if (NONCOMPLIANT_COMMENT.matcher(comment).find()) {
        ParsedComment parsedComment = parseIssue(comment, line);
        issues.put(LINE.get(parsedComment.issue), parsedComment.issue);
        parsedComment.flows.forEach(f -> flows.put(f.id, f));
      }
      if (FLOW_COMMENT.matcher(comment).find()) {
        parseFlows(comment, line).forEach(f -> flows.put(f.id, f));
      }
    }

    @VisibleForTesting
    static List<FlowComment> parseFlows(@Nullable String comment, int line) {
      if (comment == null) {
        return Collections.emptyList();
      }
      List<List<String>> flowIds = new ArrayList<>();
      List<Integer> flowStarts = new ArrayList<>();
      Matcher matcher = FLOW.matcher(comment);
      while (matcher.find()) {
        List<String> ids = Arrays.asList(matcher.group("ids").split(","));
        flowIds.add(ids);
        flowStarts.add(matcher.start());
      }
      // add one more fake start at the end, so the boundary of comment i is flowStarts[i],flowStarts[i+1] also for the last one
      flowStarts.add(comment.length());

      return IntStream.range(0, flowIds.size())
        .mapToObj(i -> createFlows(flowIds.get(i), line, flowStarts.get(i), comment.substring(flowStarts.get(i), flowStarts.get(i + 1))))
        .flatMap(Function.identity())
        .collect(Collectors.toList());
    }

    private static Stream<FlowComment> createFlows(List<String> ids, int line, int startColumn, String flow) {
      Map<IssueAttribute, Object> attributes = new EnumMap<>(IssueAttribute.class);
      attributes.putAll(parseAttributes(flow));
      String message = parseMessage(flow, flow.length());
      attributes.put(MESSAGE, message);
      return ids.stream().map(id -> new FlowComment(id, line, startColumn, attributes));
    }



    @VisibleForTesting
    static ParsedComment parseIssue(String comment, int line) {
      Matcher shiftMatcher = SHIFT.matcher(comment);
      Matcher flowMatcher = FLOW.matcher(comment);
      return createIssue(line,
        shiftMatcher.find() ? shiftMatcher.group(1) : null,
        comment,
        parseMessage(comment, flowMatcher.find() ? flowMatcher.start() : comment.length()),
        comment);
    }

    private static ParsedComment createIssue(int line, @Nullable String shift, @Nullable String attributes, @Nullable String message, @Nullable String flow) {
      Issue issue = Issue.create();
      issue.put(LINE, parseLineShifting(shift).getLine(line));
      Map<IssueAttribute, Object> attrs = parseAttributes(attributes);
      attrs = adjustEndLine(attrs, line);
      issue.putAll(attrs);
      if (message != null) {
        issue.put(MESSAGE, message);
      }
      List<FlowComment> flows = parseFlows(flow, line);
      return new ParsedComment(issue, flows);
    }

    private static LineRef parseLineShifting(@Nullable String shift) {
      if (shift == null) {
        return new LineRef.RelativeLineRef(0);
      }
      try {
        return LineRef.fromString(shift);
      } catch (NumberFormatException e) {
        Fail.fail("Use only '@+N' or '@-N' to shifts messages.");
        return null;
      }
    }

    private static Map<IssueAttribute, Object> parseAttributes(@Nullable String comment) {
      comment = StringUtils.substringBetween(comment, "[[", "]]");
      if (comment == null) {
        return Collections.emptyMap();
      }
      return Arrays.stream(comment.split(";"))
        .map(Parser::parseAttribute)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<IssueAttribute, Object> adjustEndLine(Map<IssueAttribute, Object> attributes, int line) {
      Object endLine = attributes.get(END_LINE);
      if (endLine != null && endLine instanceof LineRef.RelativeLineRef) {
        LineRef.RelativeLineRef relativeLineRef = (LineRef.RelativeLineRef) endLine;
        if (relativeLineRef.offset < 0) {
          Fail.fail("endLine attribute should be relative to the line and must be +N with N integer");
        }
        EnumMap<IssueAttribute, Object> copy = new EnumMap<>(attributes);
        copy.put(END_LINE, new LineRef.AbsoluteLineRef(relativeLineRef.getLine(line)));
        return copy;
      }
      return attributes;
    }

    private static Map.Entry<IssueAttribute, Object> parseAttribute(String attribute) {
      Scanner scanner = new Scanner(attribute).useDelimiter("[=]+");
      String name = scanner.next();
      if (!ATTRIBUTE_MAP.containsKey(name)) {
        Fail.fail("// Noncompliant attributes not valid: " + attribute);
      }
      IssueAttribute key = ATTRIBUTE_MAP.get(name);
      Object value = key.setter.apply(scanner.hasNext() ? scanner.next() : null);
      return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    private static String parseMessage(String cleanedComment, int horizon) {
      return StringUtils.substringBetween(cleanedComment.substring(0, horizon), "{{", "}}");
    }

    static class ParsedComment {
      final Issue issue;
      final List<FlowComment> flows;

      private ParsedComment(Issue issue, List<FlowComment> flows) {
        this.issue = issue;
        this.flows = flows;
      }
    }

    abstract static class LineRef {
      abstract int getLine(int ref);

      static LineRef fromString(String input) {
        if (input.startsWith("+") || input.startsWith("-")) {
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

        @Override
        int getLine(int ref) {
          return ref + offset;
        }
      }

      @Override
      public int hashCode() {
        return Objects.hash(getLine(0));
      }

      @Override
      public boolean equals(Object obj) {
        return LineRef.class.isAssignableFrom(obj.getClass()) && Objects.equals(getLine(0), ((LineRef) obj).getLine(0));
      }
    }
  }

}
