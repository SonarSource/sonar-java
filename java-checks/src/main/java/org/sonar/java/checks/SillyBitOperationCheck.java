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
public class SillyBitOperationCheck extends IssuableSubscriptionVisitor {

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
      reportIssue(operatorToken, "Remove this silly bit operation.");
    }
  }

  private static Long getBitwiseOperationIdentityElement(Tree tree) {
    if (tree.is(Kind.AND, Kind.AND_ASSIGNMENT)) {
      return  -1L;
    }
    return 0L;
  }

}
