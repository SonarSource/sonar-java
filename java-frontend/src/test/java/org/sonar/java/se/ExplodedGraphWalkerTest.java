/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.reflect.ClassPath;
import org.junit.Test;
import org.sonar.java.se.checks.ConditionAlwaysTrueOrFalseCheck;
import org.sonar.java.se.checks.CustomUnclosedResourcesCheck;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.checks.LocksNotUnlockedCheck;
import org.sonar.java.se.checks.NonNullSetToNullCheck;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.checks.UnclosedResourcesCheck;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ExplodedGraphWalkerTest {

  @Test
  public void seEngineTest() {
    JavaCheckVerifier.verify("src/test/files/se/SeEngineTest.java", seChecks());
  }

  @Test
  public void test_cleanup_state() {
    final int[] steps = new int[2];
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestCleanupState.java", new SymbolicExecutionVisitor(Collections.emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        ExplodedGraphWalker explodedGraphWalker = new ExplodedGraphWalker(false);
        explodedGraphWalker.visitMethod((MethodTree) tree, new MethodBehavior(((MethodTree) tree).symbol()));
        steps[0] += explodedGraphWalker.steps;
      }
    });
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestCleanupState.java", new SymbolicExecutionVisitor(Collections.emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        ExplodedGraphWalker explodedGraphWalker = new ExplodedGraphWalker();
        explodedGraphWalker.visitMethod((MethodTree) tree, new MethodBehavior(((MethodTree) tree).symbol()));
        steps[1] += explodedGraphWalker.steps;
      }
    });
    assertThat(steps[0]).isPositive();
    assertThat(steps[0]).isGreaterThan(steps[1]);
  }

  @Test
  public void reproducer() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/Reproducer.java", seChecks());
  }

  @Test
  public void use_false_branch_on_loop_when_reaching_max_exec_program_point() {
    ExplodedGraph.ProgramPoint[] programPoints = new ExplodedGraph.ProgramPoint[2];
    ExplodedGraphWalker explodedGraphWalker = new ExplodedGraphWalker() {

      boolean shouldEnqueueFalseBranch = false;

      @Override
      public void enqueue(ExplodedGraph.ProgramPoint programPoint, ProgramState programState, boolean exitPath) {
        int nbOfExecution = programState.numberOfTimeVisited(programPoint);
        if (nbOfExecution > MAX_EXEC_PROGRAM_POINT) {
          shouldEnqueueFalseBranch = true;
          programPoints[0] = programPoint;
        } else {
          shouldEnqueueFalseBranch = false;
        }
        int workListSize = workList.size();

        super.enqueue(programPoint, programState, exitPath);

        assertThat(workList.size()).isEqualTo(workListSize + 1);
        if (shouldEnqueueFalseBranch) {
          assertThat(programPoints[1]).isNull();
          programPoints[1] = workList.peekFirst().programPoint;
        }
      }
    };
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestMaxExecProgramPoint.java", new SymbolicExecutionVisitor(Collections.emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        explodedGraphWalker.visitMethod((MethodTree) tree, new MethodBehavior(((MethodTree) tree).symbol()));
      }
    });

    // we reached the max number of execution of a program point
    assertThat(programPoints[0]).isNotNull();
    // B2 - for each
    assertThat(programPoints[0].block.id()).isEqualTo(2);

    // we enqueued a new node in the workList after reaching the max number of execeution point
    assertThat(programPoints[1]).isNotNull();
    // B1 - using the false branch to exit the loop
    assertThat(programPoints[1].block.id()).isEqualTo(1);
  }

  @Test
  public void test_limited_loop_execution() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestCase.java", new SymbolicExecutionVisitor(Collections.emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        try {
          new ExplodedGraphWalker().visitMethod((MethodTree) tree, new MethodBehavior(((MethodTree) tree).symbol()));
        } catch (ExplodedGraphWalker.MaximumStepsReachedException exception) {
          fail("loop execution should be limited");
        }
      }
    });
  }

  @Test
  public void test_maximum_steps_reached() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/MaxSteps.java", new SymbolicExecutionVisitor(Collections.emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        try {
          new ExplodedGraphWalker().visitMethod((MethodTree) tree, new MethodBehavior(((MethodTree) tree).symbol()));
          fail("Too many states were processed !");
        } catch (ExplodedGraphWalker.MaximumStepsReachedException exception) {
          assertThat(exception.getMessage()).startsWith("reached limit of 16000 steps for method");
        }
      }
    });
  }

  @Test
  public void test_maximum_steps_reached_with_issue() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/MaxStepsWithIssue.java", new UnclosedResourcesCheck());
  }

  @Test
  public void test_maximum_number_nested_states() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/MaxNestedStates.java", new SymbolicExecutionVisitor(Collections.emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        try {
          new ExplodedGraphWalker().visitMethod((MethodTree) tree, new MethodBehavior(((MethodTree) tree).symbol()));
          fail("Too many states were processed !");
        } catch (ExplodedGraphWalker.MaximumStepsReachedException exception) {
          assertThat(exception.getMessage()).startsWith("reached maximum number of 10000 branched states");
        }
      }
    });
  }

  @Test
  public void system_exit() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/SystemExit.java", seChecks());
  }

  @Test
  public void read_package_annotations() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/PackageAnnotationsNullable.java", seChecks());
    JavaCheckVerifier.verify("src/test/files/se/PackageAnnotationsNonNull.java", seChecks());
  }

  @Test
  public void xproc_usage_of_method_behaviors() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/XProcMethodBehavior.java", seChecks());
  }

  @Test
  public void xproc_usage_of_method_behaviors_with_explicit_exceptional_path() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/XProcMethodBehaviorExplicitException.java", seChecks());
  }

  @Test
  public void xproc_usage_of_method_behaviors_with_explicit_exceptional_path_and_branching() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/XProcMethodBehaviorExplicitExceptionBranching.java", seChecks());
  }

  @Test
  public void xproc_usage_of_exceptional_path_and_branching() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/XProcExceptionalBranching.java", seChecks());
  }

  static class MethodAsInstruction extends SECheck {
    int toStringCall = 0;
    @Override
    public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
      if(syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
        if(((MethodInvocationTree) syntaxNode).symbol().name().equals("toString")) {
          toStringCall++;
        }
      }
      return super.checkPreStatement(context, syntaxNode);
    }
  }
  @Test
  public void methods_should_be_evaluated_only_once() throws Exception {
    MethodAsInstruction check = new MethodAsInstruction();
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/EvaluateMethodOnce.java", check);
    assertThat(check.toStringCall).isEqualTo(1);
  }

  @Test
  public void eg_walker_factory_default_checks() throws IOException {
    // Compute the list of SEChecks defined in package
    List<String> seChecks = ClassPath.from(ExplodedGraphWalkerTest.class.getClassLoader())
      .getTopLevelClasses("org.sonar.java.se.checks")
      .stream()
      .map(ClassPath.ClassInfo::getSimpleName)
      .filter(name -> name.endsWith("Check") && !name.equals(SECheck.class.getSimpleName()))
      // CustomUnclosedResource is a template rule and should not be activated by default
      .filter(name -> !name.equals(CustomUnclosedResourcesCheck.class.getSimpleName()))
      .sorted()
      .collect(Collectors.toList());
    ExplodedGraphWalker.ExplodedGraphWalkerFactory factory = new ExplodedGraphWalker.ExplodedGraphWalkerFactory(new ArrayList<>());
    assertThat(factory.seChecks.stream().map(c -> c.getClass().getSimpleName()).sorted().collect(Collectors.toList())).isEqualTo(seChecks);
  }

  private static SECheck[] seChecks() {
    return new SECheck[]{
      new NullDereferenceCheck(),
      new DivisionByZeroCheck(),
      new ConditionAlwaysTrueOrFalseCheck(),
      new UnclosedResourcesCheck(),
      new CustomUnclosedResourcesCheck(),
      new LocksNotUnlockedCheck(),
      new NonNullSetToNullCheck(),
    };
  }

}
