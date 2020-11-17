/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.testing;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.RspecKey;
import org.sonar.java.collections.MapBuilder;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.sonar.java.testing.Expectations.IssueAttribute.EFFORT_TO_FIX;
import static org.sonar.java.testing.Expectations.IssueAttribute.END_COLUMN;
import static org.sonar.java.testing.Expectations.IssueAttribute.END_LINE;
import static org.sonar.java.testing.Expectations.IssueAttribute.FLOWS;
import static org.sonar.java.testing.Expectations.IssueAttribute.LINE;
import static org.sonar.java.testing.Expectations.IssueAttribute.MESSAGE;
import static org.sonar.java.testing.Expectations.IssueAttribute.ORDER;
import static org.sonar.java.testing.Expectations.IssueAttribute.SECONDARY_LOCATIONS;
import static org.sonar.java.testing.Expectations.IssueAttribute.START_COLUMN;

class Expectations {

  private static final Map<String, IssueAttribute> ATTRIBUTE_MAP = MapBuilder.<String, IssueAttribute>newMap()
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
    private Function<Object, Object> getter = Function.identity();

    IssueAttribute(Function<String, ?> setter) {
      this.setter = setter;
    }

    IssueAttribute(Function<String, ?> setter, UnaryOperator<Object> getter) {
      this.setter = setter;
      this.getter = getter;
    }

    private static <T> Function<String, List<T>> multiValueAttribute(Function<String, T> convert) {
      return (String input) -> isNullOrEmpty(input) ? Collections.emptyList() : Arrays.stream(input.split(",")).map(String::trim).map(convert).collect(toList());
    }

    private static boolean isNullOrEmpty(@Nullable String input) {
      return input == null || input.trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    <T> T get(Map<IssueAttribute, Object> values) {
      Object rawValue = values.get(this);
      return rawValue == null ? null : (T) getter.apply(rawValue);
    }
  }

  static class Issue extends EnumMap<IssueAttribute, Object> {

    private Issue() {
      super(IssueAttribute.class);
    }
  }

  static class FlowComment {
    final String id;
    final int line;
    final Map<IssueAttribute, Object> attributes;
    final int startColumn;

    private FlowComment(String id, int line, int startColumn, Map<IssueAttribute, Object> attributes) {
      this.id = id;
      this.line = line;
      this.startColumn = startColumn;
      this.attributes = Collections.unmodifiableMap(attributes);
    }

    private int compareTo(FlowComment other) {
      if (this == other) {
        return 0;
      }
      Integer thisOrder = ORDER.get(attributes);
      Integer otherOrder = ORDER.get(other.attributes);
      if (thisOrder != null && otherOrder != null) {
        if(thisOrder.equals(otherOrder)) {
          throw new AssertionError(String.format("Same explicit ORDER=%s provided for two comments.%n%s%n%s", thisOrder, this, other));
        }
        return thisOrder.compareTo(otherOrder);
      }
      if (thisOrder == null && otherOrder == null) {
        int compareLines = Integer.compare(line, other.line);
        return compareLines != 0 ? compareLines : Integer.compare(startColumn, other.startColumn);
      }
      throw new AssertionError(String.format("Mixed explicit and implicit order in same flow.%n%s%n%s", this, other));
    }

    @CheckForNull
    String message() {
      return MESSAGE.get(attributes);
    }

    @Override
    public String toString() {
      return String.format("%d: flow@%s %s", line, id, attributes.toString());
    }
  }

  final Map<Integer, List<Expectations.Issue>> issues = new HashMap<>();
  final Map<String, SortedSet<FlowComment>> flows = new HashMap<>();
  private boolean expectNoIssues = false;
  private String expectedProjectIssue = null;
  private String expectedFileIssue = null;

  private Set<String> seenFlowIds = new HashSet<>();

  Expectations() {
  }

  void setExpectNoIssues() {
    this.expectNoIssues = true;
  }

  boolean expectNoIssues() {
    return expectNoIssues;
  }

  boolean expectIssueAtFileLevel() {
    return StringUtils.isNotEmpty(expectedFileIssue);
  }

  void setExpectedFileIssue(String expectedMessage) {
    this.expectedFileIssue = expectedMessage;
  }

  String expectedFileIssue() {
    return expectedFileIssue;
  }

  boolean expectIssueAtProjectLevel() {
    return StringUtils.isNotEmpty(expectedProjectIssue);
  }

  void setExpectedProjectIssue(String expectedMessage) {
    this.expectedProjectIssue = expectedMessage;
  }

  String expectedProjectIssue() {
    return expectedProjectIssue;
  }

