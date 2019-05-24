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

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.checks.BooleanGratuitousExpressionsCheck;
import org.sonar.java.se.checks.ConditionalUnreachableCodeCheck;
import org.sonar.java.se.checks.CustomUnclosedResourcesCheck;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.checks.InvariantReturnCheck;
import org.sonar.java.se.checks.LocksNotUnlockedCheck;
import org.sonar.java.se.checks.MapComputeIfAbsentOrPresentCheck;
import org.sonar.java.se.checks.MinMaxRangeCheck;
import org.sonar.java.se.checks.NonNullSetToNullCheck;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.ObjectOutputStreamCheck;
import org.sonar.java.se.checks.OptionalGetBeforeIsPresentCheck;
import org.sonar.java.se.checks.ParameterNullnessCheck;
import org.sonar.java.se.checks.RedundantAssignmentsCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.checks.StreamNotConsumedCheck;
import org.sonar.java.se.checks.UnclosedResourcesCheck;
import org.sonar.java.se.checks.debug.DebugMethodYieldsCheck;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitor;

public class ExplodedGraphWalkerTest {

  @Test
  public void seEngineTest() {
    JavaCheckVerifier.verify("src/test/files/se/SeEngineTest.java", seChecks());
  }

  @Test
  public void test_cleanup_state() {
    final int[] steps = new int[2];
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestCleanupState.java", new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {
      @Override
      public void visitNode(Tree tree) {
        ExplodedGraphWalker explodedGraphWalker = new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel(), false);
        explodedGraphWalker.visitMethod((MethodTree) tree, methodBehaviorForSymbol(((MethodTree) tree).symbol()));
        steps[0] += explodedGraphWalker.steps;
      }
    });
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestCleanupState.java", new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {
      @Override
      public void visitNode(Tree tree) {
        ExplodedGraphWalker explodedGraphWalker = new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel());
        MethodTree methodTree = (MethodTree) tree;
        explodedGraphWalker.visitMethod(methodTree, methodBehaviorForSymbol(methodTree.symbol()));
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
  public void exception_catched_in_loop() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/LoopExceptionField.java", seChecks());
  }

  @Test
  public void constraints_on_fields() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/ConstraintsOnFields.java", seChecks());
  }

  @Test
  public void different_exceptions_lead_to_different_program_states_with_catch_exception_block() {
    Set<Type> encounteredExceptions = new HashSet<>();
    int[] tested = {0};

    JavaCheckVerifier.verifyNoIssue("src/test/files/se/ExceptionalSymbolicValueStacked.java", new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {

      private ExplodedGraphWalker explodedGraphWalker;

      @Override
      public void visitNode(Tree tree) {
        if (explodedGraphWalker == null) {
          explodedGraphWalker = new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel()) {

            private ExplodedGraph.Node firstExceptionalNode = null;

            @Override
            public void enqueue(ProgramPoint newProgramPoint, ProgramState programState, boolean exitPath, MethodYield methodYield) {
              SymbolicValue.ExceptionalSymbolicValue exceptionSV = null;
              SymbolicValue peekValue = programState.peekValue();
              boolean getNode = false;
              if (peekValue instanceof SymbolicValue.ExceptionalSymbolicValue) {
                exceptionSV = (SymbolicValue.ExceptionalSymbolicValue) peekValue;
                Type exceptionType = exceptionSV.exceptionType();
                if (exceptionType != null && (exceptionType.is("org.foo.MyException1") || exceptionType.is("org.foo.MyException2"))) {
                  encounteredExceptions.add(exceptionType);
                  getNode = true;
                }
              }
              int workListSize = workList.size();
              super.enqueue(newProgramPoint, programState, exitPath, methodYield);

              if (getNode) {
                if (firstExceptionalNode == null) {
                  firstExceptionalNode = workList.peekFirst();
                }
                assertThat(workList.size()).as("Should have created a new node in the graph for each of the exceptions").isEqualTo(workListSize + 1);
                assertThat(workList.peekFirst().programState.peekValue()).as("Exceptional Symbolic Value should stay on the stack").isEqualTo(exceptionSV);
                tested[0]++;
              }
            };
          };
        }

        MethodTree methodTree = (MethodTree) tree;
        if ("foo".equals(methodTree.symbol().name())) {
          explodedGraphWalker.visitMethod(methodTree, methodBehaviorForSymbol(methodTree.symbol()));
        } else {
          super.visitNode(methodTree);
        }
      };
    });

    assertThat(encounteredExceptions).hasSize(2);
    assertThat(tested[0]).isEqualTo(2);
  }

  @Test
  public void use_false_branch_on_loop_when_reaching_max_exec_program_point() {
    ProgramPoint[] programPoints = new ProgramPoint[2];
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestMaxExecProgramPoint.java", new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {

      private ExplodedGraphWalker explodedGraphWalker = null;

      @Override
      public void visitNode(Tree tree) {
        if (explodedGraphWalker == null) {
          explodedGraphWalker = new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel()) {

            boolean shouldEnqueueFalseBranch = false;

            @Override
            public void enqueue(ProgramPoint programPoint, ProgramState programState, boolean exitPath) {
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
        }

        MethodTree methodTree = (MethodTree) tree;
        explodedGraphWalker.visitMethod(methodTree, methodBehaviorForSymbol(methodTree.symbol()));
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
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestCase.java", new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {
      @Override
      public void visitNode(Tree tree) {
        try {
          MethodTree methodTree = (MethodTree) tree;
          new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel()).visitMethod(methodTree, methodBehaviorForSymbol(methodTree.symbol()));
        } catch (ExplodedGraphWalker.MaximumStepsReachedException exception) {
          fail("loop execution should be limited");
        }
      }
    });
  }

  @Test
  public void test_max_number_starting_states() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/MaxStartingStates.java",
      new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {
        @Override
        public void visitNode(Tree tree) {
          try {
            MethodTree methodTree = (MethodTree) tree;
            new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel()).visitMethod(methodTree, methodBehaviorForSymbol(methodTree.symbol()));
            fail("Too many starting states were generated!!");
          } catch (ExplodedGraphWalker.MaximumStartingStatesException exception) {
            assertThat(exception.getMessage()).startsWith("reached maximum number of 1024 starting states for method");
          }
        }
      });
  }

  @Test
  public void test_max_number_starting_states_boundaries() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/StartingStates1024.java",
      new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {
        @Override
        public void visitNode(Tree tree) {
          try {
            MethodTree methodTree = (MethodTree) tree;
            new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel()).visitMethod(methodTree, methodBehaviorForSymbol(methodTree.symbol()));
          } catch (ExplodedGraphWalker.MaximumStartingStatesException exception) {
            fail("Should have been able to generate 1024 states!");
          }
        }
      });
  }

  @Test
  public void test_maximum_steps_reached() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/MaxSteps.java", new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {
      @Override
      public void visitNode(Tree tree) {
        try {
          MethodTree methodTree = (MethodTree) tree;
          new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel()).visitMethod(methodTree, methodBehaviorForSymbol(methodTree.symbol()));
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
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/MaxNestedStates.java", new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {
      @Override
      public void visitNode(Tree tree) {
        try {
          MethodTree methodTree = (MethodTree) tree;
          new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel()).visitMethod(methodTree, methodBehaviorForSymbol(methodTree.symbol()));
          fail("Too many states were processed !");
        } catch (ExplodedGraphWalker.MaximumStepsReachedException exception) {
          assertThat(exception.getMessage()).startsWith("reached maximum number of 10000 branched states");
        }
      }
    });
  }

  @Test
  public void test_propagation_of_bytecode_analysis_failure() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/BytecodeExceptionPropagation.java", new NullDereferenceCheck());

  }

  @Test
  public void system_exit() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/SystemExit.java", seChecks());
  }

  @Test
  public void read_parametersAreNonnullByDefault_and_parametersAreNullableByDefault_annotations() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/annotations/PackageAnnotationsNullable.java", seChecks());
    JavaCheckVerifier.verify("src/test/files/se/annotations/PackageAnnotationsNonNull.java", seChecks());

    JavaCheckVerifier.verify("src/test/files/se/annotations/ClassAnnotationsNullable.java", seChecks());
    JavaCheckVerifier.verify("src/test/files/se/annotations/ClassAnnotationsNonNull.java", seChecks());

    JavaCheckVerifier.verify("src/test/files/se/annotations/MethodAnnotationsNullable.java", seChecks());
    JavaCheckVerifier.verify("src/test/files/se/annotations/MethodAnnotationsNonNull.java", seChecks());
  }

  @Test
  public void read_jetbrains_nullness_annotations() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/annotations/JetBrains.java", seChecks());
  }

  @Test
  public void androidx_nullness_annotations() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/annotations/AndroidXAnnotations.java", seChecks());
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

  @Test
  public void xproc_usage_of_exceptional_path_and_branching_with_reporting() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/XProcExceptionalBranchingReporting.java", seChecks());
  }

  @Test
  public void xproc_reporting_with_var_args() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/XProcReportingWithVarArgs.java", seChecks());
  }

  @Test
  public void xproc_keep_yield_for_reporting() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/YieldReporting.java", new SymbolicExecutionVisitor(Collections.emptyList(), new BehaviorCache(new SquidClassLoader(new ArrayList<>()))) {
      @Override
      public void visitNode(Tree tree) {
        MethodTree methodTree = (MethodTree) tree;
        if ("test".equals(methodTree.symbol().name())) {
          ExplodedGraphWalker egw = new ExplodedGraphWalker(this.behaviorCache, (SemanticModel) context.getSemanticModel());
          egw.visitMethod(methodTree, null);
          assertThat(egw.checkerDispatcher.methodYield).isNull();
        }
      }
    });
  }

  @Test
  public void test_this_super_not_null() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/ThisSuperNotNull.java", seChecks());
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
  public void compound_assignment_should_create_new_value_on_stack() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/CompoundAssignmentExecution.java", new SECheck() {

      private SymbolicValue rhsValue;

      @Override
      public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
        ProgramState state = context.getState();
        if (syntaxNode instanceof AssignmentExpressionTree) {
          rhsValue = state.peekValue();
        }
        return state;
      }

      @Override
      public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
        ProgramState state = context.getState();
        if (syntaxNode instanceof AssignmentExpressionTree) {
          assertThat(state.peekValue()).isNotEqualTo(rhsValue);
          // there should be only one value after compound assignment, result of compound operator
          assertThatThrownBy(() -> state.peekValue(1)).isInstanceOf(IllegalStateException.class);
        }
        return state;
      }
    });
  }

  @Test
  public void binary_expression_creates_not_null_value() throws Exception {
    int[] counter = new int[1];
    SECheck check = new SECheck() {
      @Override
      public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
        ProgramState state = context.getState();
        if (syntaxNode instanceof BinaryExpressionTree) {
          SymbolicValue sv = state.peekValue();
          assertThat(state.getConstraint(sv, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
          counter[0]++;
        }
        return state;
      }
    };
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/BinaryTreeExecution.java", check);
    assertThat(counter[0]).isEqualTo(17);
  }

  @Test
  public void simple_assignment_should_preserve_value_on_stack() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SimpleAssignmentExecution.java", new SECheck() {

      private SymbolicValue rhsValue;

      @Override
      public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
        ProgramState state = context.getState();
        if (syntaxNode instanceof AssignmentExpressionTree) {
          rhsValue = state.peekValue();
        }
        return state;
      }

      @Override
      public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
        ProgramState state = context.getState();
        if (syntaxNode instanceof AssignmentExpressionTree) {
          assertThat(state.peekValue()).isEqualTo(rhsValue);
          // there should be only one value after simple assignment, which is symbolic value of RHS
          assertThatThrownBy(() -> state.peekValue(1)).isInstanceOf(IllegalStateException.class);
        }
        return state;
      }
    });
  }

  @Test
  public void eg_walker_factory_default_checks() throws IOException {
    List<String> nonDefaultChecks = Stream.of(
      CustomUnclosedResourcesCheck.class,
      ConditionalUnreachableCodeCheck.class,
      BooleanGratuitousExpressionsCheck.class,
      InvariantReturnCheck.class,
      StreamNotConsumedCheck.class,
      MapComputeIfAbsentOrPresentCheck.class,
      ObjectOutputStreamCheck.class,
      DebugMethodYieldsCheck.class,
      MinMaxRangeCheck.class,
      ParameterNullnessCheck.class
      )
      .map(Class::getSimpleName)
      .collect(Collectors.toList());
    // Compute the list of SEChecks defined in package
    List<String> seChecks = ClassPath.from(ExplodedGraphWalkerTest.class.getClassLoader())
      .getTopLevelClasses("org.sonar.java.se.checks")
      .stream()
      .map(ClassPath.ClassInfo::getSimpleName)
      .filter(name -> name.endsWith("Check") && !name.equals(SECheck.class.getSimpleName()))
      .filter(name -> !nonDefaultChecks.contains(name))
      .sorted()
      .collect(Collectors.toList());

    ExplodedGraphWalker.ExplodedGraphWalkerFactory factory = new ExplodedGraphWalker.ExplodedGraphWalkerFactory(new ArrayList<>());
    assertThat(factory.seChecks.stream().map(c -> c.getClass().getSimpleName()).sorted().collect(Collectors.toList())).isEqualTo(seChecks);
  }

  @Test
  public void private_method_should_be_visited() {
    List<String> visitedMethods = new ArrayList<>();
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/XprocIfaceWithPrivateMethod.java", new SECheck() {
      @Override
      public void init(MethodTree methodTree, CFG cfg) {
        visitedMethods.add(methodTree.symbol().name());
      }
    });
    assertThat(visitedMethods).containsExactly("test", "privateMethod");
  }

  @Test
  public void constraints_on_field_reset() {
    JavaCheckVerifier.verify("src/test/java/org/sonar/java/resolve/targets/se/EGWResetFieldsA.java", seChecks());
    JavaCheckVerifier.verify("src/test/java/org/sonar/java/resolve/targets/se/EGWResetFieldsB.java", seChecks());
  }

  @Test
  public void test_enqueueing_of_catch_blocks() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/java/org/sonar/java/bytecode/se/testdata/ExceptionEnqueue.java");

    MethodBehavior mb = sev.behaviorCache.behaviors.get("org.sonar.java.bytecode.se.testdata.ExceptionEnqueue#testCatchBlockEnqueue(Lorg/sonar/java/bytecode/se/testdata/ExceptionEnqueue;)Z");
    List<HappyPathYield> happyPathYields = mb.happyPathYields().collect(Collectors.toList());
    assertThat(happyPathYields).hasSize(1);
    assertThat(happyPathYields.get(0).resultConstraint()).isNull();

    mb = sev.behaviorCache.behaviors.get("org.sonar.java.bytecode.se.testdata.ExceptionEnqueue#testCatchBlockEnqueue2()Z");
    happyPathYields = mb.happyPathYields().collect(Collectors.toList());
    assertThat(happyPathYields).hasSize(1);
    // correctly result constraint should be TRUE, but we enqueue also unreachable catch block which creates yield with FALSE result
    // and yields are reduced consequently
    assertThat(happyPathYields.get(0).resultConstraint()).isNull();
  }

  private static SECheck[] seChecks() {
    return new SECheck[]{
      new NullDereferenceCheck(),
      new DivisionByZeroCheck(),
      new ConditionalUnreachableCodeCheck(),
      new BooleanGratuitousExpressionsCheck(),
      new UnclosedResourcesCheck(),
      new CustomUnclosedResourcesCheck(),
      new LocksNotUnlockedCheck(),
      new NonNullSetToNullCheck(),
      new OptionalGetBeforeIsPresentCheck(),
      new RedundantAssignmentsCheck()
    };
  }

  private static MethodBehavior methodBehaviorForSymbol(Symbol.MethodSymbol symbol) {
    boolean varArgs = ((JavaSymbol.MethodJavaSymbol) symbol).isVarArgs();
    return new MethodBehavior(symbol.signature(), varArgs);
  }
}
