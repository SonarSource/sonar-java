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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableSet;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

@Rule(
  key = "S864",
  name = "Limited dependence should be placed on operator precedence rules in expressions",
  tags = {"cert", "cwe", "misra"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class OperatorPrecedenceCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Set<Tree.Kind> LOGICAL_OPERATORS = ImmutableSet.of(
    Tree.Kind.CONDITIONAL_AND,
    Tree.Kind.CONDITIONAL_OR
    );

  private static final Set<Tree.Kind> RELATIONAL_OPERATORS = ImmutableSet.of(
    Tree.Kind.EQUAL_TO,
    Tree.Kind.NOT_EQUAL_TO,
    Tree.Kind.GREATER_THAN,
    Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
    Tree.Kind.LESS_THAN,
    Tree.Kind.LESS_THAN_OR_EQUAL_TO
    );

  private JavaFileScannerContext context;
  private Deque<Tree.Kind> stack = new LinkedList<>();
  private boolean inCondition;
  private boolean hasIssue;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    scan(tree.expression());
    stack.push(null);
    scan(tree.index());
    stack.pop();
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (stack.peek() != null) {
      raiseIssue(tree);
    }
    stack.push(Tree.Kind.ASSIGNMENT);
    super.visitAssignmentExpression(tree);
    stack.pop();
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    Tree.Kind kind = getKind(tree);
    Tree.Kind peek = stack.peek();
    if (peek != null && (inCondition || peek != Tree.Kind.ASSIGNMENT) && !isRelationalNestedInLogical(peek, kind) && peek != kind) {
      hasIssue = true;
    }
    stack.push(kind);
    super.visitBinaryExpression(tree);
    stack.pop();
  }

  private boolean isRelationalNestedInLogical(Tree.Kind base, Tree.Kind nested) {
    return LOGICAL_OPERATORS.contains(base) && RELATIONAL_OPERATORS.contains(nested);
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    stack.push(null);
    scan(tree.condition());
    stack.pop();
    stack.push(Tree.Kind.CONDITIONAL_EXPRESSION);
    scan(tree.trueExpression());
    scan(tree.falseExpression());
    stack.pop();
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    scan(tree.statement());
    visitCondition(tree.condition());
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    super.visitExpressionStatement(tree);
    if (hasIssue) {
      raiseIssue(tree.expression());
    }
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    scan(tree.initializer());
    visitCondition(tree.condition());
    scan(tree.update());
    scan(tree.statement());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    visitCondition(tree.condition());
    scan(tree.thenStatement());
    scan(tree.elseStatement());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    scan(tree.methodSelect());
    scan(tree.typeArguments());
    for (ExpressionTree argument : tree.arguments()) {
      stack.push(null);
      scan(argument);
      stack.pop();
    }
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    stack.push(null);
    super.visitNewArray(tree);
    stack.pop();
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    stack.push(null);
    super.visitNewClass(tree);
    stack.pop();
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    stack.push(null);
    super.visitParenthesized(tree);
    stack.pop();
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    super.visitReturnStatement(tree);
    if (hasIssue) {
      raiseIssue(tree.expression());
    }
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    visitCondition(tree.expression());
    scan(tree.cases());
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    super.visitThrowStatement(tree);
    if (hasIssue) {
      raiseIssue(tree.expression());
    }
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    if (tree.initializer() != null && hasIssue) {
      raiseIssue(tree.initializer());
    }
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    visitCondition(tree.condition());
    scan(tree.statement());
  }

  private void raiseIssue(Tree tree) {
    context.addIssue(tree, this, "Add parentheses to make the operator precedence explicit.");
    hasIssue = false;
  }

  private Tree.Kind getKind(Tree tree) {
    return ((JavaTree) tree).getKind();
  }

  private void visitCondition(@Nullable ExpressionTree condition) {
    if (condition != null) {
      inCondition = true;
      scan(condition);
      inCondition = false;
      if (hasIssue) {
        raiseIssue(condition);
      }
    }
  }

}
