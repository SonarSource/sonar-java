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
package org.sonar.java.checks.unused;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2677")
public class UnusedReturnedDataCheck extends IssuableSubscriptionVisitor {

  private static final List<MethodMatcher> CHECKED_METHODS = Arrays.asList(
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.io.BufferedReader"))
      .name("readLine")
      .withoutParameter(),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.io.Reader"))
      .name("read")
      .withoutParameter());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      CHECKED_METHODS.stream()
        .map(matcher -> isTreeMethodInvocation(((ExpressionStatementTree) tree).expression(), matcher))
        .filter(Objects::nonNull)
        .forEach(mit -> raiseIssue(ExpressionUtils.methodName(mit)));
    } else {
      BinaryExpressionTree expressionTree = (BinaryExpressionTree) tree;
      ExpressionTree leftOperand = expressionTree.leftOperand();
      ExpressionTree rightOperand = expressionTree.rightOperand();
      for (MethodMatcher matcher : CHECKED_METHODS) {
        MethodInvocationTree leftMit = isTreeMethodInvocation(leftOperand, matcher);
        if (leftMit != null && isTreeLiteralNull(rightOperand)) {
          raiseIssue(ExpressionUtils.methodName(leftMit));
        }
        MethodInvocationTree rightMit = isTreeMethodInvocation(rightOperand, matcher);
        if (rightMit != null && isTreeLiteralNull(leftOperand)) {
          raiseIssue(ExpressionUtils.methodName(rightMit));
        }
      }
    }
  }

  @CheckForNull
  private static MethodInvocationTree isTreeMethodInvocation(ExpressionTree tree, MethodMatcher matcher) {
    Tree expression = ExpressionUtils.skipParentheses(tree);
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
      if (matcher.matches(methodInvocation)) {
        return methodInvocation;
      }
    }
    return null;
  }

  private static boolean isTreeLiteralNull(ExpressionTree tree) {
    return ExpressionUtils.skipParentheses(tree).is(Tree.Kind.NULL_LITERAL);
  }

  private void raiseIssue(IdentifierTree identifierTree) {
    reportIssue(identifierTree, String.format("Use or store the value returned from \"%s\" instead of throwing it away.", identifierTree.identifierToken().text()));
  }

}
