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
package org.sonar.java.se;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.Preconditions;
import org.sonar.java.cfg.SELiveVariables;
import org.sonar.java.model.CFGUtils;
import org.sonar.java.model.SEExpressionUtils;
import org.sonar.java.model.SELineUtils;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.checks.LocksNotUnlockedCheck;
import org.sonar.java.se.checks.NoWayOutLoopCheck;
import org.sonar.java.se.checks.NonNullSetToNullCheck;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.OptionalGetBeforeIsPresentCheck;
import org.sonar.java.se.checks.RedundantAssignmentsCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.checks.StreamConsumedCheck;
import org.sonar.java.se.checks.UnclosedResourcesCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph.Block;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonarsource.performance.measure.PerformanceMeasure;

import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;

public class ExplodedGraphWalker {

  /**
   * Arbitrary number to limit symbolic execution.
   */
  private static final int MAX_STEPS = 16_000;
  public static final int MAX_NESTED_BOOLEAN_STATES = 10_000;
  // would correspond to 10 parameters annotated with @Nullable
  private static final int MAX_STARTING_STATES = 1_024;
  private static final Set<String> THIS_SUPER = SetUtils.immutableSetOf("this", "super");

  // VisibleForTesting
  static final int MAX_EXEC_PROGRAM_POINT = 2;

  private static final MethodMatchers SYSTEM_EXIT_MATCHER = MethodMatchers.create().ofTypes("java.lang.System").names("exit").addParametersMatcher("int").build();
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final MethodMatchers.NameBuilder JAVA_LANG_OBJECT_SUBTYPE = MethodMatchers.create().ofSubTypes(JAVA_LANG_OBJECT);
  private static final MethodMatchers OBJECT_WAIT_MATCHER = JAVA_LANG_OBJECT_SUBTYPE.names("wait")
    .addWithoutParametersMatcher()
    .addParametersMatcher("long")
    .addParametersMatcher("long", "int")
    .build();
  private static final MethodMatchers GET_CLASS_MATCHER = JAVA_LANG_OBJECT_SUBTYPE.names("getClass").addWithoutParametersMatcher().build();
  private static final MethodMatchers THREAD_SLEEP_MATCHER = MethodMatchers.create().ofTypes("java.lang.Thread").names("sleep").withAnyParameters().build();
  private static final MethodMatchers EQUALS = MethodMatchers.create().ofAnyType().names("equals").addParametersMatcher(JAVA_LANG_OBJECT).build();
  public static final MethodMatchers EQUALS_METHODS = MethodMatchers.or(
    EQUALS,
    MethodMatchers.create().ofTypes("java.util.Objects").names("equals").withAnyParameters().build());
  public static final MethodMatchers BOOLEAN_VALUE_MATCHER = MethodMatchers.create().ofTypes("java.lang.Boolean")
    .names("booleanValue").addWithoutParametersMatcher().build();

  private final AlwaysTrueOrFalseExpressionCollector alwaysTrueOrFalseExpressionCollector;
  private MethodTree methodTree;

  private ExplodedGraph explodedGraph;

  // VisibleForTesting
  Deque<ExplodedGraph.Node> workList;
  ExplodedGraph.Node node;
  ProgramPoint programPosition;
  ProgramState programState;
  private SELiveVariables liveVariables;
  // VisibleForTesting
  CheckerDispatcher checkerDispatcher;
  private Block exitBlock;

  private final Sema semanticModel;
  private final BehaviorCache behaviorCache;
  // VisibleForTesting
  int steps;

  ConstraintManager constraintManager;
  private boolean cleanup = true;
  @Nullable
  MethodBehavior methodBehavior;
  private Set<ExplodedGraph.Node> endOfExecutionPath;

  public static class ExplodedGraphTooBigException extends RuntimeException {

    public ExplodedGraphTooBigException(String s) {
      super(s);
    }
  }

  public static class MaximumStepsReachedException extends RuntimeException {

    public MaximumStepsReachedException(String s) {
      super(s);
    }

    public MaximumStepsReachedException(String s, RuntimeException e) {
      super(s, e);
    }
  }

  public static class TooManyNestedBooleanStatesException extends RuntimeException {
  }

  public static class MaximumStartingStatesException extends RuntimeException {
    public MaximumStartingStatesException(String s) {
      super(s);
    }
  }

  // VisibleForTesting
  public ExplodedGraphWalker(BehaviorCache behaviorCache, JavaFileScannerContext context) {
    List<SECheck> checks = Arrays.asList(new NullDereferenceCheck(), new DivisionByZeroCheck(),
      new UnclosedResourcesCheck(), new LocksNotUnlockedCheck(), new NonNullSetToNullCheck(), new NoWayOutLoopCheck());
    this.alwaysTrueOrFalseExpressionCollector = new AlwaysTrueOrFalseExpressionCollector();
    this.checkerDispatcher = new CheckerDispatcher(this, checks, context);
    this.behaviorCache = behaviorCache;
    this.semanticModel = (Sema) context.getSemanticModel();
  }

  // VisibleForTesting
  ExplodedGraphWalker(BehaviorCache behaviorCache, JavaFileScannerContext context, boolean cleanup) {
    this(behaviorCache, context);
    this.cleanup = cleanup;
  }

  // VisibleForTesting
  protected ExplodedGraphWalker(List<SECheck> seChecks, BehaviorCache behaviorCache, JavaFileScannerContext context) {
    this.alwaysTrueOrFalseExpressionCollector = new AlwaysTrueOrFalseExpressionCollector();
    this.checkerDispatcher = new CheckerDispatcher(this, seChecks, context);
    this.behaviorCache = behaviorCache;
    this.semanticModel = (Sema) context.getSemanticModel();
  }

  public MethodBehavior visitMethod(MethodTree tree) {
    return visitMethod(tree, null);
  }

  public MethodBehavior visitMethod(MethodTree tree, @Nullable MethodBehavior methodBehavior) {
    Preconditions.checkArgument(methodBehavior == null || !methodBehavior.isComplete() || !methodBehavior.isVisited(), "Trying to execute an already visited methodBehavior");
    this.methodBehavior = methodBehavior;
    BlockTree body = tree.block();
    if (body != null) {
      execute(tree);
    }
    return this.methodBehavior;
  }

