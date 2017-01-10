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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import javax.annotation.Nullable;

@Rule(key = "AssignmentInSubExpressionCheck")
@RspecKey("S1121")
public class AssignmentInSubExpressionCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    scan(context.getTree());
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    //skip scanning of annotation : assignment in annotation is normal behaviour
    scan(annotationTree.annotationType());
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    //skip lambda if body is an assignement
    if(!lambdaExpressionTree.body().is(Kind.ASSIGNMENT)) {
      super.visitLambdaExpression(lambdaExpressionTree);
    }
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    ExpressionTree expressionTree = tree.expression();

    while (expressionTree instanceof AssignmentExpressionTree) {
      AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) expressionTree;
      scan(assignmentExpressionTree.variable());
      expressionTree = assignmentExpressionTree.expression();
    }

    scan(expressionTree);
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (isRelationalExpression(tree)) {
      visitInnerExpression(tree.leftOperand());
      visitInnerExpression(tree.rightOperand());
    } else {
      super.visitBinaryExpression(tree);
    }
  }

  private void visitInnerExpression(ExpressionTree tree) {
    AssignmentExpressionTree assignmentExpressionTree = getInnerAssignmentExpression(tree);
    if (assignmentExpressionTree != null) {
      super.visitAssignmentExpression(assignmentExpressionTree);
    } else {
      scan(tree);
    }
  }

  @Nullable
  private static AssignmentExpressionTree getInnerAssignmentExpression(ExpressionTree tree) {
    if (tree.is(Kind.PARENTHESIZED_EXPRESSION)) {
      ParenthesizedTree parenthesizedTree = (ParenthesizedTree) tree;

      if (parenthesizedTree.expression().is(Kind.ASSIGNMENT)) {
        return (AssignmentExpressionTree) parenthesizedTree.expression();
      }
    }

    return null;
  }

  private static boolean isRelationalExpression(Tree tree) {
    return tree.is(
      Kind.EQUAL_TO,
      Kind.NOT_EQUAL_TO,
      Kind.LESS_THAN,
      Kind.LESS_THAN_OR_EQUAL_TO,
      Kind.GREATER_THAN,
      Kind.GREATER_THAN_OR_EQUAL_TO);
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    scan(tree.statement());
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    context.reportIssue(this, tree.operatorToken(), "Extract the assignment out of this expression.");
  }

}
