/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Deque;
import java.util.LinkedList;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
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

@Rule(key = "S1643")
public class StringConcatenationInLoopCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private Deque<Tree> loopLevel = new LinkedList<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    loopLevel.clear();
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
    if (idTree == null) {
      return false;
    }
    Tree declaration = idTree.symbol().declaration();
    if (declaration == null) {
      return true;
    }
    Tree parent = declaration.parent();
    Tree parentLoop = loopLevel.peek();
    while (parent != null && !parent.equals(parentLoop)) {
      if (parent.is(
        Tree.Kind.CLASS,
        Tree.Kind.ENUM,
        Tree.Kind.INTERFACE,
        Tree.Kind.METHOD,
        Tree.Kind.LAMBDA_EXPRESSION)) {
        // declaration is necessarily not part of a loop
        return true;
      }
      parent = parent.parent();
    }
    return parent == null;
  }

  private static boolean isNotArrayAccess(AssignmentExpressionTree tree) {
    return !tree.variable().is(Tree.Kind.ARRAY_ACCESS_EXPRESSION);
  }

  @Nullable
  private static IdentifierTree getIdentifierTree(ExpressionTree tree) {
    tree = ExpressionUtils.skipParentheses(tree);
    switch (tree.kind()) {
      case IDENTIFIER:
        return (IdentifierTree) tree;
      case MEMBER_SELECT:
        return getIdentifierTree(((MemberSelectExpressionTree) tree).expression());
      case ARRAY_ACCESS_EXPRESSION:
        return getIdentifierTree(((ArrayAccessExpressionTree) tree).expression());
      case METHOD_INVOCATION:
        return getIdentifierTree(((MethodInvocationTree) tree).methodSelect());
      default:
        // any other unsupported case
        return null;
    }
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
