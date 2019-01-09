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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.Deque;
import java.util.LinkedList;

@Rule(key = "S1643")
public class StringConcatenationInLoopCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private Deque<Tree> loopLevel = new LinkedList<>();
  private SemanticModel semanticModel;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    loopLevel.clear();
    semanticModel = (SemanticModel) context.getSemanticModel();
    scan(context.getTree());
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (!loopLevel.isEmpty() && isStringConcatenation(tree) && isNotLoopLocalVar(tree) && isNotArrayAccess(tree)) {
      context.reportIssue(this, tree.variable(), "Use a StringBuilder instead.");
    }
    super.visitAssignmentExpression(tree);
  }

  private boolean isNotLoopLocalVar(AssignmentExpressionTree tree) {
    IdentifierTree idTree = getIdentifierTree(tree.variable());
    Tree envTree = semanticModel.getTree(semanticModel.getEnv(idTree.symbol()));
    Tree loopTree = loopLevel.peek();
    return envTree == null || !(envTree.equals(loopTree) || envTree.equals(loopStatement(loopTree)));
  }

  private static boolean isNotArrayAccess(AssignmentExpressionTree tree) {
    return !tree.variable().is(Tree.Kind.ARRAY_ACCESS_EXPRESSION);
  }

  private static IdentifierTree getIdentifierTree(ExpressionTree tree) {
    IdentifierTree idTree;
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      idTree = getIdentifierTree(((MemberSelectExpressionTree) tree).expression());
    } else if (tree.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
      idTree = getIdentifierTree(((ArrayAccessExpressionTree) tree).expression());
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      idTree = getIdentifierTree(((MethodInvocationTree) tree).methodSelect());
    } else {
      idTree = (IdentifierTree) tree;
    }
    return idTree;
  }

  private static Tree loopStatement(Tree loopTree) {
    if (loopTree.is(Tree.Kind.FOR_STATEMENT)) {
      return ((ForStatementTree) loopTree).statement();
    } else if (loopTree.is(Tree.Kind.DO_STATEMENT)) {
      return ((DoWhileStatementTree) loopTree).statement();
    } else if (loopTree.is(Tree.Kind.WHILE_STATEMENT)) {
      return ((WhileStatementTree) loopTree).statement();
    } else if (loopTree.is(Tree.Kind.FOR_EACH_STATEMENT)) {
      return ((ForEachStatement) loopTree).statement();
    }
    return null;
  }

  private static boolean isStringConcatenation(AssignmentExpressionTree tree) {
    return tree.symbolType().is("java.lang.String") && isConcatenation(tree);
  }

  private static boolean isConcatenation(AssignmentExpressionTree tree) {
    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      ExpressionTree expressionTree = ExpressionUtils.skipParentheses(tree.expression());
      return expressionTree.is(Tree.Kind.PLUS) && concatenateVariable(tree.variable(), (BinaryExpressionTree) expressionTree);
    }
    return tree.is(Tree.Kind.PLUS_ASSIGNMENT);
  }

  private static boolean concatenateVariable(ExpressionTree variable, BinaryExpressionTree plus) {
    return concatenateVariable(variable, plus.leftOperand()) || concatenateVariable(variable, plus.rightOperand());
  }

  private static boolean concatenateVariable(ExpressionTree variable, ExpressionTree operand) {
    if (operand.is(Tree.Kind.PLUS)) {
      return concatenateVariable(variable, (BinaryExpressionTree) operand);
    }
    return SyntacticEquivalence.areEquivalent(variable, operand);
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    loopLevel.push(tree);
    super.visitForEachStatement(tree);
    loopLevel.pop();
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    loopLevel.push(tree);
    super.visitForStatement(tree);
    loopLevel.pop();
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    loopLevel.push(tree);
    super.visitWhileStatement(tree);
    loopLevel.pop();
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    loopLevel.push(tree);
    super.visitDoWhileStatement(tree);
    loopLevel.pop();
  }

}
