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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.JavaTree;
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
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ExplodedGraphWalker extends BaseTreeVisitor {

  /**
   * Arbitrary number to limit symbolic execution.
   */
  private static final int MAX_STEPS = 2000;
  private static final Logger LOG = LoggerFactory.getLogger(ExplodedGraphWalker.class);
  private static final Set<String> THIS_SUPER = ImmutableSet.of("this", "super");

  private final ConditionAlwaysTrueOrFalseCheck alwaysTrueOrFalseCheck;
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
    alwaysTrueOrFalseCheck = new ConditionAlwaysTrueOrFalseCheck();
    this.checkerDispatcher = new CheckerDispatcher(this, context, Lists.<SECheck>newArrayList(alwaysTrueOrFalseCheck, new NullDereferenceCheck()));
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
    explodedGraph = new ExplodedGraph();
    constraintManager = new ConstraintManager();
    workList = new LinkedList<>();
    LOG.debug("Exploring Exploded Graph for method " + tree.simpleName().name() + " at line " + ((JavaTree) tree).getLine());
    programState = ProgramState.EMPTY_STATE;
    Iterable<ProgramState> startingStates = Lists.newArrayList(programState);
    for (final VariableTree variableTree : tree.parameters()) {
      // create
      final SymbolicValue sv = constraintManager.eval(programState, variableTree);
      startingStates = Iterables.transform(startingStates, new Function<ProgramState, ProgramState>() {
        @Override
        public ProgramState apply(ProgramState input) {
          return put(input, variableTree.symbol(), sv);
        }
      });

      if (variableTree.symbol().metadata().isAnnotatedWith("javax.annotation.CheckForNull")) {
        startingStates = Iterables.concat(Iterables.transform(startingStates, new Function<ProgramState, List<ProgramState>>() {
          @Override
          public List<ProgramState> apply(ProgramState input) {
            return Lists.newArrayList(
              ConstraintManager.setConstraint(input, sv, ConstraintManager.NullConstraint.NULL),
              ConstraintManager.setConstraint(input, sv, ConstraintManager.NullConstraint.NOT_NULL));
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
        checkerDispatcher.executeCheckPreStatement(programPosition.block.terminator());
      } else {
        // process branch
        handleBlockExit(programPosition);
      }
    }

    checkerDispatcher.executeCheckEndOfExecution();
    // Cleanup:
    explodedGraph = null;
    workList = null;
    node = null;
    programState = null;
    constraintManager = null;
  }

  private void handleBlockExit(ExplodedGraph.ProgramPoint programPosition) {
    CFG.Block block = programPosition.block;
    Tree terminator = block.terminator();
    if (terminator != null) {
      switch (terminator.kind()) {
        case IF_STATEMENT:
          handleBranch(block, ((IfStatementTree) terminator).condition());
          return;
        case CONDITIONAL_OR:
        case CONDITIONAL_AND:
          handleBranch(block, ((BinaryExpressionTree) terminator).leftOperand());
          return;
        case CONDITIONAL_EXPRESSION:
          handleBranch(block, ((ConditionalExpressionTree) terminator).condition());
          return;
        case FOR_STATEMENT:
          ForStatementTree forStatement = (ForStatementTree) terminator;
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
    Pair<ProgramState, ProgramState> pair = constraintManager.assumeDual(programState, condition);
    if (pair.a != null) {
      // enqueue false-branch, if feasible
      enqueue(new ExplodedGraph.ProgramPoint(programPosition.falseBlock(), 0), pair.a);
      if (checkPath) {
        alwaysTrueOrFalseCheck.evaluatedToFalse(condition);
      }
    } else {
      SymbolicValue val = getVal(condition);
      if (val != null) {
        programState = ConstraintManager.setConstraint(programState, val, ConstraintManager.BooleanConstraint.TRUE);
      }
    }
    if (pair.b != null) {
      // enqueue true-branch, if feasible
      enqueue(new ExplodedGraph.ProgramPoint(programPosition.trueBlock(), 0), pair.b);
      if (checkPath) {
        alwaysTrueOrFalseCheck.evaluatedToTrue(condition);
      }
    } else {
      SymbolicValue val = getVal(condition);
      if (val != null) {
        programState = ConstraintManager.setConstraint(programState, val, ConstraintManager.BooleanConstraint.FALSE);
      }
    }

  }

  private void visit(Tree tree, Tree terminator) {
    LOG.debug("visiting node " + tree.kind().name() + " at line " + ((JavaTree) tree).getLine());
    switch (tree.kind()) {
      case LABELED_STATEMENT:
      case SWITCH_STATEMENT:
      case EXPRESSION_STATEMENT:
      case PARENTHESIZED_EXPRESSION:
        throw new IllegalStateException("Cannot appear in CFG: " + tree.kind().name());
      case VARIABLE:
        VariableTree variableTree = (VariableTree) tree;
        ExpressionTree initializer = variableTree.initializer();
        if (initializer == null) {
          if (terminator != null && terminator.is(Tree.Kind.FOR_EACH_STATEMENT)) {
            setSymbolicValueForEachValue(variableTree);
          } else if (variableTree.type().symbolType().is("boolean")) {
            setSymbolicValueFalseValue(variableTree);
          } else if (!variableTree.type().symbolType().isPrimitive()) {
            setSymbolicValueNullValue(variableTree);
          }
        } else {
          setSymbolicValueValueFromInitializer(variableTree, initializer);
        }
        break;
      case ASSIGNMENT:
        AssignmentExpressionTree assignmentExpressionTree = ((AssignmentExpressionTree) tree);
        // FIXME restricted to identifiers for now.
        if (assignmentExpressionTree.variable().is(Tree.Kind.IDENTIFIER)) {
          SymbolicValue value = getVal(assignmentExpressionTree.expression());
          programState = put(programState, ((IdentifierTree) assignmentExpressionTree.variable()).symbol(), value);
        }
        break;
      case METHOD_INVOCATION:
        setSymbolicValueOnFields((MethodInvocationTree) tree);
        break;
      default:
    }
    checkerDispatcher.executeCheckPreStatement(tree);
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
    for (Entry<Symbol, SymbolicValue> entry : values.entrySet()) {
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
      programState = new ProgramState(values, programState.constraints);
    }
  }

  private static boolean isField(Symbol symbol) {
    return !symbol.owner().isMethodSymbol();
  }

  private void setSymbolicValueNullValue(VariableTree variableTree) {
    programState = put(programState, variableTree.symbol(), SymbolicValue.NULL_LITERAL);
  }

  private void setSymbolicValueForEachValue(VariableTree variableTree) {
    SymbolicValue val = constraintManager.createSymbolicValue(variableTree);
    programState = put(programState, variableTree.symbol(), val);
  }

  private void setSymbolicValueFalseValue(VariableTree variableTree) {
    programState = put(programState, variableTree.symbol(), SymbolicValue.FALSE_LITERAL);
  }

  private void setSymbolicValueValueFromInitializer(VariableTree variableTree, ExpressionTree initializer) {
    SymbolicValue val = constraintManager.eval(programState, initializer);
    programState = put(programState, variableTree.symbol(), val);
  }

  private static ProgramState put(ProgramState programState, Symbol symbol, SymbolicValue value) {
    SymbolicValue symbolicValue = programState.values.get(symbol);
    // update program state only for a different symbolic value
    if (symbolicValue == null || !symbolicValue.equals(value)) {
      Map<Symbol, SymbolicValue> temp = Maps.newHashMap(programState.values);
      temp.put(symbol, value);
      return new ProgramState(temp, programState.constraints);
    }
    return programState;
  }

  public void enqueue(ExplodedGraph.ProgramPoint programPoint, ProgramState programState) {
    ExplodedGraph.Node currentNode = explodedGraph.getNode(programPoint, programState);
    if (!currentNode.isNew) {
      // has been enqueued earlier
      return;
    }
    workList.addFirst(currentNode);
  }

  @CheckForNull
  public SymbolicValue getVal(Tree expression) {
    SymbolicValue value = constraintManager.eval(programState, expression);
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) expression;
      Symbol symbol = identifierTree.symbol();
      if (symbol.isVariableSymbol()) {
        programState = put(programState, symbol, value);
      }
    }
    return value;
  }
}
