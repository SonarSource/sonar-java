/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.symexecengine;

import com.google.common.base.Preconditions;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.Set;

public abstract class DataFlowVisitor extends BaseTreeVisitor {

  public DataFlowVisitor() {
    executionState = new ExecutionState();
  }

  protected ExecutionState executionState;

  protected abstract boolean isSymbolRelevant(Symbol symbol);

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    if (isSymbolRelevant(tree.symbol())) {
      executionState.defineSymbol(tree.symbol());
      executionState.createValueForSymbol(tree.symbol(), tree);
    }
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    Symbol symbol = getSymbol(tree.variable());
    if(symbol != null && isSymbolRelevant(symbol)) {
      executionState.createValueForSymbol(symbol, tree.expression());
    }
  }

  @CheckForNull
  protected Symbol getSymbol(ExpressionTree variable) {
    if (!variable.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {
      return null;
    }
    IdentifierTree identifier;
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) variable;
    } else {
      identifier = ((MemberSelectExpressionTree) variable).identifier();
    }
    return identifier.symbol();
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    // do nothing, inner methods will be visited later
  }

  @Override
  public void visitClass(ClassTree tree) {
    // do nothing, inner methods will be visited later
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    ExecutionState blockES = new ExecutionState(executionState);
    executionState = blockES;
    scan(tree.block());
    scan(tree.resources());
    handleResources(tree.resources());
    for (CatchTree catchTree : tree.catches()) {
      executionState = new ExecutionState(blockES.parent);
      scan(catchTree.block());
      blockES.merge(executionState);
    }

    if (tree.finallyBlock() != null) {
      executionState = new ExecutionState(blockES.parent);
      scan(tree.finallyBlock());
      executionState.reportIssues();
      executionState = blockES.parent.overrideBy(blockES.overrideBy(executionState));
    } else {
      executionState = blockES.restoreParent();
    }
  }

  /**
   * Allow some treatment on resources by implementors.
   * @param resources
   */
  protected void handleResources(List<VariableTree> resources) {
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    scan(tree.condition());
    ExecutionState thenES = new ExecutionState(executionState);
    executionState = thenES;
    scan(tree.thenStatement());

    if (tree.elseStatement() == null) {
      executionState = thenES.restoreParent();
    } else {
      ExecutionState elseES = new ExecutionState(thenES.parent);
      executionState = elseES;
      scan(tree.elseStatement());
      elseES.reportIssues();
      executionState = thenES.parent.overrideBy(thenES.merge(elseES));
    }
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    scan(tree.expression());
    ExecutionState resultingES = new ExecutionState(executionState);
    executionState = new ExecutionState(executionState);
    for (CaseGroupTree caseGroupTree : tree.cases()) {
      for (StatementTree statement : caseGroupTree.body()) {
        if (isBreakOrReturnStatement(statement)) {
          resultingES = executionState.merge(resultingES);
          executionState = new ExecutionState(resultingES.parent);
        } else {
          scan(statement);
        }
      }
    }
    if (!lastStatementIsBreakOrReturn(tree)) {
      // merge the last execution state
      resultingES = executionState.merge(resultingES);
    }

    if (switchContainsDefaultLabel(tree)) {
      // the default block guarantees that we will cover all the paths
      executionState = resultingES.parent.overrideBy(resultingES);
    } else {
      executionState = resultingES.parent.merge(resultingES);
    }
  }

  private boolean isBreakOrReturnStatement(StatementTree statement) {
    return statement.is(Tree.Kind.BREAK_STATEMENT, Tree.Kind.RETURN_STATEMENT);
  }

  private boolean switchContainsDefaultLabel(SwitchStatementTree tree) {
    for (CaseGroupTree caseGroupTree : tree.cases()) {
      for (CaseLabelTree label : caseGroupTree.labels()) {
        if ("default".equals(label.caseOrDefaultKeyword().text())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean lastStatementIsBreakOrReturn(SwitchStatementTree tree) {
    List<CaseGroupTree> cases = tree.cases();
    if (!cases.isEmpty()) {
      List<StatementTree> lastStatements = cases.get(cases.size() - 1).body();
      return !lastStatements.isEmpty() && isBreakOrReturnStatement(lastStatements.get(lastStatements.size() - 1));
    }
    return false;
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    scan(tree.condition());
    visitLoopStatement(tree.statement());
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    visitLoopStatement(tree.statement());
    scan(tree.condition());
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    scan(tree.initializer());
    scan(tree.condition());
    scan(tree.update());
    visitLoopStatement(tree.statement());
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    scan(tree.variable());
    scan(tree.expression());
    visitLoopStatement(tree.statement());
  }

  private void visitLoopStatement(StatementTree tree) {
    executionState = new ExecutionState(executionState);
    //Scan twice the tree in loop to create multiple value if required
    scan(tree);
    scan(tree);
    executionState = executionState.restoreParent();
  }

  public Set<Tree> getIssueTrees() {
    Preconditions.checkState(executionState.parent == null);
    return executionState.getIssueTrees();
  }

}
