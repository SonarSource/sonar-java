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
package org.sonar.java.model;

import org.apache.commons.lang.StringUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class LiteralUtils {

  private LiteralUtils() {
    // This class only contains static methods
  }

  @CheckForNull
  public static Integer intLiteralValue(ExpressionTree expression) {
    if (expression.is(Tree.Kind.INT_LITERAL)) {
      return intLiteralValue((LiteralTree) expression);
    }
    if (expression.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
      UnaryExpressionTree unaryExp = (UnaryExpressionTree) expression;
      Integer subExpressionIntValue = intLiteralValue(unaryExp.expression());
      return expression.is(Tree.Kind.UNARY_MINUS) ? minus(subExpressionIntValue) : subExpressionIntValue;
    }
    return null;
  }

  @CheckForNull
  private static Integer intLiteralValue(LiteralTree literal) {
    String literalValue = literal.value().replaceAll("_", "");
    if (literalValue.startsWith("0x") || literalValue.startsWith("0b")) {
      return null;
    }
    return Integer.valueOf(literalValue);
  }

  @CheckForNull
  public static Long longLiteralValue(ExpressionTree tree) {
    ExpressionTree expression = tree;

    int sign = tree.is(Kind.UNARY_MINUS) ? -1 : 1;
    if (tree.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      expression = ((UnaryExpressionTree) tree).expression();
    }

    if (expression.is(Kind.INT_LITERAL, Kind.LONG_LITERAL)) {
      String value = trimLongSuffix(((LiteralTree) expression).value());
      // long as hexadecimal can be written using underscore to separate groups
      value = value.replaceAll("\\_", "");
      try {
        return sign * Long.decode(value);
      } catch (NumberFormatException e) {
        // Long.decode() may fail in case of very large long number written in hexadecimal. In such situation, we ignore the number.
        // Note that Long.MAX_VALUE = "0x7FFF_FFFF_FFFF_FFFFL", but it is possible to write larger numbers in hexadecimal
        // to be used as mask in bitwise operation. For instance:
        // 0x8000_0000_0000_0000L (MAX_VALUE + 1),
        // 0xFFFF_FFFF_FFFF_FFFFL (only ones),
        // 0xFFFF_FFFF_FFFF_FFFEL (only ones except least significant bit), ...
      }
    }
    return null;
  }

  @CheckForNull
  private static Integer minus(@Nullable Integer nullableInteger) {
    return nullableInteger == null ? null : -nullableInteger;
  }

  public static boolean isEmptyString(Tree tree) {
    return tree.is(Tree.Kind.STRING_LITERAL) && trimQuotes(((LiteralTree) tree).value()).isEmpty();
  }

  public static String trimQuotes(String value) {
    return value.substring(1, value.length() - 1);
  }

  public static String trimLongSuffix(String longString) {
    if (StringUtils.isBlank(longString)) {
      return longString;
    }
    int lastCharPosition = longString.length() - 1;
    char lastChar = longString.charAt(lastCharPosition);
    String value = longString;
    if (lastChar == 'L' || lastChar == 'l') {
      value = longString.substring(0, lastCharPosition);
    }
    return value;
  }
}