  private void execute(MethodTree tree) {
    PerformanceMeasure.Duration cfgDuration = PerformanceMeasure.start("cfg");
    ControlFlowGraph cfg = tree.cfg();
    exitBlock = cfg.exitBlock();
    cfgDuration.stop();
    if (!cfg.hasCompleteSemantic()) {
      return;
    }

    checkerDispatcher.init(tree, cfg);

    PerformanceMeasure.Duration liveVariablesDuration = PerformanceMeasure.start("SELiveVariables.analyze");
    liveVariables = SELiveVariables.analyze(cfg);
    liveVariablesDuration.stop();

    explodedGraph = new ExplodedGraph();
    methodTree = tree;
    constraintManager = new ConstraintManager();
    workList = new LinkedList<>();
    // Linked hashSet is required to guarantee order of yields to be generated
    endOfExecutionPath = new LinkedHashSet<>();
    programState = ProgramState.EMPTY_STATE;
    steps = 0;
    enqueueStartingStates(tree, cfg);
    while (!workList.isEmpty()) {
      steps++;
      throwExceptionIfMaxStepsHasBeenReached(tree);
      // LIFO:
      setNode(workList.removeFirst());
      Block block = programPosition.block;
      if (block.successors().isEmpty()) {
        endOfExecutionPath.add(node);
        continue;
      }
      try {
        Tree terminator = block.terminator();
        if (programPosition.i < block.elements().size()) {
          // process block element
          visit(block.elements().get(programPosition.i), terminator);
        } else if (terminator == null) {
          // process block exit, which is unconditional jump such as goto-statement or return-statement
          handleBlockExit(programPosition);
        } else if (programPosition.i == block.elements().size()) {
          // process block exist, which is conditional jump such as if-statement
          PerformanceMeasure.Duration postStatementDuration = PerformanceMeasure.start("PostStatement");
          checkerDispatcher.executeCheckPostStatement(terminator);
          postStatementDuration.stop();
        } else {
          // process branch
          // process block exist, which is conditional jump such as if-statement
          PerformanceMeasure.Duration preStatementDuration = PerformanceMeasure.start("PreStatement");
          checkerDispatcher.executeCheckPreStatement(terminator);
          preStatementDuration.stop();
          PerformanceMeasure.Duration handleBlockExitDuration = PerformanceMeasure.start("handleBlockExit");
          handleBlockExit(programPosition);
          handleBlockExitDuration.stop();
        }
      } catch (TooManyNestedBooleanStatesException e) {
        throwTooManyBooleanStates(tree, e);
      } catch (RelationalSymbolicValue.TransitiveRelationExceededException e) {
        throwTooManyTransitiveRelationsException(tree, e);
      }
    }

    handleEndOfExecutionPath(false);
    PerformanceMeasure.Duration endOfExecutionDuration = PerformanceMeasure.start("EndOfExecution");
    checkerDispatcher.executeCheckEndOfExecution();
    endOfExecutionDuration.stop();
    // Cleanup:
    workList = null;
    node = null;
    programState = null;
    constraintManager = null;

    if (methodBehavior != null){
      methodBehavior.completed();
    }
  }

  private void enqueueStartingStates(MethodTree tree, ControlFlowGraph cfg) {
    for (ProgramState startingState : startingStates(tree, programState)) {
      enqueue(new ProgramPoint(cfg.entryBlock()), startingState);
    }
  }

  private void throwTooManyTransitiveRelationsException(MethodTree tree, RelationalSymbolicValue.TransitiveRelationExceededException e) {
    String message = String.format("reached maximum number of transitive relations generated for method %s in class %s",
      tree.simpleName().name(), tree.symbol().owner().name());
    MaximumStepsReachedException cause = new MaximumStepsReachedException(message, e);
    interrupted(cause);
    throw cause;
  }

  private void throwTooManyBooleanStates(MethodTree tree, TooManyNestedBooleanStatesException e) {
    String message = String.format("reached maximum number of %d branched states for method %s in class %s",
      MAX_NESTED_BOOLEAN_STATES, tree.simpleName().name(), tree.symbol().owner().name());
    MaximumStepsReachedException cause = new MaximumStepsReachedException(message, e);
    interrupted(cause);
    throw cause;
  }

  private void throwExceptionIfMaxStepsHasBeenReached(MethodTree tree) {
    if (steps > maxSteps()) {
      String message = String.format("reached limit of %d steps for method %s#%d in class %s",
        maxSteps(), tree.simpleName().name(), SELineUtils.startLine(tree.simpleName()), tree.symbol().owner().name());
      MaximumStepsReachedException cause = new MaximumStepsReachedException(message);
      interrupted(cause);
      throw cause;
    }
  }

  private void interrupted(Exception cause) {
    handleEndOfExecutionPath(true);
    checkerDispatcher.interruptedExecution(cause);
  }

  private void setNode(ExplodedGraph.Node node) {
    this.node = node;
    programPosition = this.node.programPoint;
    programState = this.node.programState;
  }

  private void handleEndOfExecutionPath(boolean interrupted) {
    ExplodedGraph.Node savedNode = node;
    endOfExecutionPath.forEach(n -> {
      setNode(n);
      checkerDispatcher.executeCheckEndOfExecutionPath(constraintManager);
      if (!interrupted && methodBehavior != null) {
        methodBehavior.createYield(node);
      }
    });
    setNode(savedNode);
  }

  public void addExceptionalYield(SymbolicValue target, ProgramState exceptionalState, String exceptionFullyQualifiedName, SECheck check) {
    // in order to create such Exceptional Yield, a parameter of the method has to be the cause of the exception
    if (methodBehavior != null && methodBehavior.parameters().contains(target)) {
      Type exceptionType = semanticModel.getClassType(exceptionFullyQualifiedName);
      ProgramState newExceptionalState = exceptionalState.clearStack().stackValue(constraintManager.createExceptionalSymbolicValue(exceptionType));
      ExplodedGraph.Node exitNode = explodedGraph.node(node.programPoint, newExceptionalState);
      methodBehavior.createExceptionalCheckBasedYield(target, exitNode, exceptionFullyQualifiedName, check);
      exitNode.addParent(node, null);
    }
  }

  private Iterable<ProgramState> startingStates(MethodTree tree, ProgramState currentState) {
    Stream<ProgramState> stateStream = Stream.of(currentState);
    int numberStartingStates = 1;
    boolean isEqualsMethod = EQUALS.matches(tree);
    boolean hasMethodBehavior = methodBehavior != null;

    for (final VariableTree variableTree : methodOrRecordConstructorParameters(tree)) {
      final SymbolicValue sv = constraintManager.createSymbolicValue(variableTree);
      Symbol variableSymbol = variableTree.symbol();
      if (hasMethodBehavior) {
        methodBehavior.addParameter(sv);
      }
      stateStream = stateStream.map(ps -> ps.put(variableSymbol, sv));
      if (isEqualsMethod || parameterCanBeNull(variableSymbol)) {
        // each nullable parameter generate 2 starting states, combined with all the others
        numberStartingStates *= 2;
        if (numberStartingStates > MAX_STARTING_STATES) {
          throwMaximumStartingStates(methodTree);
        }
        stateStream = stateStream.flatMap((ProgramState ps) ->
          Stream.concat(
            sv.setConstraint(ps, ObjectConstraint.NULL).stream(),
            sv.setConstraint(ps, ObjectConstraint.NOT_NULL).stream()
            ));
      } else if (variableSymbol.metadata().nullabilityData().isNonNull(PACKAGE, false, false)) {
        stateStream = stateStream.flatMap(ps -> sv.setConstraint(ps, ObjectConstraint.NOT_NULL).stream());
      }
    }
    return stateStream.toList();
  }