  Optional<String> containFlow(List<AnalyzerMessage> actual) {
    List<Integer> actualLines = flowToLines(actual, AnalyzerMessage::getLine);
    Set<String> expectedFlows = flows.keySet().stream()
      .filter(flowId -> !seenFlowIds.contains(flowId))
      .filter(flowId -> flowToLines(flows.get(flowId), f -> f.line).equals(actualLines))
      .collect(Collectors.toSet());
    if (expectedFlows.isEmpty()) {
      return Optional.empty();
    }
    if (expectedFlows.size() == 1) {
      String flowId = expectedFlows.iterator().next();
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
    Set<String> result = new HashSet<>(flows.keySet());
    result.removeAll(seenFlowIds);
    return result;
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

  Parser parser() {
    return new Parser(issues, flows);
  }

  Parser noEffectParser() {
    Parser parser = new Parser(issues, flows);
    parser.nonCompliantComment = Pattern.compile("NO_ISSUES_WILL_BE_COLLECTED");
    parser.shift = Pattern.compile("NO_ISSUES_WILL_BE_COLLECTED");
    return parser;
  }

  @VisibleForTesting
  static class Parser extends IssuableSubscriptionVisitor {
    private static final String NONCOMPLIANT_FLAG = "Noncompliant";

    private Pattern nonCompliantComment = Pattern.compile("//\\s+" + NONCOMPLIANT_FLAG);
    private Pattern shift = Pattern.compile(NONCOMPLIANT_FLAG + "@(\\S+)");

    private static final Pattern FLOW_COMMENT = Pattern.compile("//\\s+flow");
    private static final Pattern FLOW = Pattern.compile("flow@(?<ids>\\S+).*?(?=flow@)?");

    private final Map<Integer, List<Issue>> issues;
    private final Map<String, SortedSet<FlowComment>> flows;

    private Parser(Map<Integer, List<Issue>> issues, Map<String, SortedSet<FlowComment>> flows) {
      this.issues = issues;
      this.flows = flows;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      collectExpectedIssues(syntaxTrivia.comment(), syntaxTrivia.startLine());
    }

    @VisibleForTesting
    void collectExpectedIssues(String comment, int line) {
      if (nonCompliantComment.matcher(comment).find()) {
        ParsedComment parsedComment = parseIssue(comment, line);
        issues.computeIfAbsent(LINE.get(parsedComment.issue), k -> new ArrayList<>()).add(parsedComment.issue);
        parsedComment.flows.forEach(f -> flows.computeIfAbsent(f.id, k -> newFlowSet()).add(f));
      }
      if (FLOW_COMMENT.matcher(comment).find()) {
        parseFlows(comment, line).forEach(f -> flows.computeIfAbsent(f.id, k -> newFlowSet()).add(f));
      }
    }

    private static TreeSet<FlowComment> newFlowSet() {
      return new TreeSet<>(Collections.reverseOrder(FlowComment::compareTo));
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
    ParsedComment parseIssue(String comment, int line) {
      Matcher shiftMatcher = shift.matcher(comment);
      Matcher flowMatcher = FLOW.matcher(comment);
      return createIssue(line,
        shiftMatcher.find() ? shiftMatcher.group(1) : null,
        comment,
        parseMessage(comment, flowMatcher.find() ? flowMatcher.start() : comment.length()),
        comment);
    }

    private static ParsedComment createIssue(int line, @Nullable String shift, @Nullable String attributes, @Nullable String message, @Nullable String flow) {
      Issue issue = new Issue();
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
        throw new AssertionError("Use only '@+N' or '@-N' to shifts messages.");
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
      if (endLine instanceof LineRef.RelativeLineRef) {
        LineRef.RelativeLineRef relativeLineRef = (LineRef.RelativeLineRef) endLine;
        if (relativeLineRef.offset < 0) {
          throw new AssertionError("endLine attribute should be relative to the line and must be +N with N integer");
        }
        EnumMap<IssueAttribute, Object> copy = new EnumMap<>(attributes);
        copy.put(END_LINE, new LineRef.AbsoluteLineRef(relativeLineRef.getLine(line)));
        return copy;
      }
      return attributes;
    }

    private static Map.Entry<IssueAttribute, Object> parseAttribute(String attribute) {
      try (Scanner scanner = new Scanner(attribute)) {
        scanner.useDelimiter("[=]+");
        String name = scanner.next();
        if (!ATTRIBUTE_MAP.containsKey(name)) {
          throw new AssertionError(String.format("// Noncompliant attributes not valid: '%s'", attribute));
        }
        IssueAttribute key = ATTRIBUTE_MAP.get(name);
        Object value = key.setter.apply(scanner.hasNext() ? scanner.next() : null);
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
      }
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

        @Override
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
        return obj != null
          && LineRef.class.isAssignableFrom(obj.getClass())
          && Objects.equals(getLine(0), ((LineRef) obj).getLine(0));
      }
    }
  }

  enum RemediationFunction {
    LINEAR, CONST
  }

  static class RuleJSON {
    static class Remediation {
      String func;
    }

    Remediation remediation;
  }

  @CheckForNull
  static RemediationFunction remediationFunction(AnalyzerMessage issue) {
    String ruleKey = ruleKey(issue);
    try {
      RuleJSON rule = getRuleJSON(ruleKey);
      if (rule.remediation == null) {
        return null;
      }
      switch (rule.remediation.func) {
        case "Linear":
          return RemediationFunction.LINEAR;
        case "Constant/Issue":
          return RemediationFunction.CONST;
        default:
          return null;
      }
    } catch (IOException | JsonParseException e) {
      // Failed to open JSON file, as this is not part of API yet, we should not fail because of this
      // Remediation function and cost not provided, "constant" is assumed.
      return null;
    }
  }

  private static RuleJSON getRuleJSON(String ruleKey) throws IOException {
    String ruleJson = "/org/sonar/l10n/java/rules/java/" + ruleKey + "_java.json";
    URL resource = CheckVerifier.class.getResource(ruleJson);
    if (resource == null) {
      throw new IOException(ruleJson + " not found");
    }
    Gson gson = new Gson();
    return gson.fromJson(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8), RuleJSON.class);
  }

  private static String ruleKey(AnalyzerMessage issue) {
    String ruleKey;
    RspecKey rspecKeyAnnotation = AnnotationUtils.getAnnotation(issue.getCheck().getClass(), RspecKey.class);
    if (rspecKeyAnnotation != null) {
      ruleKey = rspecKeyAnnotation.value();
    } else {
      Rule ruleAnnotation = AnnotationUtils.getAnnotation(issue.getCheck().getClass(), Rule.class);
      if (ruleAnnotation != null) {
        ruleKey = ruleAnnotation.key();
      } else {
        throw new AssertionError("Rules should be annotated with '@Rule(key = \"...\")' annotation (org.sonar.check.Rule).");
      }
    }
    return ruleKey;
  }

}
