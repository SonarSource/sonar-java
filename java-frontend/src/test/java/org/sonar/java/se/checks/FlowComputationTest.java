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
package org.sonar.java.se.checks;

import org.junit.Rule;
import org.junit.Test;

import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.resolve.Result;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.JavaCheckVerifier;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FlowComputationTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void test() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputation.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck());
  }

  @Test
  public void test_catof() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationCATOF.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck());
  }

  @Test
  public void test_messages_on_method_invocation() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationMIT.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck());
  }

  @Test
  public void test_flow_messages_on_parameter_declaration() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowMessagesParameterDeclaration.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(),
      new BooleanGratuitousExpressionsCheck());
  }

  @Test
  public void test_flow_messages_on_branch() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowMessagesBranch.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(),
      new BooleanGratuitousExpressionsCheck());
  }

  @Test
  public void test_getArgumentIdentifier() throws Exception {
    MethodInvocationTree mit = (MethodInvocationTree) Result.createForJavaFile("src/test/files/se/FlowComputationGetArgumentIdentifier").referenceTree(6, 5).parent();

    assertThatThrownBy(() -> FlowComputation.getArgumentIdentifier(mit, -1)).isInstanceOf(IllegalArgumentException.class).hasMessage("index must be within arguments range.");
    assertThat(FlowComputation.getArgumentIdentifier(mit, 0).name()).isEqualTo("localVariable");
    assertThat(FlowComputation.getArgumentIdentifier(mit, 1).name()).isEqualTo("field");
    assertThat(FlowComputation.getArgumentIdentifier(mit, 2)).isNull();
    assertThatThrownBy(() -> FlowComputation.getArgumentIdentifier(mit, 4)).isInstanceOf(IllegalArgumentException.class).hasMessage("index must be within arguments range.");
  }

  @Test
  public void test_relational_sv_operands() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationRelSV.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck());
  }

  @Test
  public void test_unary_sv_operands() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationUnarySV.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck());
  }

  @Test
  public void test_multiple_paths() {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationMultiplePath.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck());
  }

  @Test
  public void test_multiple_paths_xproc() {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationMultiplePathXProc.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck());
  }

  @Test
  public void test_trigger_yield_flow_computation_only_on_relevant_yields() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/UselessFlowComputation.java",  new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck());
  }

  @Test
  public void avoid_visiting_equivalent_paths() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationNoOverflowWhenMergingPaths.java",  new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck());
    assertThat(logTester.logs(LoggerLevel.DEBUG)).doesNotContain("Flow was not able to complete");
  }

  @Test
  public void xproc_flow_messages() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/XProcFlowMessages.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new DivisionByZeroCheck());
  }

  @Test
  public void xproc_flow_messages_constraint_is_VS_can_be() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/XProcFlowMessagesIsCanBe.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new DivisionByZeroCheck());
  }

  @Test
  public void test_flows_with_single_msg_not_reported() throws Exception {
    JavaCheckVerifier noFlowsVerifier = new JavaCheckVerifier() {
      @Override
      protected void checkIssues(Set<AnalyzerMessage> issues) {
        assertThat(issues).hasSize(4);
        issues.forEach(issue -> assertThat(issue.flows.stream().allMatch(List::isEmpty))
          .as("No flows expected, but %s was reported.", issue.flows)
          .isTrue());
      }
    };

    noFlowsVerifier.scanFile("src/test/files/se/FlowsWithSingleMsg.java",new SECheck[] { new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(),
      new BooleanGratuitousExpressionsCheck(), new DivisionByZeroCheck()});
  }

  @Test
  public void test_method_invocations_without_flows() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/MethodInvocationWithoutFlows.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new DivisionByZeroCheck());
  }

  @Test
  public void test_exception_flows() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/ExceptionFlows.java", new NullDereferenceCheck());
  }

  @Test
  public void test_location_should_not_be_created_on_null_tree() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowNullTree.java", new NullDereferenceCheck(), new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck(), new DivisionByZeroCheck());
  }

}
