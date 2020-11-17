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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.TestUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckVerifierTest {

  private static final String FILENAME_ISSUES = "src/test/files/JavaCheckVerifier.java";
  private static final String FILENAME_NO_ISSUE = "src/test/files/JavaCheckVerifierNoIssue.java";
  private static final IssuableSubscriptionVisitor NO_EFFECT_VISITOR = new IssuableSubscriptionVisitor() {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.emptyList();
    }
  };

  @Test
  void verify_line_issues() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifier.java")
      .withCheck(visitor)
      .verifyIssues();
  }

  @Test
  void verify_unexpected_issue() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withIssue(4, "extra message");
    CheckVerifier verifier = CheckVerifier.newVerifier().onFile(FILENAME_ISSUES).withCheck(visitor);

    try {
      verifier.verifyIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at [4]");
    }
  }

  @Test
  void verify_combined_missing_expected_and_unexpected_issues() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withIssue(4, "extra message").withoutIssue(1);
    CheckVerifier verifier = CheckVerifier.newVerifier().onFile(FILENAME_ISSUES).withCheck(visitor);

    try {
      verifier.verifyIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected at [1], Unexpected at [4]");
    }
  }

  @Test
  void verify_missing_expected_issue() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withoutIssue(1);
    CheckVerifier verifier = CheckVerifier.newVerifier().onFile(FILENAME_ISSUES).withCheck(visitor);

    try {
      verifier.verifyIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected at [1]");
    }
  }

  @Test
  void verify_issue_on_file() {
    String expectedMessage = "messageOnFile";
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withIssueOnFile(expectedMessage);
    CheckVerifier.newVerifier()
      .onFile(FILENAME_ISSUES)
      .withCheck(visitor)
      .verifyIssueOnFile(expectedMessage);
  }

  @Test
  void verify_issue_on_file_incorrect() {
    CheckVerifier verifier = CheckVerifier.newVerifier().onFile(FILENAME_ISSUES).withCheck(new FakeVisitor().withDefaultIssues());
    assertThrows(AssertionError.class, () -> verifier.onFile("messageOnFile"));
  }

  @Test
  void verify_no_issue() {
    CheckVerifier.newVerifier()
      .onFile(FILENAME_NO_ISSUE)
      .withCheck(NO_EFFECT_VISITOR)
      .verifyNoIssues();
  }

  @Test
  void verify_with_provided_classes() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();

    CheckVerifier.newVerifier()
      .onFile(FILENAME_ISSUES)
      .withCheck(visitor)
      .withClassPath(Collections.emptyList())
      .verifyIssues();
  }

  @Test
  void verify_with_default_test_jar() throws IOException {
    // This path is the actual test-jars path for this project, as the currently supplied jar doesn't cause any issues in the test file
    // retain the actual folder with contents. This will prevent other tests to fail which rely on the supplied bytecode.
    CheckVerifier.newVerifier()
      .onFile(FILENAME_NO_ISSUE)
      .withCheck(NO_EFFECT_VISITOR)
      .verifyNoIssues();
  }

  @Test
  void verify_should_fail_when_using_incorrect_shift() throws IOException {
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierIncorrectShift.java")
      .withCheck(NO_EFFECT_VISITOR);

    try {
      verifier.verifyNoIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Use only '@+N' or '@-N' to shifts messages.");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_attribute() throws IOException {
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierIncorrectAttribute.java")
      .withCheck(NO_EFFECT_VISITOR);

    try {
      verifier.verifyNoIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: 'invalid=1'");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_attribute2() throws IOException {
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierIncorrectAttribute2.java")
      .withCheck(NO_EFFECT_VISITOR);

    try {
      verifier.verifyNoIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: 'invalid=1=2'");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_endLine() throws IOException {
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierIncorrectEndLine.java")
      .withCheck(NO_EFFECT_VISITOR);

    try {
      verifier.verifyNoIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("endLine attribute should be relative to the line and must be +N with N integer");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_secondaryLocation() throws IOException {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierIncorrectSecondaryLocation.java")
      .withCheck(visitor);

    try {
      verifier.verifyIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Secondary locations: expected: [] unexpected: [3]. In JavaCheckVerifierIncorrectSecondaryLocation.java:10");
    }
  }

  @Test
  void verify_should_fail_when_using_incorrect_secondaryLocation2() throws IOException {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierIncorrectSecondaryLocation2.java")
      .withCheck(visitor);

    try {
      verifier.verifyIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Secondary locations: expected: [5] unexpected: []. In JavaCheckVerifierIncorrectSecondaryLocation2.java:10");
    }
  }

  @Test
  void verify_flows() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(11, "A \"NullPointerException\" could be thrown; \"b\" is nullable here", 5, 11, 15)
      .flow()
      .flowItem(3, "a is assigned to null here", 12, 20)
      .flowItem(9, "a is assigned to b here", 7, 12)
      .flow()
      .flowItem(7, "b is assigned to null here", 7, 15)
      .add()
      .issueWithFlow(20)
      .flow(17, "msg", 19, null)
      .add()
      .issueWithFlow(27)
      .flow(24, "common", 25, "msg1")
      .flow(24, "common", 26, "msg2")
      .add()
      .issueWithFlow(36)
      .flow(31, "When", 32, "Given")
      .add()
      .issueWithFlow(46)
      .flow(41, "When", 42, "Given")
      .add();

    CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierFlows.java")
      .withCheck(fakeVisitor)
      .verifyIssues();
  }

  @Test
  void verify_unexpected_flows() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(11, "A \"NullPointerException\" could be thrown; \"b\" is nullable here", 5, 11, 15)
      .flow()
      .flowItem(5, "a is assigned to null here", 12, 20)
      .flowItem(6, "a is assigned to b here", 7, 12)
      .flow()
      .flowItem(7, "b is assigned to null here", 7, 15)
      .add()
      .issueWithFlow(20)
      .flow(17, "msg", 19, null)
      .add();
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierFlows.java")
      .withCheck(fakeVisitor);

    try {
      verifier.verifyIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Flow npe1 has line differences. Expected: [9, 3] but was: [6, 5]");
    }
  }

  @Test
  void verify_missing_flows() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(11, "A \"NullPointerException\" could be thrown; \"b\" is nullable here", 5, 11, 15)
      .flow()
      .flowItem(7, "b is assigned to null here", 7, 15)
      .add()
      .issueWithFlow(20)
      .flow(17, "msg", 19, null)
      .add();
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierFlows.java")
      .withCheck(fakeVisitor);

    try {
      verifier.verifyIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Missing flows: npe1 [9,3].");
    }
  }

  @Test
  void verify_flow_messages() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(11, "A \"NullPointerException\" could be thrown; \"b\" is nullable here", 5, 11, 15)
      .flow()
      .flowItem(3, "invalid 1", 12, 20)
      .flowItem(9, "invalid 2", 7, 12)
      .flow()
      .flowItem(7, "b is assigned to null here", 7, 15)
      .add()
      .issueWithFlow(20)
      .flow(17, "msg", 19, null)
      .add();
    Throwable throwable = catchThrowable(() -> CheckVerifier.newVerifier().onFile("src/test/files/JavaCheckVerifierFlows.java").withCheck(fakeVisitor).verifyIssues());
    assertThat(throwable)
      .isInstanceOf(AssertionError.class)
      .hasMessage("Wrong messages in flow npe1 [9,3]. Expected: [\"a is assigned to b here\", \"a is assigned to null here\"] but was: [\"invalid 2\", \"invalid 1\"]");
  }

  @Test
  void verify_flow_locations() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(11, "A \"NullPointerException\" could be thrown; \"b\" is nullable here", 5, 11, 15)
      .flow()
      .flowItem(3, "a is assigned to null here", 6, 20)
      .flowItem(9, "a is assigned to b here", 7, 12)
      .flow()
      .flowItem(7, "b is assigned to null here", 7, 15)
      .add()
      .issueWithFlow(20)
      .flow(17, "msg", 19, null)
      .add();
    Throwable throwable = catchThrowable(() -> CheckVerifier.newVerifier().onFile("src/test/files/JavaCheckVerifierFlows.java").withCheck(fakeVisitor).verifyIssues());
    assertThat(throwable)
      .isInstanceOf(AssertionError.class)
      .hasMessage("line 3 attribute mismatch for 'START_COLUMN'. Expected: '12', but was: '6'");
  }

  @Test
  void verify_superfluous_flows() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(11, "A \"NullPointerException\" could be thrown; \"b\" is nullable here.", 5, 11, 15)
      .flow()
      .flowItem(3, "a is assigned to null here", 12, 20)
      .flowItem(9, "a is assigned to b here", 7, 12)
      .add();
    CheckVerifier verifier = CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierFlowsSuperfluous.java")
      .withCheck(fakeVisitor);

    try {
      verifier.verifyIssues();
      Fail.fail("Should have failed");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Following flow comments were observed, but not referenced by any issue: {superfluous=8,6,4, npe2=7}");
    }
  }

  @Test
  void verify_flow_messages_explicit_order() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(4, "error", 5, 11, 15)
      .flow()
      .flowItem(5, "msg1")
      .flowItem(6, "msg2")
      .flowItem(4, "msg3")
      .add();
    CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierFlowsExplicitOrder.java")
      .withCheck(fakeVisitor)
      .verifyIssues();
  }

  @Test
  void verify_flow_messages_implicit_order() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(4, "error", 5, 11, 15)
      .flow()
      .flowItem(4, "msg1")
      .flowItem(5, "msg2")
      .flowItem(5, "msg3")
      .flowItem(6, "msg4")
      .flowItem(6, "msg5")
      .add();
    CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierFlowsImplicitOrder.java")
      .withCheck(fakeVisitor)
      .verifyIssues();
  }

  @Test
  void verify_fail_when_same_explicit_order_is_provided() {
    Throwable throwable = catchThrowable(() -> CheckVerifier.newVerifier().onFile("src/test/files/JavaCheckVerifierFlowsDuplicateExplicitOrder.java").withCheck(new FakeVisitor()).verifyIssues());
    assertThat(throwable)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("Same explicit ORDER=1 provided for two comments.")
      .hasMessageContaining("6: flow@f {ORDER=1, MESSAGE=msg1}")
      .hasMessageContaining("7: flow@f {ORDER=1, MESSAGE=msg2}");
  }

  @Test
  void verify_fail_when_mixing_explicit_and_implicit_order() {
    Throwable throwable = catchThrowable(() -> CheckVerifier.newVerifier().onFile("src/test/files/JavaCheckVerifierFlowsMixedExplicitOrder.java").withCheck(new FakeVisitor()).verifyIssues());
    assertThat(throwable)
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("Mixed explicit and implicit order in same flow.")
      .hasMessageContaining("5: flow@f {ORDER=3, MESSAGE=msg3}")
      .hasMessageContaining("7: flow@f {MESSAGE=msg2}");
  }

  @Test
  void verify_two_flows_with_same_lines() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(4, "error", 5, 11, 15)
      .flow()
      .flowItem(4, "line4")
      .flowItem(5, "f1")
      .flowItem(6, "line6")
      .flow()
      .flowItem(4, "line4")
      .flowItem(5, "f2")
      .flowItem(6, "line6")
      .add()
      .issueWithFlow(10, "error", 5, 11, 15)
      .flow()
      .flowItem(10, "msg1")
      .flowItem(11, "msg2")
      .flowItem(12, "msg3")
      .flow()
      .flowItem(10, "msg1")
      .flowItem(11, "msg2")
      .flowItem(12, "msg3")
      .add();
    CheckVerifier.newVerifier()
      .onFile("src/test/files/JavaCheckVerifierFlowsWithSameLines.java")
      .withCheck(fakeVisitor)
      .verifyIssues();
  }

  @Test
  void verify_two_flows_with_same_lines_wrong_msg() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(4, "error", 5, 11, 15)
      .flow()
      .flowItem(4, "line4")
      .flowItem(5, "f1")
      .flowItem(6, "line6")
      .flow()
      .flowItem(4, "line4")
      .flowItem(5, "f2")
      .flowItem(6, "line6")
      .add();
    Throwable throwable = catchThrowable(() -> CheckVerifier.newVerifier().onFile("src/test/files/JavaCheckVerifierFlowsWithSameLines2.java").withCheck(fakeVisitor).verifyIssues());
    assertThat(throwable)
      .hasMessage("Unexpected flows: [6,5,4]\n" + "[6,5,4]. Missing flows: wrong_msg1 [6,5,4],wrong_msg2 [6,5,4].");
  }

  @Rule(key = "JavaCheckVerifier-Tester")
  private static class FakeVisitor extends IssuableSubscriptionVisitor implements IssueWithFlowBuilder {

    Map<Integer, List<String>> issues = new LinkedHashMap<>();
    Map<Integer, List<AnalyzerMessage>> preciseIssues = new LinkedHashMap<>();
    List<String> issuesOnFile = new LinkedList<>();
    private static final InputFile FAKE_INPUT_FILE = TestUtils.emptyInputFile("a");
    private static final InputFile OTHER_FAKE_INPUT_FILE = TestUtils.emptyInputFile("f");
    private AnalyzerMessage issueWithFlow;

    private FakeVisitor withDefaultIssues() {
      AnalyzerMessage withMultipleLocation = new AnalyzerMessage(this, FAKE_INPUT_FILE, new AnalyzerMessage.TextSpan(10, 9, 10, 10), "message4", 3);
      withMultipleLocation.flows.add(Collections.singletonList(new AnalyzerMessage(this, FAKE_INPUT_FILE, 3, "no message", 0)));
      withMultipleLocation.flows.add(Collections.singletonList(new AnalyzerMessage(this, FAKE_INPUT_FILE, 4, "no message", 0)));
      return this.withIssue(1, "message")
        .withIssue(3, "message1")
        .withIssue(7, "message2")
        .withIssue(8, "message3")
        .withIssue(8, "message3")
        .withPreciseIssue(withMultipleLocation)
        .withPreciseIssue(new AnalyzerMessage(this, FAKE_INPUT_FILE, 11, "no message", 4))
        .withPreciseIssue(new AnalyzerMessage(this, FAKE_INPUT_FILE, 12, "message12", 0))
        .withPreciseIssue(new AnalyzerMessage(this, FAKE_INPUT_FILE, new AnalyzerMessage.TextSpan(14, 5, 15, 11), "message12", 0))
        .withIssue(17, "message17");
    }

    private FakeVisitor withPreciseIssue(AnalyzerMessage message) {
      preciseIssues.computeIfAbsent(message.getLine(), key -> new LinkedList<>()).add(message);
      return this;
    }

    private FakeVisitor withIssue(int line, String message) {
      issues.computeIfAbsent(line, key -> new LinkedList<>()).add(message);
      return this;
    }

    private FakeVisitor withoutIssue(int line) {
      issues.remove(line);
      preciseIssues.remove(line);
      return this;
    }

    private FakeVisitor withIssueOnFile(String message) {
      issuesOnFile.add(message);
      return this;
    }

    private FakeVisitor issueWithFlow(int line) {
      return issueWithFlow(null, new AnalyzerMessage.TextSpan(line));
    }

    private FakeVisitor issueWithFlow(int line, @Nullable String message, int startColumn, int endLine, int endColumn) {
      return issueWithFlow(message, new AnalyzerMessage.TextSpan(line, startColumn, endLine, endColumn));
    }

    private FakeVisitor issueWithFlow(@Nullable String message, AnalyzerMessage.TextSpan location) {
      checkState(issueWithFlow == null, "Finish previous issueWithFlow by calling #add");
      issueWithFlow = new AnalyzerMessage(this, OTHER_FAKE_INPUT_FILE, location, message, 0);
      return this;
    }

    private FakeVisitor flow() {
      Objects.requireNonNull(issueWithFlow, "Finish previous issueWithFlow by calling #add");
      issueWithFlow.flows.add(new LinkedList<>());
      return this;
    }

    private FakeVisitor flow(int line1, @Nullable String msg1, int line2, @Nullable String msg2) {
      flow();
      flowItem(line1, msg1);
      return flowItem(line2, msg2);
    }

    private FakeVisitor flowItem(int line, @Nullable String msg) {
      return flowItem(msg, new AnalyzerMessage.TextSpan(line));
    }

    private FakeVisitor flowItem(int line, @Nullable String msg, int startColumn, int endColumn) {
      return flowItem(msg, new AnalyzerMessage.TextSpan(line, startColumn, line, endColumn));
    }

    private FakeVisitor flowItem(@Nullable String msg, AnalyzerMessage.TextSpan textSpan) {
      List<List<AnalyzerMessage>> flows = issueWithFlow.flows;
      checkState(!flows.isEmpty(), "Call #flow first to create a flow");
      AnalyzerMessage flowItem = new AnalyzerMessage(this, OTHER_FAKE_INPUT_FILE, textSpan, msg, 0);
      flows.get(flows.size() - 1).add(flowItem);
      return this;
    }

    private FakeVisitor add() {
      // flows are in reverse order the same way as real checks report in reverse order
      issueWithFlow.flows.forEach(Collections::reverse);
      withPreciseIssue(issueWithFlow);
      issueWithFlow = null;
      return this;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.emptyList();
    }

    @Override
    public void setContext(JavaFileScannerContext context) {
      super.setContext(context);
      for (Integer line : issues.keySet()) {
        for (String message : issues.get(line)) {
          addIssue(line, message);
        }
      }
      List<AnalyzerMessage> anamyerMessages = preciseIssues.values().stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());
      for (AnalyzerMessage analyzerMessage : anamyerMessages) {
        Double messageCost = analyzerMessage.getCost();
        Integer cost = messageCost != null ? messageCost.intValue() : null;
        List<JavaFileScannerContext.Location> secLocations = new ArrayList<>();
        if (!analyzerMessage.flows.isEmpty()) {
          List<List<JavaFileScannerContext.Location>> flows = analyzerMessage.flows.stream()
            .map(FakeVisitor::messagesToLocations)
            .collect(toList());
          context.reportIssueWithFlow(this, mockTree(analyzerMessage), analyzerMessage.getMessage(), flows, null);
        } else {
          reportIssue(mockTree(analyzerMessage), analyzerMessage.getMessage(), secLocations, cost);
        }
      }
      for (String message : issuesOnFile) {
        addIssueOnFile(message);
      }
    }

    private static List<JavaFileScannerContext.Location> messagesToLocations(List<AnalyzerMessage> flow) {
      return flow.stream().map(m -> new JavaFileScannerContext.Location(m.getMessage(), mockTree(m))).collect(toList());
    }

    private static Tree mockTree(final AnalyzerMessage analyzerMessage) {
      AnalyzerMessage.TextSpan textSpan = analyzerMessage.primaryLocation();
      if (textSpan.onLine()) {
        return new InternalSyntaxToken(textSpan.startLine, 0, "mock", new ArrayList<>(), false);
      }
      return new ReturnStatementTreeImpl(
        new InternalSyntaxToken(textSpan.startLine, textSpan.startCharacter - 1, "", new ArrayList<>(), false),
        null,
        new InternalSyntaxToken(textSpan.endLine, textSpan.endCharacter - 1, "", new ArrayList<>(), false));
    }
  }

  private static void checkState(boolean condition, String errorMessage) {
    if (!condition) {
      throw new IllegalStateException(errorMessage);
    }
  }

  private interface IssueWithFlowBuilder {

  }

}
