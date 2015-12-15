/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

@Rule(
  key = "S2677",
  name = "\"read\" and \"readLine\" return values should be used",
  priority = Priority.BLOCKER,
  tags = {Tag.BUG})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("5min")
public class UnusedReturnedDataCheck extends SubscriptionBaseVisitor {

  private static final List<MethodMatcher> CHECKED_METHODS = ImmutableList.of(
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.io.BufferedReader"))
      .name("readLine"),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.io.Reader"))
      .name("read"));

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      for (MethodMatcher matcher : CHECKED_METHODS) {
        Symbol symbol = isTreeMethodInvocation(((ExpressionStatementTree) tree).expression(), matcher);
        if (symbol != null) {
          raiseIssue(tree, symbol.name());
        }
      }
    } else {
      BinaryExpressionTree expressionTree = (BinaryExpressionTree) tree;
      for (MethodMatcher matcher : CHECKED_METHODS) {
        Symbol leftSymbol = isTreeMethodInvocation(expressionTree.leftOperand(), matcher);
        if (leftSymbol != null && isTreeLiteralNull(expressionTree.rightOperand())) {
          raiseIssue(tree, leftSymbol.name());
        }
        Symbol rightSymbol = isTreeMethodInvocation(expressionTree.rightOperand(), matcher);
        if (rightSymbol != null && isTreeLiteralNull(expressionTree.leftOperand())) {
          raiseIssue(tree, rightSymbol.name());
        }
      }
    }
  }

  @CheckForNull
  private static Symbol isTreeMethodInvocation(ExpressionTree tree, MethodMatcher matcher) {
    Tree expression = ExpressionsHelper.skipParentheses(tree);
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
      if (matcher.matches(methodInvocation)) {
        return methodInvocation.symbol();
      }
    }
    return null;
  }

  private static boolean isTreeLiteralNull(ExpressionTree tree) {
    return ExpressionsHelper.skipParentheses(tree).is(Tree.Kind.NULL_LITERAL);
  }

  private void raiseIssue(Tree tree, String methodName) {
    addIssue(tree, String.format("Use or store the value returned from \"%s\" instead of throwing it away.", methodName));
  }

}
