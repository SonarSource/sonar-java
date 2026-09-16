/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.springcontext;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the value of a single {@code @Profile} element into a {@link ProfileExpression}.
 *
 * <p>The grammar mirrors what Spring accepts in a profile expression:
 *
 * <pre>
 * expression := operand ( ( "&amp;" operand )+ | ( "|" operand )+ )?
 * operand    := "!" operand
 *             | "(" expression ")"
 *             | name
 * name       := trimmed, non-empty run of characters other than '(' ')' '&amp;' '|' '!'
 * </pre>
 *
 * <p>Spring's rule that {@code &} and {@code |} may not be mixed without parentheses is a property of that
 * grammar rather than a check applied afterwards. There is consequently no operator precedence to define.
 *
 * <p>Whitespace is not a delimiter, only padding trimmed at name boundaries — as in Spring, whose parser
 * tokenizes on {@code "()&|!"} and trims each token. So {@code "dev & prod"} names two profiles while
 * {@code "my profile"} names one, called {@code my profile}.
 *
 * <p>Where Spring is lenient in ways that look like typos rather than intent (e.g. {@code "(a)(b)"} merged as a
 * disjunction, {@code "a & "} reduced to {@code a}) this parser rejects instead. Every rejection yields
 * {@link ProfileExpression#UNKNOWN}, which callers treat as possibly active, so being stricter than Spring
 * only costs precision.
 */
public final class ProfileExpressionParser {

  /**
   * Nesting limit for the recursive descent, protecting against a {@link StackOverflowError} on pathological
   * input such as {@code @Profile("((((…a…))))"}. Exceeding it is reported as a parse error.
   */
  private static final int MAX_DEPTH = 64;

  private ProfileExpressionParser() {
  }

  /**
   * Parses a Spring profile expression.
   *
   * @param expression The value of one {@code @Profile} element.
   * @return The parsed expression, or {@link ProfileExpression#UNKNOWN} if it is blank or malformed.
   */
  public static ProfileExpression parse(String expression) {
    try {
      var cursor = new Cursor(expression);
      ProfileExpression parsed = cursor.expression(0);
      cursor.skipWhitespace();
      if (!cursor.atEnd()) {
        throw new ParseError();
      }
      return parsed;
    } catch (ParseError e) {
      return ProfileExpression.UNKNOWN;
    }
  }

  private static final class Cursor {

    private final String source;
    private int index;

    private Cursor(String source) {
      this.source = source;
    }

    private ProfileExpression expression(int depth) {
      ProfileExpression first = operand(depth);
      skipWhitespace();
      char operator = peek();
      if (operator != '&' && operator != '|') {
        return first;
      }
      List<ProfileExpression> operands = new ArrayList<>();
      operands.add(first);
      while (peek() == operator) {
        advance();
        operands.add(operand(depth));
        skipWhitespace();
      }
      return operator == '&' ? ProfileExpression.and(operands) : ProfileExpression.or(operands);
    }

    private ProfileExpression operand(int depth) {
      if (depth > MAX_DEPTH) {
        throw new ParseError();
      }
      skipWhitespace();
      if (peek() == '!') {
        advance();
        return ProfileExpression.not(operand(depth + 1));
      }
      if (peek() == '(') {
        advance();
        ProfileExpression nested = expression(depth + 1);
        skipWhitespace();
        expect(')');
        return nested;
      }
      return ProfileExpression.profile(name());
    }

    private String name() {
      int start = index;
      while (!atEnd() && ProfileExpression.OPERATOR_CHARACTERS.indexOf(peek()) < 0) {
        advance();
      }
      String name = source.substring(start, index).trim();
      if (name.isEmpty()) {
        throw new ParseError();
      }
      return name;
    }

    private void skipWhitespace() {
      while (!atEnd() && Character.isWhitespace(peek())) {
        advance();
      }
    }

    private void expect(char expected) {
      if (peek() != expected) {
        throw new ParseError();
      }
      advance();
    }

    private char peek() {
      return atEnd() ? '\0' : source.charAt(index);
    }

    private void advance() {
      index++;
    }

    private boolean atEnd() {
      return index >= source.length();
    }
  }

  private static class ParseError extends RuntimeException {

    private ParseError() {
      super(null, null, false, false);
    }
  }
}
