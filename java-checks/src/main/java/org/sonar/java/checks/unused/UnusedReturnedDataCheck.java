/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.unused;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2677")
public class UnusedReturnedDataCheck extends IssuableSubscriptionVisitor {

  private static final List<MethodMatchers> CHECKED_METHODS = Arrays.asList(
    MethodMatchers.create()
      .ofSubTypes("java.io.BufferedReader")
      .names("readLine")
      .addWithoutParametersMatcher()
    .build(),
    MethodMatchers.create()
      .ofSubTypes("java.io.Reader")
      .names("read")
      .addWithoutParametersMatcher()
      .build());

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
      for (MethodMatchers matcher : CHECKED_METHODS) {
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
  private static MethodInvocationTree isTreeMethodInvocation(ExpressionTree tree, MethodMatchers matcher) {
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
