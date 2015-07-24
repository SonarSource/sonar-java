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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.JavaTree;
import org.sonar.java.se.checkers.NullDereferenceChecker;
import org.sonar.java.se.checkers.SEChecker;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import java.io.PrintStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

public class ExplodedGraphWalker extends BaseTreeVisitor {

  /**
   * Arbitrary number to limit symbolic execution.
   */
  private static final int MAX_STEPS = 300;
  private ExplodedGraph explodedGraph;
  private Deque<ExplodedGraph.Node> workList;
  private final PrintStream out;
  private ExplodedGraph.Node node;
  public ExplodedGraph.ProgramPoint programPosition;
  public ProgramState programState;
  private CheckerDispatcher checkerDispatcher;
  @VisibleForTesting
  int steps;
  ConstraintManager constraintManager;

  public ExplodedGraphWalker(PrintStream out) {
    this.out = out;
    this.checkerDispatcher = new CheckerDispatcher(this, Lists.<SEChecker>newArrayList(new NullDereferenceChecker(out)));
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
    CFG cfg = CFG.build(tree);
    cfg.debugTo(out);
    explodedGraph = new ExplodedGraph();
    constraintManager = new ConstraintManager();
    workList = new LinkedList<>();
    out.println("Exploring Exploded Graph for method "+tree.simpleName().name()+" at line "+ ((JavaTree) tree).getLine());
    programState = ProgramState.EMPTY_STATE;
    for (VariableTree variableTree : tree.parameters()) {
      //create
      SymbolicValue sv = constraintManager.eval(programState, variableTree);
      programState = put(programState, variableTree.symbol(), sv);
      if(variableTree.symbol().metadata().isAnnotatedWith("javax.annotation.CheckForNull")) {
        //FIXME : introduce new state : maybe_null ??
        programState = setConstraint(programState, sv, SymbolicValue.NullSymbolicValue.NULL);
      }
    }
    enqueue(new ExplodedGraph.ProgramPoint(cfg.entry(), 0), programState);
    steps = 0;
    while (!workList.isEmpty()) {
      steps++;
      if (steps > MAX_STEPS) {
        throw new IllegalStateException("reached limit of " + MAX_STEPS + " steps");
      }
      // LIFO:
      node = workList.removeFirst();
      out.println(node);
      programPosition = node.programPoint;
      if (/* last */programPosition.block.successors.isEmpty()) {
        // not guaranteed that last block will be reached, e.g. "label: goto label;"
        // TODO(Godin): notify clients before continuing with another position
        continue;
      }
      programState = node.programState;

      if (programPosition.i < programPosition.block.elements().size()) {
        // process block element
        out.println("process block element " + programPosition.i);
        visit(programPosition.block.elements().get(programPosition.i));
      } else if (programPosition.block.terminator == null) {
        // process block exit, which is unconditional jump such as goto-statement or return-statement
        out.println("terminator is null");
        handleBlockExit(programPosition);
      } else if (programPosition.i == programPosition.block.elements().size()) {
        out.println("process block element " + programPosition.i);
        // process block exist, which is conditional jump such as if-statement
        checkerDispatcher.executeCheckPreStatement(programPosition.block.terminator);
      } else {
        // process branch
        out.println("process branch");
        handleBlockExit(programPosition);
      }
    }
    out.println();

    // Cleanup:
    explodedGraph = null;
    workList = null;
    node = null;
    programState = null;
    constraintManager = null;
  }

  private void handleBlockExit(ExplodedGraph.ProgramPoint programPosition) {
    CFG.Block block = programPosition.block;
    if (block.terminator != null) {
      switch (block.terminator.kind()) {
        case IF_STATEMENT:
          handleBranch(block, ((IfStatementTree) block.terminator).condition());
          return;
        case CONDITIONAL_OR:
        case CONDITIONAL_AND:
          handleBranch(block, ((BinaryExpressionTree) block.terminator).leftOperand());
          return;
        case CONDITIONAL_EXPRESSION:
          handleBranch(block, ((ConditionalExpressionTree) block.terminator).condition());
          return;
      }
    }
    // unconditional jumps, for-statement, switch-statement:
    for (CFG.Block successor : Lists.reverse(block.successors)) {
      enqueue(new ExplodedGraph.ProgramPoint(successor, 0), programState);
    }
  }