  private static List<VariableTree> methodOrRecordConstructorParameters(MethodTree methodTree) {
    Tree parent = methodTree.parent();
    if (parent.kind() == Tree.Kind.RECORD && methodTree.openParenToken() == null) {
      return ((ClassTree) parent).recordComponents();
    }
    return methodTree.parameters();
  }

  private static void throwMaximumStartingStates(MethodTree tree) {
    String message = String.format("reached maximum number of %d starting states for method %s in class %s",
      MAX_STARTING_STATES, tree.simpleName().name(), tree.symbol().owner().name());
    throw new MaximumStartingStatesException(message);
  }

  private static boolean parameterCanBeNull(Symbol variableSymbol) {
    if (variableSymbol.type().isPrimitive()) {
      return false;
    }
    return variableSymbol.metadata().nullabilityData().isNullable(PACKAGE, false, false);
  }

  private void cleanUpProgramState(Block block) {
    if (cleanup) {
      Collection<SymbolicValue> protectedSVs = methodBehavior == null ? Collections.emptyList() : methodBehavior.parameters();
      programState = programState.cleanupDeadSymbols(liveVariables.getOut(block), protectedSVs);
      programState = programState.cleanupConstraints(protectedSVs);
    }
  }

  private void handleBlockExit(ProgramPoint programPosition) {
    Block block = programPosition.block;
    Tree terminator = block.terminator();
    cleanUpProgramState(block);
    boolean exitPath = node.exitPath;
    if (terminator != null) {
      switch (terminator.kind()) {
        case IF_STATEMENT:
          ExpressionTree ifCondition = ((IfStatementTree) terminator).condition();
          handleBranch(block, cleanupCondition(ifCondition), verifyCondition(ifCondition));
          return;
        case SWITCH_STATEMENT:
          handleSwitch(block, ((SwitchStatementTree) terminator).cases());
          return;
        case SWITCH_EXPRESSION:
          handleSwitch(block, ((SwitchExpressionTree) terminator).cases());
          return;
        case CONDITIONAL_OR,
          CONDITIONAL_AND:
          handleBranch(block, ((BinaryExpressionTree) terminator).leftOperand());
          return;
        case CONDITIONAL_EXPRESSION:
          handleBranch(block, ((ConditionalExpressionTree) terminator).condition());
          return;
        case FOR_STATEMENT:
          ExpressionTree condition = ((ForStatementTree) terminator).condition();
          if (condition != null) {
            handleBranch(block, condition, false);
            return;
          }
          break;
        case WHILE_STATEMENT:
          ExpressionTree whileCondition = ((WhileStatementTree) terminator).condition();
          handleBranch(block, cleanupCondition(whileCondition), verifyCondition(whileCondition));
          return;
        case DO_STATEMENT:
          ExpressionTree doCondition = ((DoWhileStatementTree) terminator).condition();
          handleBranch(block, cleanupCondition(doCondition), verifyCondition(doCondition));
          return;
        case SYNCHRONIZED_STATEMENT:
          resetFieldValues(false);
          break;
        case RETURN_STATEMENT:
          ExpressionTree returnExpression = ((ReturnStatementTree) terminator).expression();
          if (returnExpression != null) {
            programState.storeExitValue();
          }
          break;
        case THROW_STATEMENT:
          ProgramState.Pop unstack = programState.unstackValue(1);
          SymbolicValue sv = unstack.values.get(0);
          SymbolicValue.ExceptionalSymbolicValue exceptionalSV;
          if (sv instanceof SymbolicValue.CaughtExceptionSymbolicValue caughtExceptionSymbolicValue) {
            // retrowing the exception from a catch block
            exceptionalSV = caughtExceptionSymbolicValue.exception();
          } else {
            exceptionalSV = constraintManager.createExceptionalSymbolicValue(((ThrowStatementTree) terminator).expression().symbolType());
          }
          programState = unstack.state.stackValue(exceptionalSV);
          programState.storeExitValue();
          break;
        default:
          // do nothing by default.
      }
    }
    // unconditional jumps, for-statement, synchronized:
    if (exitPath) {
      if (block.exitBlock() != null) {
        enqueue(new ProgramPoint(block.exitBlock()), programState, true);
      } else {
        for (Block successor : block.successors()) {
          enqueue(new ProgramPoint(successor), programState, true);
        }
      }

    } else {
      for (Block successor : block.successors()) {
        if (!block.isFinallyBlock() || isDirectFlowSuccessorOf(successor, block)) {
          enqueue(new ProgramPoint(successor), programState, successor == block.exitBlock());
        }
      }
    }
  }

