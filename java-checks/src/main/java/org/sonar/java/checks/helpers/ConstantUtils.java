/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks.helpers;

import javax.annotation.CheckForNull;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;

public class ConstantUtils {

  private ConstantUtils() {
  }

  @CheckForNull
  public static String resolveAsStringConstant(ExpressionTree tree) {
    ExpressionTree expression = tree;
    while (expression.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      expression = ((ParenthesizedTree) expression).expression();
    }
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      expression = ((MemberSelectExpressionTree) expression).identifier();
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree id = (IdentifierTree) expression;
      Symbol symbol = id.symbol();
      if (symbol instanceof JavaSymbol.ConstantJavaSymbol) {
        Object constantValue = ((JavaSymbol.ConstantJavaSymbol) symbol).value();
        if (constantValue instanceof String) {
          return (String) constantValue;
        }
      }
    }
    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      return LiteralUtils.trimQuotes(((LiteralTree) expression).value());
    }
    if (expression.is(Tree.Kind.PLUS)) {
      BinaryExpressionTree binaryExpression = (BinaryExpressionTree) expression;
      String left = resolveAsStringConstant(binaryExpression.leftOperand());
      String right = resolveAsStringConstant(binaryExpression.rightOperand());
      if (left != null && right != null) {
        return left + right;
      }
    }
    return null;
  }

}
