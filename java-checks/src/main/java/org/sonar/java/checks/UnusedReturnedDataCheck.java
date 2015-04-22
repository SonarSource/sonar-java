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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S2677",
  name = "\"read\", \"readLine\", and \"next\" return values should be used",
  tags = {"bug"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("5min")
public class UnusedReturnedDataCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final List<MethodInvocationMatcher> CHECKED_METHODS = ImmutableList.of(
    MethodInvocationMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.io.BufferedReader"))
      .name("readLine")
      .withNoParameterConstraint(),
    MethodInvocationMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.util.Iterator"))
      .name("next")
      .withNoParameterConstraint(),
    MethodInvocationMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.io.Reader"))
      .name("read")
      .withNoParameterConstraint());

  @Nullable
  private MethodInvocationTree currentMethodTree;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    super.visitExpressionStatement(tree);
    for (MethodInvocationMatcher matcher : CHECKED_METHODS) {
      Symbol symbol = isTreeMethodInvocation(tree.expression(), matcher);
      if (symbol != null) {
        raiseIssue(tree, symbol.name());
      }
    }
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      for (MethodInvocationMatcher matcher : CHECKED_METHODS) {
        Symbol leftSymbol = isTreeMethodInvocation(tree.leftOperand(), matcher);
        if (leftSymbol != null && isTreeLiteralNull(tree.rightOperand())) {
          raiseIssue(tree, leftSymbol.name());
        }
        Symbol rightSymbol = isTreeMethodInvocation(tree.rightOperand(), matcher);
        if (rightSymbol != null && isTreeLiteralNull(tree.leftOperand())) {
          raiseIssue(tree, rightSymbol.name());
        }
      }
    }
  }

  @CheckForNull
  private Symbol isTreeMethodInvocation(ExpressionTree tree, MethodInvocationMatcher matcher) {
    Tree expression = removeParenthesis(tree);
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
      if (matcher.matches(methodInvocation)) {
        return methodInvocation.symbol();
      }
    }
    return null;
  }

  private boolean isTreeLiteralNull(ExpressionTree tree) {
    return removeParenthesis(tree).is(Tree.Kind.NULL_LITERAL);
  }

  private ExpressionTree removeParenthesis(ExpressionTree tree) {
    ExpressionTree result = tree;
    while (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      result = ((ParenthesizedTree) result).expression();
    }
    return result;
  }

  private void raiseIssue(Tree tree, String methodName) {
    context.addIssue(tree, UnusedReturnedDataCheck.this, String.format("Use or store the value returned from \"%s\" instead of throwing it away.", methodName));
  }

}
