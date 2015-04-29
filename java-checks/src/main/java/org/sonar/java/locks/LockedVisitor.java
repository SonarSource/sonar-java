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
package org.sonar.java.locks;

import org.sonar.java.checks.SubscriptionBaseVisitor;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.TypeCriteria;
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
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import javax.annotation.CheckForNull;

import java.util.List;

public class LockedVisitor extends BaseTreeVisitor {

  private ExecutionState executionState;

  private static final String JAVA_LOCK = "java.util.concurrent.locks.Lock";

  private static final MethodInvocationMatcherCollection LOCK_INVOCATIONS = lockMethodInvocationMatcher();
  private static final MethodInvocationMatcher UNLOCK_INVOCATION = MethodInvocationMatcher.create().typeDefinition(JAVA_LOCK).name("unlock");

  public LockedVisitor(SubscriptionBaseVisitor check) {
    executionState = new ExecutionState(check);
  }

  private static MethodInvocationMatcherCollection lockMethodInvocationMatcher() {
    return MethodInvocationMatcherCollection.create(
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("lock"),
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("lockInterruptibly"),
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("tryLock")
        .withNoParameterConstraint());
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    if (variable.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT)) {

      IdentifierTree identifier;
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        identifier = (IdentifierTree) variable;
      } else {
        identifier = ((MemberSelectExpressionTree) variable).identifier();
      }
      Symbol symbol = identifier.symbol();
      if (symbol.type().isSubtypeOf(JAVA_LOCK)) {
        executionState.newValueForSymbol(symbol, tree);
      }
    }
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    // do nothing, inner methods will be visited later
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (LOCK_INVOCATIONS.anyMatch(tree)) {
      Symbol symbol = extractInvokedOnSymbol(tree.methodSelect());
      if (symbol != null) {
        executionState.addLockable(symbol, tree);
      }
    } else if (UNLOCK_INVOCATION.matches(tree)) {
      ExpressionTree methodSelect = tree.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
        if (expression.is(Tree.Kind.IDENTIFIER)) {
          executionState.markAsUnlocked(((IdentifierTree) expression).symbol());
        }
      }
    }
  }

  @CheckForNull
  private Symbol extractInvokedOnSymbol(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      return extractSymbol(((MemberSelectExpressionTree) expressionTree).expression());
    } else if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expressionTree).symbol().owner();
    }
    return null;
  }

  @CheckForNull
  private Symbol extractSymbol(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) expressionTree).identifier().symbol();
    } else if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expressionTree).symbol();
    }
    return null;
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

    for (CatchTree catchTree : tree.catches()) {
      executionState = new ExecutionState(blockES.parent);
      scan(catchTree.block());
      blockES.merge(executionState);
    }

    if (tree.finallyBlock() != null) {
      executionState = new ExecutionState(blockES.parent);
      scan(tree.finallyBlock());
      executionState = blockES.parent.overrideBy(blockES.overrideBy(executionState));
    } else {
      executionState = blockES.parent.merge(blockES);
    }
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    scan(tree.condition());
    ExecutionState thenES = new ExecutionState(executionState);
    executionState = thenES;
    scan(tree.thenStatement());

    if (tree.elseStatement() == null) {
      executionState = thenES.parent.merge(thenES);
    } else {
      ExecutionState elseES = new ExecutionState(thenES.parent);
      executionState = elseES;
      scan(tree.elseStatement());
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
    visitStatement(tree.statement());
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    visitStatement(tree.statement());
    scan(tree.condition());
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    scan(tree.condition());
    scan(tree.initializer());
    scan(tree.update());
    visitStatement(tree.statement());
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    scan(tree.variable());
    scan(tree.expression());
    visitStatement(tree.statement());
  }

  private void visitStatement(StatementTree tree) {
    executionState = new ExecutionState(executionState);
    scan(tree);
    executionState = executionState.restoreParent();
  }

  public void insertIssues() {
    executionState.insertIssues();
  }
}
