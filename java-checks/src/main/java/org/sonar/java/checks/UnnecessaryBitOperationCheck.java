/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S2437")
public class UnnecessaryBitOperationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(
      Kind.XOR,
      Kind.XOR_ASSIGNMENT,
      Kind.AND,
      Kind.AND_ASSIGNMENT,
      Kind.OR,
      Kind.OR_ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree expression;
    SyntaxToken operatorToken;
    if (tree.is(Kind.OR, Kind.XOR, Kind.AND)) {
      BinaryExpressionTree binary = (BinaryExpressionTree) tree;
      expression = binary.rightOperand();
      operatorToken = binary.operatorToken();
    } else {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
      expression = assignment.expression();
      operatorToken = assignment.operatorToken();
    }
    Long evaluatedExpression = LiteralUtils.longLiteralValue(expression);
    if (evaluatedExpression != null && getBitwiseOperationIdentityElement(tree).equals(evaluatedExpression)) {
      reportIssue(operatorToken, "Remove this unnecessary bit operation.");
    }
  }

  private static Long getBitwiseOperationIdentityElement(Tree tree) {
    if (tree.is(Kind.AND, Kind.AND_ASSIGNMENT)) {
      return  -1L;
    }
    return 0L;
  }

}
