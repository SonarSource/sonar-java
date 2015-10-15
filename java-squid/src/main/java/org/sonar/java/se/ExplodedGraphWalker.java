/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.se;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.JavaTree;
import org.sonar.java.se.ConstraintManager.NullConstraint;
import org.sonar.java.se.checks.ConditionAlwaysTrueOrFalseCheck;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExplodedGraphWalker extends BaseTreeVisitor {

  /**
   * Arbitrary number to limit symbolic execution.
   */
  private static final int MAX_STEPS = 2000;
  private static final Logger LOG = LoggerFactory.getLogger(ExplodedGraphWalker.class);
  private static final Set<String> THIS_SUPER = ImmutableSet.of("this", "super");

  private static final boolean DEBUG_MODE_ACTIVATED = false;
  private static final int MAX_EXEC_PROGRAM_POINT = 2;
  private final ConditionAlwaysTrueOrFalseCheck alwaysTrueOrFalseChecker;
  private ExplodedGraph explodedGraph;
  private Deque<ExplodedGraph.Node> workList;
  private ExplodedGraph.Node node;
  public ExplodedGraph.ProgramPoint programPosition;
  public ProgramState programState;

  private CheckerDispatcher checkerDispatcher;

  @VisibleForTesting
  int steps;
  ConstraintManager constraintManager;

  public static class MaximumStepsReachedException extends RuntimeException {
    public MaximumStepsReachedException(String s) {
      super(s);
    }
  }

  public ExplodedGraphWalker(JavaFileScannerContext context) throws MaximumStepsReachedException {
    alwaysTrueOrFalseChecker = new ConditionAlwaysTrueOrFalseCheck();
    this.checkerDispatcher = new CheckerDispatcher(this, context, Lists.<SECheck>newArrayList(alwaysTrueOrFalseChecker, new NullDereferenceCheck()));
  }

  @Override
  public void visitMethod(MethodTree tree) {
    super.visitMethod(tree);
    BlockTree body = tree.block();
    if (body != null) {
      execute(tree);
    }
  }

  private void execute(MethodTree tree) {
    checkerDispatcher.init();
    CFG cfg = CFG.build(tree);
    System.out.println("Exploring Exploded Graph for method " + tree.simpleName().name() + " at line " + ((JavaTree) tree).getLine());
    explodedGraph = new ExplodedGraph();
    constraintManager = new ConstraintManager();
    workList = new LinkedList<>();
    LOG.debug("Exploring Exploded Graph for method " + tree.simpleName().name() + " at line " + ((JavaTree) tree).getLine());
    programState = ProgramState.EMPTY_STATE;
    Iterable<ProgramState> startingStates = Lists.newArrayList(programState);
    for (final VariableTree variableTree : tree.parameters()) {
      // create
      final SymbolicValue sv = constraintManager.createSymbolicValue(variableTree);
      startingStates = Iterables.transform(startingStates, new Function<ProgramState, ProgramState>() {
        @Override
        public ProgramState apply(ProgramState input) {
          return ProgramState.put(input, variableTree.symbol(), sv);
        }
      });

      if (variableTree.symbol().metadata().isAnnotatedWith("javax.annotation.CheckForNull")) {
        startingStates = Iterables.concat(Iterables.transform(startingStates, new Function<ProgramState, List<ProgramState>>() {
          @Override
          public List<ProgramState> apply(ProgramState input) {
            return Lists.newArrayList(
              sv.setConstraint(input, NullConstraint.NULL),
              sv.setConstraint(input, NullConstraint.NOT_NULL));
          }
        }));

      }
    }
    for (ProgramState startingState : startingStates) {
      enqueue(new ExplodedGraph.ProgramPoint(cfg.entry(), 0), startingState);
    }
    steps = 0;
    while (!workList.isEmpty()) {
      steps++;
      if (steps > MAX_STEPS) {
        throw new MaximumStepsReachedException("reached limit of " + MAX_STEPS + " steps for method " + tree.simpleName().name() + "in class " + tree.symbol().owner().name());
      }
      // LIFO:
      node = workList.removeFirst();
      programPosition = node.programPoint;
      if (/* last */programPosition.block.successors().isEmpty()) {
        // not guaranteed that last block will be reached, e.g. "label: goto label;"
        // TODO(Godin): notify clients before continuing with another position
        continue;
      }
      programState = node.programState;
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
        handleBlockExit(programPosition);
      }
    }

    checkerDispatcher.executeCheckEndOfExecution(tree);
    // Cleanup:
    explodedGraph = null;
    workList = null;
    node = null;
    programState = null;
    constraintManager = null;
  }

  private void handleBlockExit(ExplodedGraph.ProgramPoint programPosition) {
    CFG.Block block = programPosition.block;
    if (block.terminator() != null) {
      switch (block.terminator().kind()) {
        case IF_STATEMENT:
          handleBranch(block, ((IfStatementTree) block.terminator()).condition());
          return;
        case CONDITIONAL_OR:
        case CONDITIONAL_AND:
          handleBranch(block, ((BinaryExpressionTree) block.terminator()).leftOperand());
          return;
        case CONDITIONAL_EXPRESSION:
          handleBranch(block, ((ConditionalExpressionTree) block.terminator()).condition());
          return;
        case FOR_STATEMENT:
          ForStatementTree forStatement = (ForStatementTree) block.terminator();
          if (forStatement.condition() != null) {
            handleBranch(block, forStatement.condition(), false);
            return;
          }
      }
    }
    // unconditional jumps, for-statement, switch-statement:
    for (CFG.Block successor : block.successors()) {
      enqueue(new ExplodedGraph.ProgramPoint(successor, 0), programState);
    }
  }

  private void handleBranch(CFG.Block programPosition, Tree condition) {
    handleBranch(programPosition, condition, true);
  }

  private void handleBranch(CFG.Block programPosition, Tree condition, boolean checkPath) {
    Pair<ProgramState, ProgramState> pair = constraintManager.assumeDual(programState);
    if (pair.a != null) {
      // enqueue false-branch, if feasible
      ProgramState ps = ProgramState.stackValue(pair.a, SymbolicValue.FALSE_LITERAL);
      enqueue(new ExplodedGraph.ProgramPoint(programPosition.falseBlock(), 0), ps);
      if (checkPath) {
        alwaysTrueOrFalseChecker.evaluatedToFalse(condition);
      }
    }
    if (pair.b != null) {
      ProgramState ps = ProgramState.stackValue(pair.b, SymbolicValue.TRUE_LITERAL);
      // enqueue true-branch, if feasible
      enqueue(new ExplodedGraph.ProgramPoint(programPosition.trueBlock(), 0), ps);
      if (checkPath) {
        alwaysTrueOrFalseChecker.evaluatedToTrue(condition);
      }
    }

  }

  private void visit(Tree tree, Tree terminator) {
    LOG.debug("visiting node " + tree.kind().name() + " at line " + ((JavaTree) tree).getLine());
    if (!checkerDispatcher.executeCheckPreStatement(tree)) {
      // Some of the check pre statement sink the execution on this node.
      return;
    }
    switch (tree.kind()) {
      case METHOD_INVOCATION:
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        setSymbolicValueOnFields(mit);
        programState = ProgramState.unstack(programState, mit.arguments().size()).a;
        logState(mit);
        programState = ProgramState.stackValue(programState, constraintManager.createSymbolicValue(mit));
        break;
      case LABELED_STATEMENT:
      case SWITCH_STATEMENT:
      case EXPRESSION_STATEMENT:
      case PARENTHESIZED_EXPRESSION:
        throw new IllegalStateException("Cannot appear in CFG: " + tree.kind().name());
      case VARIABLE:
        VariableTree variableTree = (VariableTree) tree;
        ExpressionTree initializer = variableTree.initializer();
        if (initializer == null) {
          SymbolicValue sv = null;
          if (terminator != null && terminator.is(Tree.Kind.FOR_EACH_STATEMENT)) {
            sv = constraintManager.createSymbolicValue(variableTree);
          } else if (variableTree.type().symbolType().is("boolean")) {
            sv = SymbolicValue.FALSE_LITERAL;
          } else if (!variableTree.type().symbolType().isPrimitive()) {
            sv = SymbolicValue.NULL_LITERAL;
          }
          if (sv != null) {
            programState = ProgramState.put(programState, variableTree.symbol(), sv);
          }
        } else {
          Pair<ProgramState, List<SymbolicValue>> unstack = ProgramState.unstack(programState, 1);
          programState = unstack.a;
          programState = ProgramState.put(programState, variableTree.symbol(), unstack.b.get(0));
        }
        break;
      case ASSIGNMENT:
        AssignmentExpressionTree assignmentExpressionTree = ((AssignmentExpressionTree) tree);
        // FIXME restricted to identifiers for now.
        if (assignmentExpressionTree.variable().is(Tree.Kind.IDENTIFIER)) {
          Pair<ProgramState, List<SymbolicValue>> unstack = ProgramState.unstack(programState, 2);
          SymbolicValue value = unstack.b.get(1);
          programState = unstack.a;
          programState = ProgramState.put(programState, ((IdentifierTree) assignmentExpressionTree.variable()).symbol(), value);
          programState = ProgramState.stackValue(programState, value);
        }
        break;
      case NEW_CLASS:
        NewClassTree newClassTree = (NewClassTree) tree;
        programState = ProgramState.unstack(programState, newClassTree.arguments().size()).a;
        SymbolicValue svNewClass = constraintManager.createSymbolicValue(newClassTree);
        programState = ProgramState.stackValue(programState, svNewClass);
        programState = svNewClass.setConstraint(programState, NullConstraint.NOT_NULL);
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
        // Consume two and produce one SV.
        Pair<ProgramState, List<SymbolicValue>> unstackBinary = ProgramState.unstack(programState, 2);
        programState = unstackBinary.a;
        SymbolicValue symbolicValue = constraintManager.createSymbolicValue(tree);
        symbolicValue.computedFrom(unstackBinary.b);
        programState = ProgramState.stackValue(programState, symbolicValue);
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
        // consume one and produce one
        Pair<ProgramState, List<SymbolicValue>> unstackUnary = ProgramState.unstack(programState, 1);
        programState = unstackUnary.a;
        SymbolicValue unarySymbolicValue = constraintManager.createSymbolicValue(tree);
        unarySymbolicValue.computedFrom(unstackUnary.b);
        programState = ProgramState.stackValue(programState, unarySymbolicValue);
        break;
      case IDENTIFIER:
        Symbol symbol = ((IdentifierTree) tree).symbol();
        SymbolicValue value = programState.values.get(symbol);
        if (value == null) {
          value = constraintManager.createSymbolicValue(tree);
          programState = ProgramState.put(programState, symbol, value);
        }
        programState = ProgramState.stackValue(programState, value);
        break;
      case MEMBER_SELECT:
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
        if (!"class".equals(mse.identifier().name())) {
          Pair<ProgramState, List<SymbolicValue>> unstackMSE = ProgramState.unstack(programState, 1);
          programState = unstackMSE.a;
        }
        SymbolicValue MSEValue = constraintManager.createSymbolicValue(tree);
        programState = ProgramState.stackValue(programState, MSEValue);
        break;
      case INT_LITERAL:
      case LONG_LITERAL:
      case FLOAT_LITERAL:
      case DOUBLE_LITERAL:
      case BOOLEAN_LITERAL:
      case CHAR_LITERAL:
      case STRING_LITERAL:
      case NULL_LITERAL:
        SymbolicValue val = constraintManager.evalLiteral((LiteralTree) tree);
        programState = ProgramState.stackValue(programState, val);
        break;
      case LAMBDA_EXPRESSION:
        programState = ProgramState.stackValue(programState, constraintManager.createSymbolicValue(tree));
        break;
      default:
    }

    checkerDispatcher.executeCheckPostStatement(tree);
    if (tree.parent().is(Tree.Kind.EXPRESSION_STATEMENT)) {
      programState = ProgramState.unstack(programState, programState.stack.size()).a;
    }
  }

  private void setSymbolicValueOnFields(MethodInvocationTree tree) {
    if (isLocalMethodInvocation(tree)) {
      resetNullValuesOnFields(tree);
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

  private void resetNullValuesOnFields(MethodInvocationTree tree) {
    boolean changed = false;
    Map<Symbol, SymbolicValue> values = Maps.newHashMap(programState.values);
    for (Map.Entry<Symbol, SymbolicValue> entry : values.entrySet()) {
      if (constraintManager.isNull(programState, entry.getValue())) {
        Symbol symbol = entry.getKey();
        if (isField(symbol)) {
          VariableTree variable = ((Symbol.VariableSymbol) symbol).declaration();
          if (variable != null) {
            changed = true;
            SymbolicValue nonNullValue = constraintManager.supersedeSymbolicValue(variable);
            values.put(symbol, nonNullValue);
          }
        }
      }
    }
    if (changed) {
      programState = new ProgramState(values, programState.constraints, programState.visitedPoints, programState.stack);
    }
  }

  private static boolean isField(Symbol symbol) {
    return !symbol.owner().isMethodSymbol();
  }

  private void logState(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER) && "printState".equals(((IdentifierTree) mit.methodSelect()).name())) {
      debugPrint(((JavaTree) mit).getLine(), node);
    }
  }

  private void debugPrint(Object... toPrint) {
    if (DEBUG_MODE_ACTIVATED) {
      LOG.error(Joiner.on(" - ").join(toPrint));
    }
  }

  public void enqueue(ExplodedGraph.ProgramPoint programPoint, ProgramState programState) {
    int nbOfExecution = programState.numberOfTimeVisited(programPoint);
    if (nbOfExecution > MAX_EXEC_PROGRAM_POINT) {
      debugPrint(programState);
      return;
    }
    Multiset<ExplodedGraph.ProgramPoint> visitedPoints = HashMultiset.create(programState.visitedPoints);
    visitedPoints.add(programPoint);
    ExplodedGraph.Node node = explodedGraph.getNode(programPoint, new ProgramState(programState.values, programState.constraints, visitedPoints, programState.stack));
    if (!node.isNew) {
      // has been enqueued earlier
      return;
    }
    workList.addFirst(node);
  }

}
