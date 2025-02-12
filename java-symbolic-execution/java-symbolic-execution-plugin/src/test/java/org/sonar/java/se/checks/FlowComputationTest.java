/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.se.checks;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.JParserTestUtils;
import org.sonar.java.se.utils.SETestUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowComputationTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void test() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowComputation.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_catof() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowComputationCATOF.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_messages_on_method_invocation() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowComputationMIT.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_flow_messages_on_parameter_declaration() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowMessagesParameterDeclaration.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_flow_messages_on_branch() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowMessagesBranch.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_getArgumentIdentifier() {
    CompilationUnitTree cut = JParserTestUtils.parse(new File("src/test/files/se/FlowComputationGetArgumentIdentifier.java"));
    MethodTree foo = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(1);
    MethodInvocationTree mit = (MethodInvocationTree) ((ExpressionStatementTree) foo.block().body().get(1)).expression();

    assertThatThrownBy(() -> FlowComputation.getArgumentIdentifier(mit, -1)).isInstanceOf(IllegalArgumentException.class).hasMessage("index must be within arguments range.");
    assertThat(FlowComputation.getArgumentIdentifier(mit, 0).name()).isEqualTo("localVariable");
    assertThat(FlowComputation.getArgumentIdentifier(mit, 1).name()).isEqualTo("field");
    assertThat(FlowComputation.getArgumentIdentifier(mit, 2)).isNull();
    assertThatThrownBy(() -> FlowComputation.getArgumentIdentifier(mit, 4)).isInstanceOf(IllegalArgumentException.class).hasMessage("index must be within arguments range.");
  }

  @Test
  void test_relational_sv_operands() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowComputationRelSV.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_unary_sv_operands() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowComputationUnarySV.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_multiple_paths() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowComputationMultiplePath.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_multiple_paths_xproc() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowComputationMultiplePathXProc.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_trigger_yield_flow_computation_only_on_relevant_yields() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/UselessFlowComputation.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void avoid_visiting_equivalent_paths() {
    logTester.setLevel(Level.DEBUG);
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowComputationNoOverflowWhenMergingPaths.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
    assertThat(logTester.logs(Level.DEBUG)).doesNotContain("Flow was not able to complete");
  }

  @Test
  void xproc_flow_messages() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/XProcFlowMessages.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new DivisionByZeroCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void xproc_flow_messages_constraint_is_VS_can_be() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/XProcFlowMessagesIsCanBe.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new DivisionByZeroCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_flows_with_single_msg_not_reported() {
    SECheckVerifier.newVerifier()
      .withCustomIssueVerifier(issues -> {
        assertThat(issues).hasSize(4);
        issues.forEach(issue -> assertThat(issue.flows.stream().allMatch(List::isEmpty))
          .as("No flows expected, but %s was reported.", issue.flows)
          .isTrue());
      })
      .onFile("src/test/files/se/FlowsWithSingleMsg.java")
      .withChecks(
        new NullDereferenceCheck(),
        new ConditionalUnreachableCodeCheck(),
        new BooleanGratuitousExpressionsCheck(),
        new DivisionByZeroCheck())
      .verifyIssues();
  }

  @Test
  void test_method_invocations_without_flows() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/MethodInvocationWithoutFlows.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new DivisionByZeroCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_exception_flows() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/ExceptionFlows.java")
      .withCheck(new NullDereferenceCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_location_should_not_be_created_on_null_tree() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/FlowNullTree.java")
      .withChecks(new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck(), new DivisionByZeroCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

}
