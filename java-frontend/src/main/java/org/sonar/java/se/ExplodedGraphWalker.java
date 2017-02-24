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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.LiveVariables;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Types;
import org.sonar.java.se.checks.ConditionAlwaysTrueOrFalseCheck;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.checks.LocksNotUnlockedCheck;
import org.sonar.java.se.checks.NoWayOutLoopCheck;
import org.sonar.java.se.checks.NonNullSetToNullCheck;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.OptionalGetBeforeIsPresentCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.checks.UnclosedResourcesCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
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
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import javax.annotation.Nullable;

import java.util.ArrayList;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExplodedGraphWalker {

  private static final String EQUALS_METHOD_NAME = "equals";
  /**
   * Arbitrary number to limit symbolic execution.
   */
  private static final int MAX_STEPS = 16_000;
  public static final int MAX_NESTED_BOOLEAN_STATES = 10_000;
  private static final Logger LOG = Loggers.get(ExplodedGraphWalker.class);
  private static final Set<String> THIS_SUPER = ImmutableSet.of("this", "super");

  private static final boolean DEBUG_MODE_ACTIVATED = false;
  @VisibleForTesting
  static final int MAX_EXEC_PROGRAM_POINT = 2;
  private static final MethodMatcher SYSTEM_EXIT_MATCHER = MethodMatcher.create().typeDefinition("java.lang.System").name("exit").addParameter("int");
  private static final MethodMatcher OBJECT_WAIT_MATCHER = MethodMatcher.create().typeDefinition("java.lang.Object").name("wait").withAnyParameters();
  private static final MethodMatcher THREAD_SLEEP_MATCHER = MethodMatcher.create().typeDefinition("java.lang.Thread").name("sleep").withAnyParameters();
  private final ConditionAlwaysTrueOrFalseCheck alwaysTrueOrFalseChecker;
  private MethodTree methodTree;

  private ExplodedGraph explodedGraph;

  @VisibleForTesting
  Deque<ExplodedGraph.Node> workList;
  ExplodedGraph.Node node;
  ProgramPoint programPosition;
  ProgramState programState;
  private LiveVariables liveVariables;
  @VisibleForTesting
  CheckerDispatcher checkerDispatcher;
  private CFG.Block exitBlock;

  private final SemanticModel semanticModel;
  private final BehaviorCache behaviorCache;
  @VisibleForTesting
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
    public MaximumStepsReachedException(String s, TooManyNestedBooleanStatesException e) {
      super(s, e);
    }

  }
  public static class TooManyNestedBooleanStatesException extends RuntimeException {

  }
  @VisibleForTesting
  public ExplodedGraphWalker(BehaviorCache behaviorCache, SemanticModel semanticModel) {
    alwaysTrueOrFalseChecker = new ConditionAlwaysTrueOrFalseCheck();
    List<SECheck> checks = Lists.newArrayList(alwaysTrueOrFalseChecker, new NullDereferenceCheck(), new DivisionByZeroCheck(),
      new UnclosedResourcesCheck(), new LocksNotUnlockedCheck(), new NonNullSetToNullCheck(), new NoWayOutLoopCheck());
    this.checkerDispatcher = new CheckerDispatcher(this, checks);
    this.behaviorCache = behaviorCache;
    this.semanticModel = semanticModel;
  }

  @VisibleForTesting
  ExplodedGraphWalker(BehaviorCache behaviorCache, SemanticModel semanticModel, boolean cleanup) {
    this(behaviorCache, semanticModel);
    this.cleanup = cleanup;
  }

  private ExplodedGraphWalker(ConditionAlwaysTrueOrFalseCheck alwaysTrueOrFalseChecker, List<SECheck> seChecks, BehaviorCache behaviorCache, SemanticModel semanticModel) {
    this.alwaysTrueOrFalseChecker = alwaysTrueOrFalseChecker;
    this.checkerDispatcher = new CheckerDispatcher(this, seChecks);
    this.behaviorCache = behaviorCache;
    this.semanticModel = semanticModel;
  }

  public ExplodedGraph getExplodedGraph() {
    return explodedGraph;
  }

  public MethodBehavior visitMethod(MethodTree tree) {
    return visitMethod(tree, null);
  }

  public MethodBehavior visitMethod(MethodTree tree, @Nullable MethodBehavior methodBehavior) {
    BlockTree body = tree.block();
    this.methodBehavior = methodBehavior;
    if (body != null) {
      execute(tree);
    }
    return this.methodBehavior;
  }

  private void execute(MethodTree tree) {
    CFG cfg = CFG.build(tree);
    exitBlock = cfg.exitBlock();
    checkerDispatcher.init(tree, cfg);
    liveVariables = LiveVariables.analyze(cfg);
    explodedGraph = new ExplodedGraph();
    methodTree = tree;
    constraintManager = new ConstraintManager();
    workList = new LinkedList<>();
    // Linked hashSet is required to guarantee order of yields to be generated
    endOfExecutionPath = new LinkedHashSet<>();
    if(DEBUG_MODE_ACTIVATED) {
      LOG.debug("Exploring Exploded Graph for method " + tree.simpleName().name() + " at line " + ((JavaTree) tree).getLine());
    }
    programState = ProgramState.EMPTY_STATE;
    steps = 0;
    for (ProgramState startingState : startingStates(tree, programState)) {
      enqueue(new ProgramPoint(cfg.entry()), startingState);
    }
    while (!workList.isEmpty()) {
      steps++;
      if (steps > MAX_STEPS) {
        throwMaxSteps(tree);
      }
      // LIFO:
      setNode(workList.removeFirst());
      if (programPosition.block.successors().isEmpty()) {
        endOfExecutionPath.add(node);
        continue;
      }
      try {
        if (programPosition.i < programPosition.block.elements().size()) {
          // process block element
          visit(programPosition.block.elements().get(programPosition.i), programPosition.block.terminator());
        } else if (programPosition.block.terminator() == null) {
          // process block exit, which is unconditional jump such as goto-statement or return-statement
          handleBlockExit(programPosition);
        } else if (programPosition.i == programPosition.block.elements().size()) {
          // process block exist, which is conditional jump such as if-statement
          checkerDispatcher.executeCheckPostStatement(programPosition.block.terminator());
        } else {
          // process branch
          // process block exist, which is conditional jump such as if-statement
          checkerDispatcher.executeCheckPreStatement(programPosition.block.terminator());
          handleBlockExit(programPosition);
        }
      } catch (TooManyNestedBooleanStatesException e) {
        throwTooManyBooleanStates(tree, e);
      }
    }

    handleEndOfExecutionPath(false);
    checkerDispatcher.executeCheckEndOfExecution();
    // Cleanup:
    workList = null;
    node = null;
    programState = null;
    constraintManager = null;
  }

  private void throwTooManyBooleanStates(MethodTree tree, TooManyNestedBooleanStatesException e) {
    interrupted();
    String message = String.format("reached maximum number of %d branched states for method %s in class %s",
      MAX_NESTED_BOOLEAN_STATES, tree.simpleName().name(), tree.symbol().owner().name());
    throw new MaximumStepsReachedException(message, e);
  }

  private void throwMaxSteps(MethodTree tree) {
    interrupted();
    String message = String.format("reached limit of %d steps for method %s#%d in class %s",
      MAX_STEPS, tree.simpleName().name(), tree.simpleName().firstToken().line(), tree.symbol().owner().name());
    throw new MaximumStepsReachedException(message);
  }

  private void interrupted() {
    handleEndOfExecutionPath(true);
    checkerDispatcher.interruptedExecution();
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
      if (!programState.exitingOnRuntimeException()) {
        checkerDispatcher.executeCheckEndOfExecutionPath(constraintManager);
      }
      if (!interrupted && methodBehavior != null) {
        methodBehavior.createYield(node);
      }
    });
    setNode(savedNode);
  }

  public void addExceptionalYield(SymbolicValue target, ProgramState exceptionalState, String exceptionFullyQualifiedName, SECheck check) {
    if (methodBehavior != null && methodBehavior.parameters().contains(target)) {
      Type exceptionType = semanticModel.getClassType(exceptionFullyQualifiedName);
      ProgramState newExceptionalState = exceptionalState.clearStack().stackValue(constraintManager.createExceptionalSymbolicValue(exceptionType));
      ExplodedGraph.Node exitNode = explodedGraph.node(node.programPoint, newExceptionalState);
      methodBehavior.createExceptionalCheckBasedYield(target, exitNode, exceptionType, check);
      exitNode.addParent(node, null);
    }
  }

  private Iterable<ProgramState> startingStates(MethodTree tree, ProgramState currentState) {
    Stream<ProgramState> stateStream = Stream.of(currentState);
    boolean isEqualsMethod = EQUALS_METHOD_NAME.equals(tree.simpleName().name()) && tree.parameters().size() == 1;
    SymbolMetadata packageMetadata = ((JavaSymbol.MethodJavaSymbol) tree.symbol()).packge().metadata();
    boolean nonNullParams = packageMetadata.isAnnotatedWith("javax.annotation.ParametersAreNonnullByDefault");
    boolean nullableParams = packageMetadata.isAnnotatedWith("javax.annotation.ParametersAreNullableByDefault");
    boolean hasMethodBehavior = methodBehavior != null;
    for (final VariableTree variableTree : tree.parameters()) {
      // create
      final SymbolicValue sv = constraintManager.createSymbolicValue(variableTree);
      Symbol variableSymbol = variableTree.symbol();
      if (hasMethodBehavior) {
        methodBehavior.addParameter(sv);
      }
      stateStream = stateStream.map(ps -> ps.put(variableSymbol, sv));
      if (isEqualsMethod || parameterCanBeNull(variableSymbol, nullableParams)) {
        stateStream = stateStream.flatMap((ProgramState ps) ->
          Stream.concat(
            sv.setConstraint(ps, ObjectConstraint.NULL).stream(),
            sv.setConstraint(ps, ObjectConstraint.NOT_NULL).stream()
            ));
      } else if(nonNullParams) {
        stateStream = stateStream.flatMap(ps -> sv.setConstraint(ps, ObjectConstraint.NOT_NULL).stream());
      }
    }
    return stateStream.collect(Collectors.toList());
  }

  private static boolean parameterCanBeNull(Symbol variableSymbol, boolean nullableParams) {
    SymbolMetadata metadata = variableSymbol.metadata();
    return metadata.isAnnotatedWith("javax.annotation.CheckForNull")
      || metadata.isAnnotatedWith("javax.annotation.Nullable")
      || (nullableParams && !variableSymbol.type().isPrimitive());
  }

  private void cleanUpProgramState(CFG.Block block) {
    if (cleanup) {
      Collection<SymbolicValue> protectedSVs = methodBehavior == null ? Collections.emptyList() : methodBehavior.parameters();
      programState = programState.cleanupDeadSymbols(liveVariables.getOut(block), protectedSVs);
      programState = programState.cleanupConstraints(protectedSVs);
    }
  }

  private void handleBlockExit(ProgramPoint programPosition) {
    CFG.Block block = programPosition.block;
    Tree terminator = block.terminator();
    cleanUpProgramState(block);
    boolean exitPath = node.exitPath;
    if (terminator != null) {
      switch (terminator.kind()) {
        case IF_STATEMENT:
          handleBranch(block, cleanupCondition(((IfStatementTree) terminator).condition()));
          return;
        case CONDITIONAL_OR:
        case CONDITIONAL_AND:
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
          handleBranch(block, cleanupCondition(whileCondition), !whileCondition.is(Tree.Kind.BOOLEAN_LITERAL));
          return;
        case DO_STATEMENT:
          ExpressionTree doCondition = ((DoWhileStatementTree) terminator).condition();
          handleBranch(block, cleanupCondition(doCondition), !doCondition.is(Tree.Kind.BOOLEAN_LITERAL));
          return;
        case SYNCHRONIZED_STATEMENT:
          resetFieldValues();
          break;
        case RETURN_STATEMENT:
          ExpressionTree returnExpression = ((ReturnStatementTree) terminator).expression();
          if (returnExpression != null) {
            programState.storeExitValue();
          }
          break;
        case THROW_STATEMENT:
          ProgramState.Pop unstack = programState.unstackValue(1);
          // we don't use the SV related to the expression
          programState = unstack.state.stackValue(constraintManager.createExceptionalSymbolicValue(((ThrowStatementTree) terminator).expression().symbolType()));
          programState.storeExitValue();
          break;
        default:
          // do nothing by default.
      }
    }
    // unconditional jumps, for-statement, switch-statement, synchronized:
    if (exitPath) {
      if (block.exitBlock() != null) {
        enqueue(new ProgramPoint(block.exitBlock()), programState, true);
      } else {
        for (CFG.Block successor : block.successors()) {
          enqueue(new ProgramPoint(successor), programState, true);
        }
      }

    } else {
      for (CFG.Block successor : block.successors()) {
        if (!block.isFinallyBlock() || isDirectFlowSuccessorOf(successor, block)) {
          node.happyPath = terminator == null || !terminator.is(Tree.Kind.THROW_STATEMENT);
          enqueue(new ProgramPoint(successor), programState, successor == block.exitBlock());
        }
      }
    }
  }

  private static boolean isDirectFlowSuccessorOf(CFG.Block successor, CFG.Block block) {
    return successor != block.exitBlock() || successor.isMethodExitBlock();
  }

  /**
   * Required for accurate reporting.
   * If condition is && or || expression, then return its right operand.
   */
  private static ExpressionTree cleanupCondition(ExpressionTree condition) {
    ExpressionTree cleanedUpCondition = ExpressionUtils.skipParentheses(condition);
    if (cleanedUpCondition.is(Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR)) {
      cleanedUpCondition = cleanupCondition(((BinaryExpressionTree) cleanedUpCondition).rightOperand());
    }
    return cleanedUpCondition;
  }

  private void handleBranch(CFG.Block programPosition, Tree condition) {
    handleBranch(programPosition, condition, true);
  }

  private void handleBranch(CFG.Block programPosition, Tree condition, boolean checkPath) {
    Pair<List<ProgramState>, List<ProgramState>> pair = constraintManager.assumeDual(programState);
    ProgramPoint falseBlockProgramPoint = new ProgramPoint(programPosition.falseBlock());
    for (ProgramState state : pair.a) {
      ProgramState ps = state;
      if (condition.parent().is(Tree.Kind.CONDITIONAL_AND) && !isPartOfConditionalExpressionCondition(condition)) {
        // push a FALSE value on the top of the stack to enforce the choice of the branch,
        // as non-reachable symbolic values won't get a TRUE/FALSE constraint when assuming dual
        ps = state.stackValue(SymbolicValue.FALSE_LITERAL);
      }
      // enqueue false-branch, if feasible
      enqueue(falseBlockProgramPoint, ps, node.exitPath);
      if (checkPath) {
        alwaysTrueOrFalseChecker.evaluatedToFalse(condition, node);
      }
    }
    ProgramPoint trueBlockProgramPoint = new ProgramPoint(programPosition.trueBlock());
    for (ProgramState state : pair.b) {
      ProgramState ps = state;
      if (condition.parent().is(Tree.Kind.CONDITIONAL_OR) && !isPartOfConditionalExpressionCondition(condition)) {
        // push a TRUE value on the top of the stack to enforce the choice of the branch,
        // as non-reachable symbolic values won't get a TRUE/FALSE constraint when assuming dual
        ps = state.stackValue(SymbolicValue.TRUE_LITERAL);
      }
      // enqueue true-branch, if feasible
      enqueue(trueBlockProgramPoint, ps, node.exitPath);
      if (checkPath) {
        alwaysTrueOrFalseChecker.evaluatedToTrue(condition, node);
      }
    }
  }

  private static boolean isPartOfConditionalExpressionCondition(Tree tree) {
    Tree current;
    Tree parent = tree;
    do {
      current = parent;
      parent = parent.parent();
    } while (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION, Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR));
    return parent.is(Tree.Kind.CONDITIONAL_EXPRESSION) && current.equals(((ConditionalExpressionTree) parent).condition());
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
      case LABELED_STATEMENT:
      case SWITCH_STATEMENT:
      case EXPRESSION_STATEMENT:
      case PARENTHESIZED_EXPRESSION:
        throw new IllegalStateException("Cannot appear in CFG: " + tree.kind().name());
      case VARIABLE:
        executeVariable((VariableTree) tree, terminator);
        break;
      case TYPE_CAST:
        executeTypeCast((TypeCastTree) tree);
        break;
      case ASSIGNMENT:
      case MULTIPLY_ASSIGNMENT:
      case DIVIDE_ASSIGNMENT:
      case REMAINDER_ASSIGNMENT:
      case PLUS_ASSIGNMENT:
      case MINUS_ASSIGNMENT:
      case LEFT_SHIFT_ASSIGNMENT:
      case RIGHT_SHIFT_ASSIGNMENT:
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        executeAssignement((AssignmentExpressionTree) tree);
        break;
      case AND_ASSIGNMENT:
      case XOR_ASSIGNMENT:
      case OR_ASSIGNMENT:
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
      case MULTIPLY:
      case DIVIDE:
      case REMAINDER:
      case PLUS:
      case MINUS:
      case LEFT_SHIFT:
      case RIGHT_SHIFT:
      case UNSIGNED_RIGHT_SHIFT:
      case AND:
      case XOR:
      case OR:
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL_TO:
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL_TO:
      case EQUAL_TO:
      case NOT_EQUAL_TO:
        executeBinaryExpression(tree);
        break;
      case POSTFIX_INCREMENT:
      case POSTFIX_DECREMENT:
      case PREFIX_INCREMENT:
      case PREFIX_DECREMENT:
      case UNARY_MINUS:
      case UNARY_PLUS:
      case BITWISE_COMPLEMENT:
      case LOGICAL_COMPLEMENT:
      case INSTANCE_OF:
        executeUnaryExpression(tree);
        break;
      case IDENTIFIER:
        executeIdentifier((IdentifierTree) tree);
        break;
      case MEMBER_SELECT:
        executeMemberSelect((MemberSelectExpressionTree) tree);
        break;
      case INT_LITERAL:
      case LONG_LITERAL:
      case FLOAT_LITERAL:
      case DOUBLE_LITERAL:
      case CHAR_LITERAL:
      case STRING_LITERAL:
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
      case LAMBDA_EXPRESSION:
      case METHOD_REFERENCE:
        programState = programState.stackValue(constraintManager.createSymbolicValue(tree));
        break;
      case ASSERT_STATEMENT:
        executeAssertStatement(tree);
        return;
      default:
    }

    checkerDispatcher.executeCheckPostStatement(tree);
    clearStack(tree);
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
    logState(mit);

    programState = unstack.state;

    // get method behavior for method with known declaration (ie: within the same file)
    MethodBehavior methodInvokedBehavior = null;
    Symbol methodSymbol = mit.symbol();
    if(methodSymbol.isMethodSymbol()) {
      methodInvokedBehavior = behaviorCache.get((Symbol.MethodSymbol) methodSymbol);
    }

    // Enqueue additional exceptional paths corresponding to unchecked exceptions, for instance OutOfMemoryError
    enqueueUncheckedExceptionalPaths();

    final SymbolicValue resultValue = constraintManager.createMethodSymbolicValue(mit, unstack.values);
    if (methodInvokedBehavior != null && methodInvokedBehavior.isComplete()) {
      List<SymbolicValue> invocationArguments = invocationArguments(unstack.values);
      List<Type> invocationTypes = mit.arguments().stream().map(ExpressionTree::symbolType).collect(Collectors.toList());

      Map<Type, SymbolicValue.ExceptionalSymbolicValue> thrownExceptionsByExceptionType = new HashMap<>();

      // Enqueue exceptional paths from exceptional yields
      methodInvokedBehavior.exceptionalPathYields()
        .forEach(yield -> yield.statesAfterInvocation(
          invocationArguments,
          invocationTypes,
          programState,
          () -> thrownExceptionsByExceptionType.computeIfAbsent(yield.exceptionType(), constraintManager::createExceptionalSymbolicValue))
          .forEach(psYield -> enqueueExceptionalPaths(psYield, yield)));

      // Enqueue happy paths
      methodInvokedBehavior.happyPathYields()
        .forEach(yield ->
          yield.statesAfterInvocation(invocationArguments, invocationTypes, programState, () -> resultValue)
            .map(psYield -> handleSpecialMethods(psYield, mit))
            .forEach(psYield -> enqueueHappyPath(psYield, mit,  yield)));
    } else {
      // Enqueue exceptional paths from thrown exceptions
      enqueueThrownExceptionalPaths(methodSymbol);

      // Enqueue happy paths
      programState = handleSpecialMethods(programState.stackValue(resultValue), mit);
      checkerDispatcher.executeCheckPostStatement(mit);
      clearStack(mit);
    }
  }

  private void enqueueHappyPath(ProgramState programState, MethodInvocationTree mit, MethodYield yield) {
    checkerDispatcher.syntaxNode = mit;
    checkerDispatcher.methodYield = yield;
    checkerDispatcher.addTransition(programState);
    checkerDispatcher.methodYield = null;
    clearStack(mit);
  }

  private ProgramState handleSpecialMethods(ProgramState ps, MethodInvocationTree mit) {
    if (isNonNullMethod(mit.symbol())) {
      return ps.addConstraint(ps.peekValue(), ObjectConstraint.NOT_NULL);
    } else if (OBJECT_WAIT_MATCHER.matches(mit)) {
      return ps.resetFieldValues(constraintManager);
    }
    return ps;
  }

  private void enqueueThrownExceptionalPaths(Symbol symbol) {
    if (!symbol.isMethodSymbol()) {
      // do nothing for unknown methods
      return;
    }
    ProgramState ps = programState.clearStack();
    ((Symbol.MethodSymbol) symbol).thrownTypes().stream()
      .map(constraintManager::createExceptionalSymbolicValue)
      .map(ps::stackValue)
      .forEach(this::enqueueExceptionalPaths);
  }

  private void enqueueUncheckedExceptionalPaths() {
    enqueueExceptionalPaths(programState.clearStack().stackValue(constraintManager.createExceptionalSymbolicValue(null)));
  }

  private void enqueueExceptionalPaths(ProgramState ps) {
    enqueueExceptionalPaths(ps, null);
  }

  private void enqueueExceptionalPaths(ProgramState ps, @Nullable MethodYield methodYield) {
    Set<CFG.Block> exceptionBlocks = node.programPoint.block.exceptions();
    List<CFG.Block> catchBlocks = exceptionBlocks.stream().filter(CFG.Block.IS_CATCH_BLOCK).collect(Collectors.toList());
    SymbolicValue peekValue = ps.peekValue();

    Preconditions.checkState(peekValue instanceof SymbolicValue.ExceptionalSymbolicValue, "Top of stack should always contains exceptional SV");
    SymbolicValue.ExceptionalSymbolicValue exceptionSV = (SymbolicValue.ExceptionalSymbolicValue) peekValue;
    // only consider the first match, as order of catch block is important
    Optional<CFG.Block> firstMatchingCatchBlock = catchBlocks.stream()
      .filter(b -> isCaughtByBlock(exceptionSV.exceptionType(), b))
      .sorted((b1, b2) -> Integer.compare(b2.id(), b1.id()))
      .findFirst();
    if (firstMatchingCatchBlock.isPresent()) {
      enqueue(new ProgramPoint(firstMatchingCatchBlock.get()), ps, methodYield);
      return;
    }

    // branch to any unchecked exception catch
    catchBlocks.stream()
      .filter(ExplodedGraphWalker::isCatchingUncheckedException)
      .forEach(b -> enqueue(new ProgramPoint(b), ps, methodYield));

    // store the exception as exit value in case of method exit in next block
    ps.storeExitValue();

    // use other exceptional blocks, i.e. finally block and exit blocks
    List<CFG.Block> otherBlocks = exceptionBlocks.stream()
      .filter(CFG.Block.IS_CATCH_BLOCK.negate())
      .collect(Collectors.toList());
    if (otherBlocks.isEmpty()) {
      // explicitly add the exception branching to method exit
      CFG.Block methodExit = node.programPoint.block.successors()
        .stream()
        .filter(CFG.Block::isMethodExitBlock)
        .findFirst()
        .orElse(exitBlock);
      enqueue(new ProgramPoint(methodExit), ps, true, methodYield);
    } else {
      otherBlocks.forEach(b -> enqueue(new ProgramPoint(b), ps, true, methodYield));
    }
  }

  private static boolean isCaughtByBlock(@Nullable Type thrownType, CFG.Block catchBlock) {
    if (thrownType != null) {
      Type caughtType = ((VariableTree) catchBlock.elements().get(0)).symbol().type();
      return thrownType.isSubtypeOf(caughtType);
    }
    return false;
  }

  private static boolean isCatchingUncheckedException(CFG.Block catchBlock) {
    Type caughtType = ((VariableTree) catchBlock.elements().get(0)).symbol().type();
    return caughtType.isSubtypeOf("java.lang.RuntimeException")
      || caughtType.isSubtypeOf("java.lang.Error")
      || caughtType.is("java.lang.Exception")
      || caughtType.is("java.lang.Throwable");
  }

  private static List<SymbolicValue> invocationArguments(List<SymbolicValue> values) {
    return Lists.reverse(values.subList(0, values.size() - 1));
  }

  private static boolean isNonNullMethod(Symbol symbol) {
    return !symbol.isUnknown() && symbol.metadata().isAnnotatedWith("javax.annotation.Nonnull");
  }

  /**
   * @see JLS8 4.12.5 for details
   */
  private void executeVariable(VariableTree variableTree, @Nullable Tree terminator) {
    if (variableTree.initializer() == null) {
      SymbolicValue sv = null;
      if (terminator != null && terminator.is(Tree.Kind.FOR_EACH_STATEMENT)) {
        sv = constraintManager.createSymbolicValue(variableTree);
      } else if (variableTree.parent().is(Tree.Kind.CATCH)) {
        sv = getCaughtException(variableTree.symbol().type());
        programState = programState.addConstraint(sv, ObjectConstraint.NOT_NULL);
      }
      if (sv != null) {
        programState = programState.put(variableTree.symbol(), sv);
      }
    } else {
      ProgramState.Pop unstack = programState.unstackValue(1);
      programState = unstack.state;
      programState = programState.put(variableTree.symbol(), unstack.values.get(0));
    }
  }

  private SymbolicValue getCaughtException(Type caughtType) {
    SymbolicValue sv = null;
    Type exceptionType = null;
    // FIXME SONARJAVA-2069 every path conducting to a catch block should have an exceptional symbolic value on top of the stack
    if (programState.peekValue() instanceof SymbolicValue.ExceptionalSymbolicValue) {
      ProgramState.Pop unstack = programState.unstackValue(1);
      programState = unstack.state;
      // use the Exceptional SV from the stack
      sv = unstack.values.get(0);
      exceptionType = ((SymbolicValue.ExceptionalSymbolicValue) sv).exceptionType();
    }
    if (exceptionType == null || exceptionType.isUnknown()) {
      // unknown exception, create an exception of the adequate type
      sv = constraintManager.createExceptionalSymbolicValue(caughtType);
    }
    return sv;
  }

  private void executeTypeCast(TypeCastTree typeCast) {
    Type type = typeCast.type().symbolType();
    if (type.isPrimitive()) {
      JavaType expType = (JavaType) typeCast.expression().symbolType();
      // create SV to consume factory if any
      SymbolicValue castSV = constraintManager.createSymbolicValue(typeCast);
      // if exp type is a primitive and subtype of cast type, we can reuse the same symbolic value
      if (!expType.isPrimitive() || !new Types().isSubtype(expType, (JavaType) type)) {
        ProgramState.Pop unstack = programState.unstackValue(1);
        programState = unstack.state;
        programState = programState.stackValue(castSV);
      }
    }
  }

  private void executeAssignement(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    ProgramState.Pop unstack;
    SymbolicValue value;

    if (ExpressionUtils.isSimpleAssignment(tree)) {
      unstack = programState.unstackValue(1);
      value = unstack.values.get(0);
    } else {
      unstack = programState.unstackValue(2);
      value = constraintManager.createSymbolicValue(tree);
    }

    programState = unstack.state;
    programState = programState.stackValue(value);
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      // only local variables or fields are added to table of values
      // FIXME SONARJAVA-1776 fields accessing using "this." should be handled
      programState = programState.put(((IdentifierTree) variable).symbol(), value);
    }
  }

  private void executeLogicalAssignment(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      ProgramState.Pop unstack = programState.unstackValue(2);
      SymbolicValue assignedTo = unstack.values.get(1);
      SymbolicValue value = unstack.values.get(0);
      programState = unstack.state;
      SymbolicValue symbolicValue = constraintManager.createSymbolicValue(tree);
      symbolicValue.computedFrom(ImmutableList.of(assignedTo, value));
      programState = programState.stackValue(symbolicValue);
      programState = programState.put(((IdentifierTree) variable).symbol(), symbolicValue);
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

  private void executeNewClass(NewClassTree tree) {
    NewClassTree newClassTree = tree;
    programState = programState.unstackValue(newClassTree.arguments().size()).state;
    // Enqueue exceptional paths
    node.programPoint.block.exceptions().forEach(b -> enqueue(new ProgramPoint(b), programState, !b.isCatchBlock()));
    SymbolicValue svNewClass = constraintManager.createSymbolicValue(newClassTree);
    programState = programState.stackValue(svNewClass);
    programState = svNewClass.setSingleConstraint(programState, ObjectConstraint.NOT_NULL);
  }

  private void executeBinaryExpression(Tree tree) {
    // Consume two and produce one SV.
    ProgramState.Pop unstackBinary = programState.unstackValue(2);
    programState = unstackBinary.state;
    SymbolicValue symbolicValue = constraintManager.createSymbolicValue(tree);
    symbolicValue.computedFrom(unstackBinary.values);
    if(tree.is(Tree.Kind.PLUS)) {
      BinaryExpressionTree bt = (BinaryExpressionTree) tree;
      if (bt.leftOperand().symbolType().is("java.lang.String")) {
        ObjectConstraint leftConstraint = programState.getConstraint(unstackBinary.values.get(1), ObjectConstraint.class);
        if (leftConstraint != null && !leftConstraint.isNull()) {
          List<ProgramState> programStates = symbolicValue.setConstraint(programState, ObjectConstraint.NOT_NULL);
          Preconditions.checkState(programStates.size() == 1);
          programState = programStates.get(0);
        }

      } else if(bt.rightOperand().symbolType().is("java.lang.String")) {
        ObjectConstraint rightConstraint = programState.getConstraint(unstackBinary.values.get(0), ObjectConstraint.class);
        if (rightConstraint != null && !rightConstraint.isNull()) {
          List<ProgramState> programStates = symbolicValue.setConstraint(programState, ObjectConstraint.NOT_NULL);
          Preconditions.checkState(programStates.size() == 1);
          programState = programStates.get(0);
        }

      }
    }
    programState = programState.stackValue(symbolicValue);
  }

  private void executeUnaryExpression(Tree tree) {
    // consume one and produce one
    ProgramState.Pop unstackUnary = programState.unstackValue(1);
    programState = unstackUnary.state;
    SymbolicValue unarySymbolicValue = constraintManager.createSymbolicValue(tree);
    unarySymbolicValue.computedFrom(unstackUnary.values);
    if (tree.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT)) {
      programState = programState.stackValue(unstackUnary.values.get(0));
    } else {
      programState = programState.stackValue(unarySymbolicValue);
    }
    if (tree.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.PREFIX_INCREMENT)
      && ((UnaryExpressionTree) tree).expression().is(Tree.Kind.IDENTIFIER)) {
      programState = programState.put(((IdentifierTree) ((UnaryExpressionTree) tree).expression()).symbol(), unarySymbolicValue);
    }
  }

  private void executeIdentifier(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    SymbolicValue value = programState.getValue(symbol);
    if (value == null) {
      value = constraintManager.createSymbolicValue(tree);
      programState = programState.stackValue(value);
      learnIdentifierNullConstraints(tree, value);
    } else {
      programState = programState.stackValue(value);
    }
    programState = programState.put(symbol, value);
  }

  private void learnIdentifierNullConstraints(IdentifierTree tree, SymbolicValue sv) {
    if (THIS_SUPER.contains(tree.name())) {
      programState = programState.addConstraint(sv, ObjectConstraint.NOT_NULL);
      return;
    }
    Tree declaration = tree.symbol().declaration();
    if (!isFinalField(tree.symbol()) || declaration == null) {
      return;
    }
    ExpressionTree initializer = ((VariableTree) declaration).initializer();
    if (initializer == null) {
      return;
    }
    // only check final field with an initializer
    if (initializer.is(Tree.Kind.NULL_LITERAL)) {
      programState = programState.addConstraint(sv, ObjectConstraint.NULL);
    } else if (initializer.is(Tree.Kind.NEW_CLASS) || initializer.is(Tree.Kind.NEW_ARRAY)) {
      programState = programState.addConstraint(sv, ObjectConstraint.NOT_NULL);
    }
  }

  private static boolean isFinalField(Symbol symbol) {
    return symbol.isVariableSymbol()
      && symbol.isFinal()
      && symbol.owner().isTypeSymbol();
  }

  private void executeMemberSelect(MemberSelectExpressionTree mse) {
    if (!"class".equals(mse.identifier().name())) {

      ProgramState.Pop unstackMSE = programState.unstackValue(1);
      programState = unstackMSE.state;
    }
    SymbolicValue mseValue = constraintManager.createSymbolicValue(mse);
    programState = programState.stackValue(mseValue);
  }

  public void clearStack(Tree tree) {
    if (tree.parent().is(Tree.Kind.EXPRESSION_STATEMENT)) {
      programState = programState.clearStack();
    }
  }

  private void setSymbolicValueOnFields(MethodInvocationTree tree) {
    if (isLocalMethodInvocation(tree) || THREAD_SLEEP_MATCHER.matches(tree)) {
      resetFieldValues();
    }
  }

  private static boolean isLocalMethodInvocation(MethodInvocationTree tree) {
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

  private void resetFieldValues() {
    programState = programState.resetFieldValues(constraintManager);
  }

  private void logState(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER) && "printState".equals(((IdentifierTree) mit.methodSelect()).name())) {
      debugPrint(((JavaTree) mit).getLine(), node);
    }
  }

  private static void debugPrint(Object... toPrint) {
    if (DEBUG_MODE_ACTIVATED) {
      LOG.error(Joiner.on(" - ").join(toPrint));
    }
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
        debugPrint(programPoint);
        return;
      }
    }
    checkExplodedGraphTooBig(programState);
    ProgramState ps = programState.visitedPoint(programPoint, nbOfExecution + 1);
    ps.lastEvaluated = programState.getLastEvaluated();
    ExplodedGraph.Node cachedNode = explodedGraph.node(programPoint, ps);
    if (!cachedNode.isNew && exitPath == cachedNode.exitPath) {
      // has been enqueued earlier
      cachedNode.addParent(node, methodYield);
      return;
    }
    cachedNode.exitPath = exitPath;
    if(node != null) {
      cachedNode.happyPath = node.happyPath;
    }
    cachedNode.addParent(node, methodYield);
    workList.addFirst(cachedNode);
  }

  private static boolean isRestartingForEachLoop(ProgramPoint programPoint) {
    Tree terminator = programPoint.block.terminator();
    return terminator != null && terminator.is(Tree.Kind.FOR_EACH_STATEMENT);
  }

  private void checkExplodedGraphTooBig(ProgramState programState) {
    // Arbitrary formula to avoid out of memory errors
    if (steps + workList.size() > MAX_STEPS / 2 && programState.constraintsSize() > 75) {
      throw new ExplodedGraphTooBigException("Program state constraints are too big : stopping Symbolic Execution for method "
        + methodTree.simpleName().name() + " in class " + methodTree.symbol().owner().name());
    }
  }

  /**
   * This class ensures that the SE checks are placed in the correct order for the ExplodedGraphWalker
   * In addition, checks that are needed for a correct ExplodedGraphWalker processing are provided in all cases.
   *
   */
  public static class ExplodedGraphWalkerFactory {

    private final ConditionAlwaysTrueOrFalseCheck alwaysTrueOrFalseChecker;
    @VisibleForTesting
    final List<SECheck> seChecks = new ArrayList<>();

    public ExplodedGraphWalkerFactory(List<JavaFileScanner> scanners) {
      List<SECheck> checks = new ArrayList<>();
      for (JavaFileScanner scanner : scanners) {
        if (scanner instanceof SECheck) {
          checks.add((SECheck) scanner);
        }
      }
      alwaysTrueOrFalseChecker = removeOrDefault(checks, new ConditionAlwaysTrueOrFalseCheck());
      // This order of the mandatory SE checks is required by the ExplodedGraphWalker
      seChecks.add(alwaysTrueOrFalseChecker);
      seChecks.add(removeOrDefault(checks, new NullDereferenceCheck()));
      seChecks.add(removeOrDefault(checks, new DivisionByZeroCheck()));
      seChecks.add(removeOrDefault(checks, new UnclosedResourcesCheck()));
      seChecks.add(removeOrDefault(checks, new LocksNotUnlockedCheck()));
      seChecks.add(removeOrDefault(checks, new NonNullSetToNullCheck()));
      seChecks.add(removeOrDefault(checks, new NoWayOutLoopCheck()));
      seChecks.add(removeOrDefault(checks, new OptionalGetBeforeIsPresentCheck()));
      seChecks.addAll(checks);
    }

    public ExplodedGraphWalker createWalker(BehaviorCache behaviorCache, SemanticModel semanticModel) {
      return new ExplodedGraphWalker(alwaysTrueOrFalseChecker, seChecks, behaviorCache, semanticModel);
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
}
