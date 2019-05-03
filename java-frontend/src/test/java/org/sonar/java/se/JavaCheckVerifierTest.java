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

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.assertj.core.api.Fail;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
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

public class JavaCheckVerifierTest {

  private static final String FILENAME_ISSUES = "src/test/files/JavaCheckVerifier.java";
  private static final String FILENAME_NO_ISSUE = "src/test/files/JavaCheckVerifierNoIssue.java";
  private static final IssuableSubscriptionVisitor NO_EFFECT_VISITOR = new IssuableSubscriptionVisitor() {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.emptyList();
    }
  };

  @Test
  public void verify_line_issues() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    JavaCheckVerifier.verify("src/test/files/JavaCheckVerifier.java", visitor);
  }

  @Test
  public void verify_unexpected_issue() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withIssue(4, "extra message");

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at [4]");
    }
  }

  @Test
  public void verify_combined_missing_expected_and_unexpected_issues() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withIssue(4, "extra message").withoutIssue(1);

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected at [1], Unexpected at [4]");
    }
  }

  @Test
  public void verify_missing_expected_issue() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues().withoutIssue(1);

    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Expected at [1]");
    }
  }

  @Test
  public void verify_issue_on_file() {
    String expectedMessage = "messageOnFile";
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withIssueOnFile(expectedMessage);
    JavaCheckVerifier.verifyIssueOnFile(FILENAME_ISSUES, expectedMessage, visitor);
  }

  @Test(expected = IllegalStateException.class)
  public void verify_issue_on_file_incorrect() {
    JavaCheckVerifier.verifyIssueOnFile(FILENAME_ISSUES, "messageOnFile", new FakeVisitor().withDefaultIssues());
  }

  @Test
  public void verify_no_issue_fail_if_noncompliant() {
    try {
      JavaCheckVerifier.verifyNoIssue(FILENAME_ISSUES, NO_EFFECT_VISITOR);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("The file should not declare noncompliants when no issues are expected");
    }
  }

  @Test
  public void verify_no_issue() {
    JavaCheckVerifier.verifyNoIssue(FILENAME_NO_ISSUE, NO_EFFECT_VISITOR);
  }

  @Test
  public void verify_with_provided_classes() {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();

    JavaCheckVerifier.verify(FILENAME_ISSUES, visitor, new ArrayList<File>());
  }

  @Test
  public void verify_with_default_test_jar() throws IOException {
    // This path is the actual test-jars path for this project, as the currently supplied jar doesn't cause any issues in the test file
    // retain the actual folder with contents. This will prevent other tests to fail which rely on the supplied bytecode.
    JavaCheckVerifier.verifyNoIssue(FILENAME_NO_ISSUE, NO_EFFECT_VISITOR);
  }

  @Test
  public void verify_with_provided_test_jar() throws IOException {
    String testJarsPathname = "target/my-test-jars";
    File file = new File(testJarsPathname);
    if (file.exists()) {
      file.delete();
    }
    if (file.mkdir()) {
      IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();

      JavaCheckVerifier.verify(FILENAME_ISSUES, visitor, testJarsPathname);
      file.delete();
    } else {
      Fail.fail("");
    }
  }

  @Test
  public void verify_with_unknown_directory_should_fail() throws IOException {
    try {
      JavaCheckVerifier.verify(FILENAME_ISSUES, NO_EFFECT_VISITOR, "unknown/test-jars");
      Fail.fail("");
    } catch (AssertionError e) {
      String message = e.getMessage();
      assertThat(message).startsWith("The directory to be used to extend class path does not exists (");
      assertThat(message).contains("unknown");
      assertThat(message).endsWith("test-jars).");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_shift() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectShift.java", NO_EFFECT_VISITOR);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Use only '@+N' or '@-N' to shifts messages.");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_attribute() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectAttribute.java", NO_EFFECT_VISITOR);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: invalid=1");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_attribute2() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectAttribute2.java", NO_EFFECT_VISITOR);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: invalid=1=2");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_endLine() throws IOException {
    try {
      JavaCheckVerifier.verifyNoIssue("src/test/files/JavaCheckVerifierIncorrectEndLine.java", NO_EFFECT_VISITOR);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("endLine attribute should be relative to the line and must be +N with N integer");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_secondaryLocation() throws IOException {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    try {
      JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierIncorrectSecondaryLocation.java", visitor);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Secondary locations: expected: [] unexpected:[3]");
    }
  }

  @Test
  public void verify_should_fail_when_using_incorrect_secondaryLocation2() throws IOException {
    IssuableSubscriptionVisitor visitor = new FakeVisitor().withDefaultIssues();
    try {
      JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierIncorrectSecondaryLocation2.java", visitor);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Secondary locations: expected: [5] unexpected:[]");
    }
  }

  @Test
  public void verify_flows() {
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
      .add()
    ;
    JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlows.java", fakeVisitor);
  }

  @Test
  public void verify_unexpected_flows() {
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
        .add()
      ;
    try {
      JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlows.java", fakeVisitor);
    } catch (AssertionError e) {
      assertThat(e).hasMessage("[Flow npe1 has line differences] expected:<[[9, 3]]> but was:<[[6, 5]]>");
    }
  }

  @Test
  public void verify_missing_flows() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(11, "A \"NullPointerException\" could be thrown; \"b\" is nullable here", 5, 11, 15)
        .flow()
          .flowItem(7, "b is assigned to null here", 7, 15)
        .add()
      .issueWithFlow(20)
        .flow(17, "msg", 19, null)
        .add();
    try {
      JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlows.java", fakeVisitor);
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Missing flows: npe1 [9,3].");
    }
  }

  @Test
  public void verify_flow_messages() {
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
    Throwable throwable = catchThrowable(() -> JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlows.java", fakeVisitor));
    assertThat(throwable)
      .isInstanceOf(AssertionError.class)
      .hasMessage("[Wrong messages in flow npe1 [9,3]] expected:<[\"[a is assigned to b here\", \"a is assigned to null here]\"]> but was:<[\"[invalid 2\", \"invalid 1]\"]>");
  }

  @Test
  public void verify_flow_locations() {
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
    Throwable throwable = catchThrowable(() -> JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlows.java", fakeVisitor));
    assertThat(throwable)
      .isInstanceOf(AssertionError.class)
      .hasMessage("[attribute mismatch for START_COLUMN: {MESSAGE=a is assigned to null here, START_COLUMN=12, END_COLUMN=20}] expected:<[12]> but was:<[6]>");
  }

  @Test
  public void verify_superfluous_flows() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(11, "A \"NullPointerException\" could be thrown; \"b\" is nullable here", 5, 11, 15)
        .flow()
          .flowItem(3, "a is assigned to null here", 12, 20)
          .flowItem(9, "a is assigned to b here", 7, 12)
        .add()
    ;
    try {
      JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlowsSuperfluous.java", fakeVisitor);
      Fail.fail("");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Following flow comments were observed, but not referenced by any issue: {superfluous=8,6,4, npe2=7}");
    }
  }

  @Test
  public void verify_flow_messages_explicit_order() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(4, "error", 5, 11, 15)
      .flow()
        .flowItem(5, "msg1")
        .flowItem(6, "msg2")
        .flowItem(4, "msg3")
      .add();
    JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlowsExplicitOrder.java", fakeVisitor);
  }

  @Test
  public void verify_flow_messages_implicit_order() {
    FakeVisitor fakeVisitor = new FakeVisitor()
      .issueWithFlow(4, "error", 5, 11, 15)
      .flow()
      .flowItem(4, "msg1")
      .flowItem(5, "msg2")
      .flowItem(5, "msg3")
      .flowItem(6, "msg4")
      .flowItem(6, "msg5")
      .add();
    JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlowsImplicitOrder.java", fakeVisitor);
  }

  @Test
  public void verify_fail_when_same_explicit_order_is_provided() {
    Throwable throwable = catchThrowable(() -> JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlowsDuplicateExplicitOrder.java", new FakeVisitor()));
    assertThat(throwable)
      .isInstanceOf(AssertionError.class)
      .hasMessage("Same explicit ORDER=1 provided for two comments.\n"
        + "6: flow@f {ORDER=1, MESSAGE=msg1}\n"
        + "7: flow@f {ORDER=1, MESSAGE=msg2}");
  }

  @Test
  public void verify_fail_when_mixing_explicit_and_implicit_order() {
    Throwable throwable = catchThrowable(() -> JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlowsMixedExplicitOrder.java", new FakeVisitor()));
    assertThat(throwable)
      .isInstanceOf(AssertionError.class)
      .hasMessage("Mixed explicit and implicit order in same flow.\n"
        + "5: flow@f {ORDER=3, MESSAGE=msg3}\n"
        + "7: flow@f {MESSAGE=msg2}");
  }

  @Test
  public void verify_two_flows_with_same_lines() {
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
    JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlowsWithSameLines.java", fakeVisitor);
  }

  @Test
  public void verify_two_flows_with_same_lines_wrong_msg() {
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
    Throwable throwable = catchThrowable(() -> JavaCheckVerifier.verify("src/test/files/JavaCheckVerifierFlowsWithSameLines2.java", fakeVisitor));
    assertThat(throwable)
      .hasMessage("Unexpected flows: [6,5,4]\n"
        + "[6,5,4]. Missing flows: wrong_msg1 [6,5,4],wrong_msg2 [6,5,4].");
  }

  private static class FakeVisitor extends IssuableSubscriptionVisitor implements IssueWithFlowBuilder {

    ListMultimap<Integer, String> issues = LinkedListMultimap.create();
    ListMultimap<Integer, AnalyzerMessage> preciseIssues = LinkedListMultimap.create();
    List<String> issuesOnFile = Lists.newLinkedList();
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
      preciseIssues.put(message.getLine(), message);
      return this;
    }

    private FakeVisitor withIssue(int line, String message) {
      issues.put(line, message);
      return this;
    }

    private FakeVisitor withoutIssue(int line) {
      issues.removeAll(line);
      preciseIssues.removeAll(line);
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
      Preconditions.checkState(issueWithFlow == null, "Finish previous issueWithFlow by calling #add");
      issueWithFlow = new AnalyzerMessage(this, OTHER_FAKE_INPUT_FILE, location, message, 0);
      return this;
    }

    private FakeVisitor flow() {
      Preconditions.checkNotNull(issueWithFlow, "Finish previous issueWithFlow by calling #add");
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
      Preconditions.checkState(!flows.isEmpty(), "Call #flow first to create a flow");
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
      for (AnalyzerMessage analyzerMessage : preciseIssues.values()) {
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
        return new InternalSyntaxToken(textSpan.startLine, 0, "mock", new ArrayList<>(), 0, 0, false);
      }
      return new ReturnStatementTreeImpl(
        new InternalSyntaxToken(textSpan.startLine, textSpan.startCharacter - 1, "", new ArrayList<>(), 0, 0, false),
        null,
        new InternalSyntaxToken(textSpan.endLine, textSpan.endCharacter - 1, "", new ArrayList<>(), 0, 0, false));
    }
  }

  private interface IssueWithFlowBuilder {

  }
}
