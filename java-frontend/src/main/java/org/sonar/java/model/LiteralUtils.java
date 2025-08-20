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
package org.sonar.java.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IntLiteralTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.LongLiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

public class LiteralUtils {

  public static final String TEXT_BLOCK_QUOTE = "\"\"\"";
  public static final int TEXT_BLOCK_QUOTE_LEN = TEXT_BLOCK_QUOTE.length();

  private LiteralUtils() {
    // This class only contains static methods
  }

  @CheckForNull
  public static Integer intLiteralValue(ExpressionTree expression) {
    if (expression instanceof IntLiteralTree intLiteralTree) {
      return intLiteralTree.intValue();
    }
    if (expression.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
      UnaryExpressionTree unaryExp = (UnaryExpressionTree) expression;
      Integer subExpressionIntValue = intLiteralValue(unaryExp.expression());
      return expression.is(Tree.Kind.UNARY_MINUS) ? minus(subExpressionIntValue) : subExpressionIntValue;
    }
    return null;
  }

  @CheckForNull
  public static Long longLiteralValue(ExpressionTree tree) {
    ExpressionTree expression = tree;

    long sign = tree.is(Kind.UNARY_MINUS) ? -1L : 1L;
    if (tree.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      expression = ((UnaryExpressionTree) tree).expression();
    }

    if (expression instanceof LongLiteralTree longLiteralTree) {
      return sign * longLiteralTree.longValue();
    } else if(expression instanceof IntLiteralTree intLiteralTree) {
      return sign * intLiteralTree.intValue();
    }
    return null;
  }

  public static long parseJavaLiteralLong(String tokenValue) {
    String value = stripUnderscores(tokenValue);
    int end = literalLongEndWithoutSuffix(value);
    char ch0 = end > 0 ? value.charAt(0) : 0;
    char ch1 = end > 1 ? value.charAt(1) : 0;
    boolean negative = false;
    int start = 0;
    if (ch0 == '-' || ch0 == '+') {
      negative = ch0 == '-';
      start++;
      ch0 = ch1;
      ch1 = end > 2 ? value.charAt(2) : 0;
    }
    if (ch0 == '0') {
      if (ch1 == 'x' || ch1 == 'X') {
        return internalParseLong(start + 2, end, value, 16, negative);
      } else if (ch1 == 'b' || ch1 == 'B') {
        return internalParseLong(start + 2, end, value, 2, negative);
      } else if (ch1 >= '0' && ch1 <= '9') {
        return internalParseLong(start + 1, end, value, 8, negative);
      }
    } else if (ch0 == '#') {
      return internalParseLong(start + 1, end, value, 16, negative);
    }
    return internalParseLong(start, end, value, 10, negative);
  }

  private static int literalLongEndWithoutSuffix(String value) {
    int end = value.length();
    char lastCh = end > 0 ? value.charAt(end - 1) : 0;
    if (lastCh == 'L' || lastCh == 'l') {
      end--;
    }
    return end;
  }

  private static long internalParseLong(int start, int length, String value, int radix, boolean negative) {
    if (start == length) {
      // return 0 for an empty string instead of throwing a NumberFormatException("Zero length string")
      return 0;
    }
    long result = Long.parseUnsignedLong(value, start, length, radix);
    return negative ? -result : result;
  }

  public static double parseJavaLiteralDouble(String tokenValue) {
    return Double.parseDouble(stripUnderscores(tokenValue));
  }

  private static String stripUnderscores(String value) {
    if (value.indexOf('_') != -1) {
      return value.replace("_", "");
    }
    return value;
  }