  private static boolean verifyCondition(ExpressionTree condition) {
    if(condition.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) condition;
      if(identifierTree.symbol().isFinal() && identifierTree.symbol().isVariableSymbol()) {
        VariableTree declaration = (VariableTree) identifierTree.symbol().declaration();
        return declaration == null || declaration.initializer() == null || !declaration.initializer().is(Tree.Kind.BOOLEAN_LITERAL);
      }
    }
    return !condition.is(Tree.Kind.BOOLEAN_LITERAL);
  }

  private static boolean isDirectFlowSuccessorOf(Block successor, Block block) {
    return successor != block.exitBlock() || (block.successors().size() == 1 && CFGUtils.isMethodExitBlock(successor));
  }

  /**
   * Required for accurate reporting.
   * If condition is && or || expression, then return its right operand.
   */
  private static ExpressionTree cleanupCondition(ExpressionTree condition) {
    ExpressionTree cleanedUpCondition = SEExpressionUtils.skipParentheses(condition);
    if (cleanedUpCondition.is(Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR)) {
      cleanedUpCondition = cleanupCondition(((BinaryExpressionTree) cleanedUpCondition).rightOperand());
    }
    return cleanedUpCondition;
  }

  private void handleSwitch(Block programPosition, List<CaseGroupTree> caseGroups) {
    ProgramState state = programState;

    Map<CaseGroupTree, List<ProgramState.SymbolicValueSymbol>> caseValues = new HashMap<>();
    for (CaseGroupTree caseGroup : ListUtils.reverse(caseGroups)) {
      // FIXME this logic do not work with patterns from JDK17+, as we need to consume the pattern adequately.
      // The NULL_PATTERN can lead to NPE in the block, as it is equivalent to if (o == null) { ... }
      // also, there is a good chance that the stack would be corrupted when patterns are used.
      int numberOfCaseValues = caseGroup.labels()
        .stream()
        .map(CaseLabelTree::expressions)
        .mapToInt(List::size)
        .sum();
      ProgramState.Pop poppedCaseValues = state.unstackValue(numberOfCaseValues);
      state = poppedCaseValues.state;
      caseValues.put(caseGroup, poppedCaseValues.valuesAndSymbols);
    }

    ProgramState.Pop poppedSwitchValue = state.unstackValue(1);
    ProgramState.SymbolicValueSymbol switchValue = poppedSwitchValue.valuesAndSymbols.get(0);

    ProgramState elseState = poppedSwitchValue.state;
    // The block that will be taken when all case-conditions are false. This will either be the default-block or, if no
    // default block exists, the block after the switch statement.
    Block elseBlock = null;
    for (Block successor : programPosition.successors()) {
      CaseGroupTree caseGroup = successor.caseGroup();
      if (caseGroup == null || !caseValues.containsKey(caseGroup)) {
        Preconditions.checkState(elseBlock == null);
        elseBlock = successor;
        continue;
      }

      for (ProgramState.SymbolicValueSymbol caseValue : caseValues.get(caseGroup)) {
        SymbolicValue equality = constraintManager.createEquality(switchValue, caseValue);
        ProgramState ps = setConstraint(state, equality, BooleanConstraint.TRUE);
        enqueue(new ProgramPoint(successor), ps, node.exitPath);
        elseState = setConstraint(elseState, equality, BooleanConstraint.FALSE);
      }
      if (successor.isDefaultBlock()) {
        Preconditions.checkState(elseBlock == null);
        elseBlock = successor;
      }
    }
    if (elseBlock != null) {
      // for a switch covering all items from an enum, there will be no elseState
      enqueue(new ProgramPoint(elseBlock), elseState, node.exitPath);
    }
  }

  private static ProgramState setConstraint(ProgramState state, SymbolicValue condition, BooleanConstraint constraint) {
    List<ProgramState> states = condition.setConstraint(state, constraint);
    if (states.isEmpty()) {
      return state;
    }
    Preconditions.checkState(states.size() == 1);
    return states.get(0);
  }

  private void handleBranch(Block programPosition, Tree condition) {
    handleBranch(programPosition, condition, true);
  }

  private void handleBranch(Block programPosition, Tree condition, boolean checkPath) {
    Pair<List<ProgramState>, List<ProgramState>> pair = constraintManager.assumeDual(programState);
    ProgramPoint falseBlockProgramPoint = new ProgramPoint(programPosition.falseBlock());
    for (ProgramState state : pair.a) {
      ProgramState ps = state;
      if (condition.parent().is(Tree.Kind.CONDITIONAL_AND) && !isConditionBranchingExpressionOrStatement(condition)) {
        // push a FALSE value on the top of the stack to enforce the choice of the branch,
        // as non-reachable symbolic values won't get a TRUE/FALSE constraint when assuming dual
        ps = state.stackValue(SymbolicValue.FALSE_LITERAL);
      }
      // enqueue false-branch, if feasible
      enqueue(falseBlockProgramPoint, ps, node.exitPath);
      if (checkPath) {
        alwaysTrueOrFalseExpressionCollector.evaluatedToFalse(cleanupCondition((ExpressionTree) condition), node);
      }
    }
    ProgramPoint trueBlockProgramPoint = new ProgramPoint(programPosition.trueBlock());
    for (ProgramState state : pair.b) {
      ProgramState ps = state;
      if (condition.parent().is(Tree.Kind.CONDITIONAL_OR) && !isConditionBranchingExpressionOrStatement(condition)) {
        // push a TRUE value on the top of the stack to enforce the choice of the branch,
        // as non-reachable symbolic values won't get a TRUE/FALSE constraint when assuming dual
        ps = state.stackValue(SymbolicValue.TRUE_LITERAL);
      }
      // enqueue true-branch, if feasible
      enqueue(trueBlockProgramPoint, ps, node.exitPath);
      if (checkPath) {
        alwaysTrueOrFalseExpressionCollector.evaluatedToTrue(cleanupCondition((ExpressionTree) condition), node);
      }
    }
  }

  private static boolean isConditionBranchingExpressionOrStatement(Tree tree) {
    Tree current;
    Tree parent = tree;
    do {
      current = parent;
      parent = parent.parent();
    } while (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION, Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR));

    switch (parent.kind()) {
      case CONDITIONAL_EXPRESSION:
        return current.equals(((ConditionalExpressionTree) parent).condition());
      case IF_STATEMENT:
        return current.equals(((IfStatementTree) parent).condition());
      case WHILE_STATEMENT:
        return current.equals(((WhileStatementTree) parent).condition());
      case FOR_STATEMENT:
        return current.equals(((ForStatementTree) parent).condition());
      case DO_STATEMENT:
        return current.equals(((DoWhileStatementTree) parent).condition());
      default:
        return false;
    }
  }

  private void visit(Tree tree, @Nullable Tree terminator) {
    if (!checkerDispatcher.executeCheckPreStatement(tree)) {
      // Some of the check pre statement sink the execution on this node.
      return;
    }
    switch (tree.kind()) {
      case METHOD_INVOCATION:
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        if(SYSTEM_EXIT_MATCHER.matches(mit)) {
          //System exit is a sink of execution
          return;
        }
        executeMethodInvocation(mit);
        return;
      case LABELED_STATEMENT,
        SWITCH_STATEMENT,
        EXPRESSION_STATEMENT,
        PARENTHESIZED_EXPRESSION:
        throw new IllegalStateException("Cannot appear in ControlFlowGraph: " + tree.kind().name());
      case VARIABLE:
        executeVariable((VariableTree) tree, terminator);
        break;
      case TYPE_CAST:
        executeTypeCast((TypeCastTree) tree);
        break;
      case ASSIGNMENT,
        MULTIPLY_ASSIGNMENT,
        DIVIDE_ASSIGNMENT,
        REMAINDER_ASSIGNMENT,
        PLUS_ASSIGNMENT,
        MINUS_ASSIGNMENT,
        LEFT_SHIFT_ASSIGNMENT,
        RIGHT_SHIFT_ASSIGNMENT,
        UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        executeAssignment((AssignmentExpressionTree) tree);
        break;
      case AND_ASSIGNMENT,
        XOR_ASSIGNMENT,
        OR_ASSIGNMENT:
        executeLogicalAssignment((AssignmentExpressionTree) tree);
        break;
      case ARRAY_ACCESS_EXPRESSION:
        executeArrayAccessExpression((ArrayAccessExpressionTree) tree);
        break;
      case NEW_ARRAY:
        executeNewArray((NewArrayTree) tree);
        break;
      case NEW_CLASS:
        executeNewClass((NewClassTree) tree);
        break;
      case MULTIPLY,
        DIVIDE,
        REMAINDER,
        PLUS,
        MINUS,
        LEFT_SHIFT,
        RIGHT_SHIFT,
        UNSIGNED_RIGHT_SHIFT,
        AND,
        XOR,
        OR,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL_TO,
        LESS_THAN,
        LESS_THAN_OR_EQUAL_TO,
        EQUAL_TO,
        NOT_EQUAL_TO:
        executeBinaryExpression(tree);
        break;
      case POSTFIX_INCREMENT,
        POSTFIX_DECREMENT,
        PREFIX_INCREMENT,
        PREFIX_DECREMENT,
        UNARY_MINUS,
        UNARY_PLUS,
        BITWISE_COMPLEMENT,
        LOGICAL_COMPLEMENT,
        INSTANCE_OF,
        PATTERN_INSTANCE_OF:
        executeUnaryExpression(tree);
        break;
      case IDENTIFIER:
        executeIdentifier((IdentifierTree) tree);
        break;
      case MEMBER_SELECT:
        executeMemberSelect((MemberSelectExpressionTree) tree);
        break;
      case INT_LITERAL,
        LONG_LITERAL,
        FLOAT_LITERAL,
        DOUBLE_LITERAL,
        CHAR_LITERAL,
        TEXT_BLOCK,
        STRING_LITERAL:
        SymbolicValue val = constraintManager.createSymbolicValue(tree);
        programState = programState.stackValue(val);
        programState = programState.addConstraint(val, ObjectConstraint.NOT_NULL);
        break;
      case BOOLEAN_LITERAL:
        boolean value = Boolean.parseBoolean(((LiteralTree) tree).value());
        programState = programState.stackValue(value ? SymbolicValue.TRUE_LITERAL : SymbolicValue.FALSE_LITERAL);
        break;
      case NULL_LITERAL:
        programState = programState.stackValue(SymbolicValue.NULL_LITERAL);
        break;
      case LAMBDA_EXPRESSION,
        METHOD_REFERENCE,
        SWITCH_EXPRESSION:
        programState = programState.stackValue(constraintManager.createSymbolicValue(tree));
        break;
      case DEFAULT_PATTERN:
        programState = programState.stackValue(constraintManager.createSymbolicValue(tree));
        break;
      case TYPE_PATTERN:
        programState = programState.stackValue(constraintManager.createSymbolicValue(tree));
        break;
      case NULL_PATTERN:
        // FIXME we could add a null constraint to the argument of the switch, since it should be considered being null in this branch
        programState = programState.stackValue(constraintManager.createSymbolicValue(tree));
        break;
      case GUARDED_PATTERN:
        // push a random SV, just to be consumed
        programState = programState.stackValue(constraintManager.createSymbolicValue(tree));
        break;
      case ASSERT_STATEMENT:
        executeAssertStatement(tree);
        return;
      default:
    }

    checkerDispatcher.executeCheckPostStatement(tree);
  }

  private void executeAssertStatement(Tree tree) {
    // After an assert statement we know that the evaluated expression is true.
    ProgramState.Pop pop = programState.unstackValue(1);
    pop.values.forEach(v -> v.setConstraint(pop.state, BooleanConstraint.TRUE)
      .forEach(ps -> {
        checkerDispatcher.syntaxNode = tree;
        checkerDispatcher.addTransition(ps);
        ps.clearStack();
      }));
  }

  private void executeMethodInvocation(MethodInvocationTree mit) {
    setSymbolicValueOnFields(mit);
    // unstack arguments and method identifier
    ProgramState.Pop unstack = programState.unstackValue(mit.arguments().size() + 1);

    programState = unstack.state;

    // get method behavior for method with known declaration (ie: within the same file)
    MethodBehavior methodInvokedBehavior = null;
    Symbol.MethodSymbol methodSymbol = mit.methodSymbol();
    if(!methodSymbol.isUnknown()) {
      methodInvokedBehavior = behaviorCache.get(methodSymbol);
    }

    // Enqueue additional exceptional paths corresponding to unchecked exceptions, for instance OutOfMemoryError
    enqueueUncheckedExceptionalPaths(methodSymbol);

    final SymbolicValue resultValue = constraintManager.createMethodSymbolicValue(mit, unstack.valuesAndSymbols);
    if (methodInvokedBehavior != null
      && methodInvokedBehavior.isComplete()
      && !EQUALS_METHODS.matches(mit)) {
      List<SymbolicValue> invocationArguments = invocationArguments(unstack.values);
      List<Type> invocationTypes = mit.arguments().stream().map(ExpressionTree::symbolType).toList();

      Map<Type, SymbolicValue.ExceptionalSymbolicValue> thrownExceptionsByExceptionType = new HashMap<>();

      // Enqueue exceptional paths from exceptional yields
      methodInvokedBehavior.exceptionalPathYields()
        .forEach(methodYield -> methodYield.statesAfterInvocation(
          invocationArguments,
          invocationTypes,
          programState,
          () -> thrownExceptionsByExceptionType.computeIfAbsent(methodYield.exceptionType(semanticModel), constraintManager::createExceptionalSymbolicValue))
          .forEach(psYield -> enqueueExceptionalPaths(psYield, methodSymbol, methodYield)));

      // Enqueue happy paths
      methodInvokedBehavior.happyPathYields()
        .forEach(methodYield ->
          methodYield.statesAfterInvocation(invocationArguments, invocationTypes, programState, () -> resultValue)
            .map(psYield -> handleSpecialMethods(psYield, mit))
            .forEach(psYield -> enqueueHappyPath(psYield, mit,  methodYield)));
    } else {
      // Enqueue exceptional paths from thrown exceptions
      enqueueThrownExceptionalPaths(methodSymbol);

      // Enqueue happy paths
      programState = handleSpecialMethods(programState.stackValue(resultValue), mit);
      checkerDispatcher.executeCheckPostStatement(mit);
      cleanupStack(mit);
    }
  }

  private void enqueueHappyPath(ProgramState programState, MethodInvocationTree mit, MethodYield methodYield) {
    checkerDispatcher.syntaxNode = mit;
    checkerDispatcher.methodYield = methodYield;
    checkerDispatcher.addTransition(programState);
    checkerDispatcher.methodYield = null;
  }

  private ProgramState handleSpecialMethods(ProgramState ps, MethodInvocationTree mit) {
    if (mit.methodSymbol().metadata().nullabilityData().isNonNull(PACKAGE, false, false)) {
      return ps.addConstraint(ps.peekValue(), ObjectConstraint.NOT_NULL);
    } else if (OBJECT_WAIT_MATCHER.matches(mit)) {
      return ps.resetFieldValues(constraintManager, false);
    }
    return ps;
  }

  private void enqueueThrownExceptionalPaths(Symbol.MethodSymbol symbol) {
    if (symbol.isUnknown()) {
      // do nothing for unknown methods
      return;
    }
    ProgramState ps = programState.clearStack();
    symbol.thrownTypes().stream()
      .map(constraintManager::createExceptionalSymbolicValue)
      .map(ps::stackValue)
      .forEach(ps1 -> enqueueExceptionalPaths(ps1, symbol));
  }

  private void enqueueUncheckedExceptionalPaths(Symbol methodSymbol) {
    enqueueExceptionalPaths(programState.clearStack().stackValue(constraintManager.createExceptionalSymbolicValue(null)), methodSymbol);
  }

  private void enqueueExceptionalPaths(ProgramState ps, Symbol methodSymbol) {
    enqueueExceptionalPaths(ps, methodSymbol, null);
  }

  private void enqueueExceptionalPaths(ProgramState ps, Symbol methodSymbol, @Nullable MethodYield methodYield) {
    Set<Block> exceptionBlocks = (Set<Block>) node.programPoint.block.exceptions();
    List<Block> catchBlocks = exceptionBlocks.stream().filter(CFGUtils.IS_CATCH_BLOCK).toList();
    SymbolicValue peekValue = ps.peekValue();

    Preconditions.checkState(peekValue instanceof SymbolicValue.ExceptionalSymbolicValue, "Top of stack should always contains exceptional SV");
    SymbolicValue.ExceptionalSymbolicValue exceptionSV = (SymbolicValue.ExceptionalSymbolicValue) peekValue;
    // only consider the first match, as order of catch block is important
    List<Block> caughtBlocks = catchBlocks.stream()
      .filter(b -> isCaughtByBlock(exceptionSV.exceptionType(), b))
      .sorted((b1, b2) -> Integer.compare(b2.id(), b1.id()))
      .toList();
    if (!caughtBlocks.isEmpty()) {
      caughtBlocks.forEach(b -> enqueue(new ProgramPoint(b), ps, methodYield));
      return;
    }

    // branch to any unchecked exception catch
    catchBlocks.stream()
      .filter(ExplodedGraphWalker::isCatchingUncheckedException)
      .forEach(b -> enqueue(new ProgramPoint(b), ps, methodYield));

    // store the exception as exit value in case of method exit in next block
    ps.storeExitValue();

    // use other exceptional blocks, i.e. finally block and exit blocks
    List<Block> otherBlocks = exceptionBlocks.stream()
      .filter(CFGUtils.IS_CATCH_BLOCK.negate().or(b -> methodSymbol.isUnknown()))
      .toList();
    if (otherBlocks.isEmpty()) {
      // explicitly add the exception branching to method exit
      Block methodExit = node.programPoint.block.successors()
        .stream()
        .map(b -> (Block) b)
        .filter(CFGUtils::isMethodExitBlock)
        .findFirst()
        .orElse(exitBlock);
      enqueue(new ProgramPoint(methodExit), ps, true, methodYield);
    } else {
      var stateWithoutException = ps.unstackValue(1).state.withEntryException(exceptionSV);
      otherBlocks.forEach(b -> enqueue(new ProgramPoint(b), stateWithoutException, true, methodYield));
    }
  }

  private static boolean isCaughtByBlock(@Nullable Type thrownType, Block catchBlock) {
    if (thrownType != null) {
      Type caughtType = ((VariableTree) catchBlock.elements().get(0)).symbol().type();
      return thrownType.isSubtypeOf(caughtType) || caughtType.isSubtypeOf(thrownType);
    }
    return false;
  }

  private static boolean isCatchingUncheckedException(Block catchBlock) {
    Type caughtType = ((VariableTree) catchBlock.elements().get(0)).symbol().type();
    return ExceptionUtils.isUncheckedException(caughtType);
  }

  private static List<SymbolicValue> invocationArguments(List<SymbolicValue> values) {
    return ListUtils.reverse(values.subList(0, values.size() - 1));
  }

  /**
   * @see JLS8 4.12.5 for details
   */
  private void executeVariable(VariableTree variableTree, @Nullable Tree terminator) {
    Symbol variableSymbol = variableTree.symbol();
    if (variableTree.initializer() == null) {
      SymbolicValue sv;
      if (terminator != null && terminator.is(Tree.Kind.FOR_EACH_STATEMENT)) {
        sv = constraintManager.createSymbolicValue(variableTree);
        if (variableSymbol.metadata().nullabilityData().isNonNull(PACKAGE, false, false)) {
          programState = programState.addConstraint(sv, ObjectConstraint.NOT_NULL);
        }
      } else {
        sv = executeVariableSpecialParent(variableTree, variableSymbol);
      }
      if (sv != null) {
        programState = programState.put(variableSymbol, sv);
      }
    } else {
      ProgramState.Pop unstack = programState.unstackValue(1);
      programState = unstack.state;
      programState = programState.put(variableSymbol, unstack.values.get(0));
    }
  }

  @Nullable
  private SymbolicValue executeVariableSpecialParent(VariableTree variableTree, Symbol variableSymbol) {
    Tree parent = variableTree.parent();
    SymbolicValue sv = null;
    if (parent.is(Tree.Kind.CATCH)) {
      sv = handleCatchVariable(variableSymbol.type());
      // an exception have been thrown and caught, stack must be cleared
      // see notes in JVMS 8 - §6.5. - instruction "athrow"
      programState = programState.clearStack();
      // exception variable is not null by definition
      programState = programState.addConstraint(sv, ObjectConstraint.NOT_NULL);
    } else if (parent.is(Tree.Kind.TYPE_PATTERN) && parent.parent().is(Tree.Kind.PATTERN_INSTANCE_OF)) {
      // FIXME only works with the TYPE_PATTERN inside an pattern instanceof
      // handling of all patterns as introduced in JDK17+ should also be supported
      // skip the unused SV corresponding to the type pattern
      programState = programState.unstackValue(1).state;
      SymbolicValue peekValue = programState.peekValue();
      if (peekValue != null) {
        // The new variable created from a PATTERN_INSTANCE_OF shares the same symbolic value as the current object.
        programState = programState.put(variableSymbol, peekValue);
      }
    }
    return sv;
  }

  private SymbolicValue handleCatchVariable(Type caughtType) {
    SymbolicValue peekValue = programState.peekValue();
    SymbolicValue.ExceptionalSymbolicValue sv = null;
    Type exceptionType = null;
    // FIXME SONARJAVA-2069 every path conducting to a catch block should have an exceptional symbolic value on top of the stack
    if (peekValue instanceof SymbolicValue.ExceptionalSymbolicValue exceptionalSymbolicValue) {
      sv = exceptionalSymbolicValue;
      exceptionType = sv.exceptionType();
    }
    if (exceptionType == null || exceptionType.isUnknown()) {
      // unknown exception, create an exception of the adequate type
      sv = constraintManager.createExceptionalSymbolicValue(caughtType);
    }
    // use a dedicated SV encapsulating the caught exception
    return constraintManager.createCaughtExceptionSymbolicValue(sv);
  }

  private void executeTypeCast(TypeCastTree typeCast) {
    Type type = typeCast.type().symbolType();
    if (type.isPrimitive()) {
      Type expType = typeCast.expression().symbolType();
      // create SV to consume factory if any
      SymbolicValue castSV = constraintManager.createSymbolicValue(typeCast);
      // if exp type is a primitive and subtype of cast type, we can reuse the same symbolic value
      if (!expType.isPrimitive() || !(expType == type || expType.isSubtypeOf(type))) {
        ProgramState.Pop unstack = programState.unstackValue(1);
        programState = unstack.state;
        programState = programState.stackValue(castSV);
      }
    }
  }

  private void executeAssignment(AssignmentExpressionTree tree) {
    ProgramState.Pop unstack;
    SymbolicValue value;

    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      unstack = SEExpressionUtils.isSimpleAssignment(tree) ? programState.unstackValue(1) : programState.unstackValue(2);
      value = unstack.values.get(0);
    } else {
      unstack = programState.unstackValue(2);
      value = constraintManager.createSymbolicValue(tree);
    }

    programState = unstack.state;
    Symbol symbol = null;
    if (tree.variable().is(Tree.Kind.IDENTIFIER) || SEExpressionUtils.isSelectOnThisOrSuper(tree)) {
      symbol = SEExpressionUtils.extractIdentifier(tree).symbol();
      programState = programState.put(symbol, value);
    }
    programState = programState.stackValue(value, symbol);
  }

  private void executeLogicalAssignment(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    // FIXME handle also assignments with this SONARJAVA-2242
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      ProgramState.Pop unstack = programState.unstackValue(2);
      ProgramState.SymbolicValueSymbol assignedTo = unstack.valuesAndSymbols.get(1);
      ProgramState.SymbolicValueSymbol value = unstack.valuesAndSymbols.get(0);
      programState = unstack.state;
      SymbolicValue symbolicValue = constraintManager.createBinarySymbolicValue(tree, Arrays.asList(assignedTo, value));
      Symbol symbol = ((IdentifierTree) variable).symbol();
      programState = programState.stackValue(symbolicValue, symbol);
      programState = programState.put(symbol, symbolicValue);
    }
  }

  private void executeArrayAccessExpression(ArrayAccessExpressionTree tree) {
    // unstack expression and dimension
    ProgramState.Pop unstack = programState.unstackValue(2);
    programState = unstack.state;
    programState = programState.stackValue(constraintManager.createSymbolicValue(tree));
  }

  private void executeNewArray(NewArrayTree newArrayTree) {
    int numberDimensions = (int) newArrayTree.dimensions().stream().map(ArrayDimensionTree::expression).filter(Objects::nonNull).count();
    programState = programState.unstackValue(numberDimensions).state;
    programState = programState.unstackValue(newArrayTree.initializers().size()).state;
    SymbolicValue svNewArray = constraintManager.createSymbolicValue(newArrayTree);
    programState = programState.stackValue(svNewArray);
    programState = svNewArray.setSingleConstraint(programState, ObjectConstraint.NOT_NULL);
  }

  private void executeNewClass(NewClassTree newClassTree) {
    programState = programState.unstackValue(newClassTree.arguments().size()).state;
    // Enqueue exceptional paths
    Symbol.MethodSymbol symbol = newClassTree.methodSymbol();
    if (node.programPoint.block.exceptions().stream().anyMatch(CFGUtils.IS_CATCH_BLOCK)) {
      // To avoid noise, we only add unchecked exceptional paths (includingUnknownException) when we are in a try-catch block.
      enqueueUncheckedExceptionalPaths(symbol);
    }
    enqueueThrownExceptionalPaths(symbol);
    SymbolicValue svNewClass = constraintManager.createSymbolicValue(newClassTree);
    programState = programState.stackValue(svNewClass);
    programState = svNewClass.setSingleConstraint(programState, ObjectConstraint.NOT_NULL);
  }

  private void executeBinaryExpression(Tree tree) {
    // Consume two and produce one SV.
    ProgramState.Pop unstackBinary = programState.unstackValue(2);
    programState = unstackBinary.state;
    SymbolicValue symbolicValue = constraintManager.createBinarySymbolicValue(tree, unstackBinary.valuesAndSymbols);
    Symbol symbolicValueSymbol = null;
    // Some binary expressions produce as a result the symbolic value of one of their operands, or a wrapped version of it.
    // For example: zero plus something returns something
    // In this case, it helps the flow computation to also attach the symbol that produces the initial symbolic value.
    if (tree.is(Tree.Kind.PLUS, Tree.Kind.MINUS, Tree.Kind.MULTIPLY, Tree.Kind.DIVIDE, Tree.Kind.REMAINDER)) {
      symbolicValueSymbol = unstackBinary.valuesAndSymbols.stream()
        .filter(e -> e.symbolicValue().wrappedValue().equals(symbolicValue.wrappedValue())).findFirst()
        .map(ProgramState.SymbolicValueSymbol::symbol)
        .orElse(null);
    }
    programState = programState.addConstraint(symbolicValue, ObjectConstraint.NOT_NULL);
    programState = programState.stackValue(symbolicValue, symbolicValueSymbol);
  }

  private void executeUnaryExpression(Tree tree) {
    // consume one and produce one
    ProgramState.Pop unstackUnary = programState.unstackValue(1);
    programState = unstackUnary.state;
    SymbolicValue unarySymbolicValue = constraintManager.createSymbolicValue(tree);
    unarySymbolicValue.computedFrom(unstackUnary.valuesAndSymbols);
    ProgramState.SymbolicValueSymbol symbolicValueSymbol = unstackUnary.valuesAndSymbols.get(0);
    if (tree.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT)) {
      programState = programState.stackValue(symbolicValueSymbol.sv, symbolicValueSymbol.symbol);
    } else {
      programState = programState.stackValue(unarySymbolicValue);
    }
    if (tree.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.PREFIX_INCREMENT)
      && symbolicValueSymbol.symbol != null) {
      programState = programState.put(symbolicValueSymbol.symbol, unarySymbolicValue);
    }
  }

  private void executeIdentifier(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    SymbolicValue value = programState.getValue(symbol);
    if (value == null) {
      value = constraintManager.createSymbolicValue(tree);
      programState = programState.stackValue(value, symbol);
      learnIdentifierConstraints(tree, value);
    } else {
      programState = programState.stackValue(value, symbol);
    }
    programState = programState.put(symbol, value);
  }

  private void learnIdentifierConstraints(IdentifierTree tree, SymbolicValue sv) {
    if (THIS_SUPER.contains(tree.name())) {
      programState = programState.addConstraint(sv, ObjectConstraint.NOT_NULL);
      return;
    }
    Tree declaration = tree.symbol().declaration();
    if (!isFinalField(tree.symbol()) || declaration == null) {
      return;
    }
    VariableTree variableTree = (VariableTree) declaration;
    ExpressionTree initializer = variableTree.initializer();
    if (initializer == null) {
      return;
    }
    // only check final field with an initializer
    initializer = SEExpressionUtils.skipParentheses(initializer);
    if (initializer.is(Tree.Kind.NULL_LITERAL)) {
      programState = programState.addConstraint(sv, ObjectConstraint.NULL);
    } else if (initializer.is(Tree.Kind.NEW_CLASS, Tree.Kind.NEW_ARRAY, Tree.Kind.STRING_LITERAL)
      || isNonNullMethodInvocation(initializer)
      || variableTree.symbol().type().isPrimitive()
      || initializer.symbolType().isPrimitive()) {
      programState = programState.addConstraint(sv, ObjectConstraint.NOT_NULL);
    }
  }

  private static boolean isFinalField(Symbol symbol) {
    return symbol.isVariableSymbol()
      && symbol.isFinal()
      && symbol.owner().isTypeSymbol();
  }

  private static boolean isNonNullMethodInvocation(ExpressionTree expr) {
    return expr.is(Tree.Kind.METHOD_INVOCATION)
      && ((MethodInvocationTree) expr).methodSymbol().metadata().nullabilityData().isNonNull(PACKAGE, false, false);
  }

  private void executeMemberSelect(MemberSelectExpressionTree mse) {
    if (!"class".equals(mse.identifier().name())) {

      ProgramState.Pop unstackMSE = programState.unstackValue(1);
      programState = unstackMSE.state;
    }

    Symbol symbol = mse.identifier().symbol();
    if (SEExpressionUtils.isSelectOnThisOrSuper(mse) || (symbol.isStatic() && symbol.isFinal())) {
      executeIdentifier(mse.identifier());
    } else {
      SymbolicValue mseValue = constraintManager.createSymbolicValue(mse);
      programState = programState.stackValue(mseValue);
    }
  }

  public void cleanupStack(Tree tree) {
    if (tree.parent().is(Tree.Kind.EXPRESSION_STATEMENT)) {
      programState = programState.unstackValue(1).state;

      // We set the entry exception to null as a workaround such that flow computations don't report on irrelevant nodes. We should
      // have a look at improving the flow computations to be able to remove this.
      programState = programState.withEntryException(null);
    }
  }

  private void setSymbolicValueOnFields(MethodInvocationTree tree) {
    boolean threadSleepMatch = THREAD_SLEEP_MATCHER.matches(tree);
    boolean providingThisAsArgument = isProvidingThisAsArgument(tree);
    if (isLocalMethodInvocation(tree) || providingThisAsArgument || threadSleepMatch) {
      boolean resetOnlyStaticFields = tree.methodSymbol().isStatic() && !threadSleepMatch && !providingThisAsArgument;
      resetFieldValues(resetOnlyStaticFields);
    }
  }

  private static boolean isLocalMethodInvocation(MethodInvocationTree tree) {
    if(GET_CLASS_MATCHER.matches(tree)) {
      return false;
    }
    ExpressionTree methodSelect = tree.methodSelect();
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      return true;
    } else if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpression = (MemberSelectExpressionTree) methodSelect;
      ExpressionTree target = memberSelectExpression.expression();
      if (target.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) target;
        return THIS_SUPER.contains(identifier.name());
      }
    }
    return false;
  }

  private static boolean isProvidingThisAsArgument(MethodInvocationTree tree) {
    return tree.arguments().stream().anyMatch(SEExpressionUtils::isThis);
  }

  private void resetFieldValues(boolean resetOnlyStaticFields) {
    programState = programState.resetFieldValues(constraintManager, resetOnlyStaticFields);
  }

  public void enqueue(ProgramPoint programPoint, ProgramState programState) {
    enqueue(programPoint, programState, false);
  }

  public void enqueue(ProgramPoint programPoint, ProgramState programState, @Nullable MethodYield methodYield) {
    enqueue(programPoint, programState, false, methodYield);
  }

  public void enqueue(ProgramPoint newProgramPoint, ProgramState programState, boolean exitPath) {
    enqueue(newProgramPoint, programState, exitPath, null);
  }

  public void enqueue(ProgramPoint newProgramPoint, ProgramState programState, boolean exitPath, @Nullable MethodYield methodYield) {
    ProgramPoint programPoint = newProgramPoint;

    int nbOfExecution = programState.numberOfTimeVisited(programPoint);
    if (nbOfExecution > MAX_EXEC_PROGRAM_POINT) {
      if (isRestartingForEachLoop(programPoint)) {
        // reached the max number of visit by program point, so take the false branch with current program state
        programPoint = new ProgramPoint(programPoint.block.falseBlock());
      } else {
        return;
      }
    }
    checkExplodedGraphTooBig(programState);
    ProgramState ps = programState.visitedPoint(programPoint, nbOfExecution + 1);
    ExplodedGraph.Node cachedNode = explodedGraph.node(programPoint, ps);
    if (!cachedNode.isNew() && exitPath == cachedNode.exitPath) {
      // has been enqueued earlier
      cachedNode.addParent(node, methodYield);
      return;
    }
    cachedNode.exitPath = exitPath;
    cachedNode.addParent(node, methodYield);
    workList.addFirst(cachedNode);
  }

  private static boolean isRestartingForEachLoop(ProgramPoint programPoint) {
    Tree terminator = programPoint.block.terminator();
    return terminator != null && terminator.is(Tree.Kind.FOR_EACH_STATEMENT);
  }

  private void checkExplodedGraphTooBig(ProgramState programState) {
    // Arbitrary formula to avoid out of memory errors
    if (steps + workList.size() > maxSteps() / 2 && programState.constraintsSize() > 75) {
      throw new ExplodedGraphTooBigException("Program state constraints are too big : stopping Symbolic Execution for method "
        + methodTree.simpleName().name() + " in class " + methodTree.symbol().owner().name());
    }
  }

  // VisibleForTesting
  protected int maxSteps() {
    return MAX_STEPS;
  }

  AlwaysTrueOrFalseExpressionCollector alwaysTrueOrFalseExpressionCollector() {
    return alwaysTrueOrFalseExpressionCollector;
  }

  /**
   * This class ensures that the SE checks are placed in the correct order for the ExplodedGraphWalker
   * In addition, checks that are needed for a correct ExplodedGraphWalker processing are provided in all cases.
   *
   */
  public static class ExplodedGraphWalkerFactory {

    // VisibleForTesting
    final List<SECheck> seChecks = new ArrayList<>();

    public ExplodedGraphWalkerFactory(List<SECheck> activeSEChecks) {
      List<SECheck> checks = new ArrayList<>(activeSEChecks);

      // This order of the mandatory SE checks is required by the ExplodedGraphWalker
      seChecks.add(removeOrDefault(checks, new NullDereferenceCheck()));
      seChecks.add(removeOrDefault(checks, new DivisionByZeroCheck()));
      seChecks.add(removeOrDefault(checks, new UnclosedResourcesCheck()));
      seChecks.add(removeOrDefault(checks, new LocksNotUnlockedCheck()));
      seChecks.add(removeOrDefault(checks, new NonNullSetToNullCheck()));
      seChecks.add(removeOrDefault(checks, new NoWayOutLoopCheck()));
      seChecks.add(removeOrDefault(checks, new OptionalGetBeforeIsPresentCheck()));
      seChecks.add(removeOrDefault(checks, new StreamConsumedCheck()));
      seChecks.add(removeOrDefault(checks, new RedundantAssignmentsCheck()));

      seChecks.addAll(checks);
    }

    public ExplodedGraphWalker createWalker(BehaviorCache behaviorCache, JavaFileScannerContext context) {
      return new ExplodedGraphWalker(seChecks, behaviorCache, context);
    }

    @SuppressWarnings("unchecked")
    private static <T extends SECheck> T removeOrDefault(List<SECheck> checks, T defaultInstance) {
      Iterator<SECheck> iterator = checks.iterator();
      while (iterator.hasNext()) {
        SECheck check = iterator.next();
        if (check.getClass().equals(defaultInstance.getClass())) {
          iterator.remove();
          return (T) check;
        }
      }
      return defaultInstance;
    }
  }

  @CheckForNull
  protected MethodBehavior peekMethodBehavior(Symbol.MethodSymbol symbol) {
    return behaviorCache.peek(symbol.signature());
  }
}
