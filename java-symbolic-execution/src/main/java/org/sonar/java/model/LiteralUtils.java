/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

public class LiteralUtils {

  private LiteralUtils() {
    // This class only contains static methods
  }

  @CheckForNull
  public static Integer intLiteralValue(ExpressionTree expression) {
    if (expression.is(Kind.INT_LITERAL)) {
      return intLiteralValue((LiteralTree) expression);
    }
    if (expression.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      UnaryExpressionTree unaryExp = (UnaryExpressionTree) expression;
      Integer subExpressionIntValue = intLiteralValue(unaryExp.expression());
      return expression.is(Kind.UNARY_MINUS) ? minus(subExpressionIntValue) : subExpressionIntValue;
    }
    return null;
  }

  private static Integer intLiteralValue(LiteralTree literal) {
    String literalValue = literal.value().replace("_", "");
    if (literalValue.startsWith("0b") || literalValue.startsWith("0B")) {
      // assume it is used as bit mask
      return Integer.parseUnsignedInt(literalValue.substring(2), 2);
    }
    return Long.decode(literalValue).intValue();
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
      value = value.replace("_", "");
      try {
        if (value.startsWith("0b") || value.startsWith("0B")) {
          return sign * Long.valueOf(value.substring(2), 2);
        }
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

  public static String trimQuotes(String value) {
    int delimiterLength = isTextBlock(value) ? 3 : 1;
    return value.substring(delimiterLength, value.length() - delimiterLength);
  }

  public static boolean isTextBlock(String value) {
    return value.startsWith("\"\"\"");
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

  public static boolean isTrue(Tree tree) {
    return tree.is(Kind.BOOLEAN_LITERAL) && "true".equals(((LiteralTree) tree).value());
  }

  public static boolean isFalse(Tree tree) {
    return tree.is(Kind.BOOLEAN_LITERAL) && "false".equals(((LiteralTree) tree).value());
  }

  public static String getAsStringValue(LiteralTree tree) {
    if (!tree.is(Kind.TEXT_BLOCK)) {
      return tree.is(Kind.STRING_LITERAL) ? trimQuotes(tree.value()) : tree.value();
    }
    String[] lines = tree.value().split("\r?\n");
    int indent = indentationOfTextBlock(lines);

    return Arrays.stream(lines)
      .skip(1)
      .map(String::stripTrailing)
      .map(s -> stripIndent(indent, s))
      .collect(Collectors.joining("\n"))
      .replaceAll("\"\"\"$", "");
  }

  private static String stripIndent(int indent, String s) {
    return s.isEmpty() ? s : s.substring(indent);
  }

  public static int indentationOfTextBlock(String[] lines) {
    return Arrays.stream(lines).skip(1)
      .filter(LiteralUtils::isNonEmptyLine)
      .mapToInt(LiteralUtils::getIndentation)
      .min().orElse(0);
  }

  private static boolean isNonEmptyLine(String line) {
    return line.chars().anyMatch(LiteralUtils::isNotWhiteSpace);
  }

  /**
   * @return Whether c is not a white space character according to the space-stripping rules of text blocks, i.e. whether
   *         it's a space, tab or form feed
   */
  private static boolean isNotWhiteSpace(int c) {
    return c != ' ' && c != '\t' && c != '\f';
  }

  private static int getIndentation(String line) {
    for (int i = 0; i < line.length(); ++i) {
      if (isNotWhiteSpace(line.charAt(i))) {
        return i;
      }
    }
    return line.length();
  }
}