  @CheckForNull
  public static Double doubleLiteralValue(ExpressionTree expression) {
    double sign = 1;
    if (expression.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      sign = expression.is(Kind.UNARY_MINUS) ? -1d : 1d;
      expression = ((UnaryExpressionTree) expression).expression();
    }
    if (expression instanceof LiteralTree literalTree) {
      Object parsedValue = literalTree.parsedValue();
      if (parsedValue instanceof Number number) {
        return sign * number.doubleValue();
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

  public static boolean is0xff(ExpressionTree expression) {
    return expression.is(Tree.Kind.INT_LITERAL) && "0xff".equalsIgnoreCase(((LiteralTree) expression).value());
  }

  public static String trimQuotes(String value) {
    int delimiterLength = isTextBlock(value) ? 3 : 1;
    return value.substring(delimiterLength, value.length() - delimiterLength);
  }

  public static boolean isTextBlock(String value) {
    return value.startsWith(TEXT_BLOCK_QUOTE);
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

  public static boolean hasValue(Tree tree, String expectedValue) {
    if (!tree.is(Kind.STRING_LITERAL)) {
      return false;
    }
    String actualValue = trimQuotes(((LiteralTree) tree).value());
    return expectedValue.equals(actualValue);
  }

  public static boolean isTrue(Tree tree) {
    return tree.is(Kind.BOOLEAN_LITERAL) && "true".equals(((LiteralTree) tree).value());
  }

  public static boolean isFalse(Tree tree) {
    return tree.is(Kind.BOOLEAN_LITERAL) && "false".equals(((LiteralTree) tree).value());
  }

  public static boolean isZero(ExpressionTree tree) {
    return tree.is(Kind.INT_LITERAL) && "0".equals(((LiteralTree) tree).value());
  }

  public static boolean isOne(ExpressionTree tree) {
    return tree.is(Kind.INT_LITERAL) && "1".equals(((LiteralTree) tree).value());
  }

  public static boolean isNegOne(ExpressionTree tree) {
    return tree.is(Kind.UNARY_MINUS) && isOne(((UnaryExpressionTree) tree).expression());
  }

  public static String unquote(String token, char quoteChar) {
    int len = token.length();
    if (len < 2 || token.charAt(0) != quoteChar || token.charAt(len - 1) != quoteChar) {
      throw new IllegalArgumentException("Unexpected token, can't unwrap character " + quoteChar + " around token: " + token);
    }
    return token.substring(1, len -1);
  }

  /**
   * The escaped characters stay escaped, {@link String#translateEscapes()} is not used.
   * See <a href="https://docs.oracle.com/javase/specs/jls/se24/html/jls-3.html#jls-3.10.6">jls-3.10.6</a> for details.
   */
  public static String removeTextBlockQuoteIndentationAndTrailingWhitespaces(String token) {
    if (token.length() < TEXT_BLOCK_QUOTE_LEN * 2 || !token.startsWith(TEXT_BLOCK_QUOTE) || !token.endsWith(TEXT_BLOCK_QUOTE)) {
      throw new IllegalArgumentException("Unexpected token, can't unwrap text block quotes " + TEXT_BLOCK_QUOTE + " from: " + token);
    }
    int start = TEXT_BLOCK_QUOTE_LEN;
    int end = token.length() - TEXT_BLOCK_QUOTE_LEN;
    // ignore whitespaces after the leading """
    while (start != end && isWhiteSpace(token.charAt(start))) {
      start++;
    }
    // ignore the first new line \r?\n|\r
    if (start < end && (token.charAt(start) == '\r')) {
      start++;
    }
    if (start < end && (token.charAt(start) == '\n')) {
      start++;
    }
    return token.substring(start, end).stripIndent();
  }

  public static int indentationOfTextBlock(String[] lines) {
    int minIndentation = Integer.MAX_VALUE;
    // ignore line 0, it contains only """[ \t\f]* and should be ignored to compute the indentation
    for (int i = 1; i < lines.length; i++) {
      minIndentation = Math.min(singleLineIndentation(lines[i], minIndentation), minIndentation);
    }
    return minIndentation == Integer.MAX_VALUE ? 0 : minIndentation;
  }

  /**
   * @return true if c is a white space character (space, tab or form feed) according to the space-stripping rules of text blocks
   * See <a href="https://docs.oracle.com/javase/specs/jls/se24/html/jls-3.html#jls-3.10.6">jls-3.10.6</a> for details.
   */
  private static boolean isWhiteSpace(int c) {
    return c == ' ' || c == '\t' || c == '\f';
  }

  private static int singleLineIndentation(String line, int defaultIfBlank) {
    int len = line.length();
    for (int i = 0; i < len; i++) {
      if (!isWhiteSpace(line.charAt(i))) {
        return i;
      }
    }
    return defaultIfBlank;
  }

  public static int lineCount(String content) {
    int count = 1;
    int pos = content.indexOf('\n');
    while (pos != -1) {
      count++;
      pos = content.indexOf('\n', pos + 1);
    }
    return count;
  }

}