  private void handleBranch(CFG.Block programPosition, Tree condition) {
    Pair<ProgramState, ProgramState> pair = constraintManager.assumeDual(programState, condition);
    if (pair.a != null) {
      // enqueue false-branch, if feasible
      enqueue(new ExplodedGraph.ProgramPoint(programPosition.successors.get(1), 0), pair.a);
    }
    if (pair.b != null) {
      // enqueue true-branch, if feasible
      enqueue(new ExplodedGraph.ProgramPoint(programPosition.successors.get(0), 0), pair.b);
    }
  }

  private void visit(Tree tree) {
    out.println("visiting node "+tree.kind().name()+ " at line "+ ((JavaTree) tree).getLine());
    switch (tree.kind()) {
      case LABELED_STATEMENT:
      case SWITCH_STATEMENT:
      case EXPRESSION_STATEMENT:
      case PARENTHESIZED_EXPRESSION:
        throw new IllegalStateException("Cannot appear in CFG: " + tree.kind().name());
      case VARIABLE:
        VariableTree variableTree = (VariableTree) tree;
        if (variableTree.type().symbolType().isPrimitive()) {
          // TODO handle primitives
        } else {
          ExpressionTree initializer = variableTree.initializer();
          if (initializer == null) {
            programState = put(programState, variableTree.symbol(), SymbolicValue.NULL_LITERAL);
          } else {
            SymbolicValue val = constraintManager.eval(programState, initializer);
            programState = put(programState, variableTree.symbol(), val);
          }
        }
        break;
      case ASSIGNMENT:
        AssignmentExpressionTree assignmentExpressionTree = ((AssignmentExpressionTree) tree);
        //FIXME restricted to identifiers for now.
        if(assignmentExpressionTree.variable().is(Tree.Kind.IDENTIFIER)) {
          SymbolicValue value = getVal(assignmentExpressionTree.expression());
          programState = put(programState, ((IdentifierTree) assignmentExpressionTree.variable()).symbol(), value);
        }
        break;
      default:
    }
    checkerDispatcher.executeCheckPreStatement(tree);
  }

  static ProgramState put(ProgramState programState, Symbol symbol, SymbolicValue value) {
    SymbolicValue symbolicValue = programState.values.get(symbol);
    // update program state only for a different symbolic value
    if (symbolicValue == null || !symbolicValue.equals(value)) {
      Map<Symbol, SymbolicValue> temp = Maps.newHashMap(programState.values);
      temp.put(symbol, value);
      return new ProgramState(temp, programState.constraints);
    }
    return programState;
  }

  //FIXME should probably return null if constraint is not possible (sv is known to be null and we want to constrained it to null)
  static ProgramState setConstraint(ProgramState programState, SymbolicValue sv, SymbolicValue.NullSymbolicValue nullConstraint) {
    Object data = programState.constraints.get(sv);
    // update program state only for a different constraint
    if (data == null || !data.equals(nullConstraint)) {
      Map<SymbolicValue, Object> temp = Maps.newHashMap(programState.constraints);
      temp.put(sv, nullConstraint);
      return new ProgramState(programState.values, temp);
    }
    return programState;
  }

  public void enqueue(ExplodedGraph.ProgramPoint programPoint, ProgramState programState) {
    ExplodedGraph.Node node = explodedGraph.getNode(programPoint, programState);
    if (!node.isNew) {
      // has been enqueued earlier
      return;
    }
    workList.addFirst(node);
  }

  @CheckForNull
  public SymbolicValue getVal(Tree expression) {
    return constraintManager.eval(programState, expression);
  }
}
