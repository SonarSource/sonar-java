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
    Object constant = resolveAsConstant(tree);
    return constant instanceof String ? (String) constant : null;
  }

  @CheckForNull
  public static Integer resolveAsIntConstant(ExpressionTree tree) {
    Object constant = resolveAsConstant(tree);
    return constant instanceof Integer ? (Integer) constant : null;
  }

  @CheckForNull
  public static Long resolveAsLongConstant(ExpressionTree tree) {
    Object constant = resolveAsConstant(tree);
    if (constant instanceof Long) {
      return (Long) constant;
    }
    if (constant instanceof Integer) {
      return ((Integer) constant).longValue();
    }
    return null;
  }

  @CheckForNull
  public static Boolean resolveAsBooleanConstant(ExpressionTree tree) {
    Object constant = resolveAsConstant(tree);
    return constant instanceof Boolean ? (Boolean) constant : null;
  }

  @CheckForNull
  public static Object resolveAsConstant(ExpressionTree tree) {
    ExpressionTree expression = tree;
    while (expression.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      expression = ((ParenthesizedTree) expression).expression();
    }
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      expression = ((MemberSelectExpressionTree) expression).identifier();
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      return resolveIdentifier((IdentifierTree) expression);
    }
    if (expression.is(Tree.Kind.BOOLEAN_LITERAL)) {
      return Boolean.parseBoolean(((LiteralTree) expression).value());
    }
    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      return LiteralUtils.trimQuotes(((LiteralTree) expression).value());
    }
    if (tree.is(Tree.Kind.INT_LITERAL, Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
      return LiteralUtils.intLiteralValue(tree);
    }
    if (tree.is(Tree.Kind.LONG_LITERAL)) {
      return LiteralUtils.longLiteralValue(tree);
    }
    if (expression.is(Tree.Kind.PLUS)) {
      return resolvePlus((BinaryExpressionTree) expression);
    }
    return null;
  }

  @CheckForNull
  private static Object resolveIdentifier(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    if (!symbol.isVariableSymbol()) {
      return null;
    }
    Symbol owner = symbol.owner();
    if (owner.isTypeSymbol() && owner.type().is("java.lang.Boolean")) {
      if ("TRUE".equals(symbol.name())) {
        return Boolean.TRUE;
      } else if ("FALSE".equals(symbol.name())) {
        return Boolean.FALSE;
      }
    }
    return ((JavaSymbol.VariableJavaSymbol) symbol).constantValue().orElse(null);
  }

  @CheckForNull
  private static Object resolvePlus(BinaryExpressionTree binaryExpression) {
    Object left = resolveAsConstant(binaryExpression.leftOperand());
    Object right = resolveAsConstant(binaryExpression.rightOperand());
    if (left == null || right == null) {
      return null;
    } else if (left instanceof String) {
      return ((String) left) + right;
    } else if (right instanceof String) {
      return left + ((String) right);
    } else if (left instanceof Long && right instanceof Long) {
      return ((Long) left) + ((Long) right);
    } else if (left instanceof Long && right instanceof Integer) {
      return ((Long) left) + ((Integer) right);
    } else if (left instanceof Integer && right instanceof Long) {
      return ((Integer) left) + ((Long) right);
    } else if (left instanceof Integer && right instanceof Integer) {
      return ((Integer) left) + ((Integer) right);
    }
    return null;
  }

}
