/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import javax.annotation.Nullable;

@Rule(
  key = "S1157",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CaseInsensitiveComparisonCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  private static enum State {
    EXPECTING_DOT_1,
    EXPECTING_TOLOWERCASE_OR_TOUPPERCASE,
    EXPECTING_LPAR_1,
    EXPECTING_RPAR_1,
    EXPECTING_DOT_2,
    EXPECTING_EQUALS,
    EXPECTING_LPAR_2,
    FOUND_ISSUE
  }

  private static enum Symbol {
    OTHER,
    DOT,
    TOLOWERCASE_OR_TOUPPERCASE,
    LPAR,
    RPAR,
    EQUALS
  }

  private static final State[][] TRANSITIONS = new State[State.values().length][Symbol.values().length];
  static {
    for (int i = 0; i < TRANSITIONS.length; i++) {
      for (int j = 0; j < TRANSITIONS[i].length; j++) {
        TRANSITIONS[i][j] = State.EXPECTING_DOT_1;
      }
    }

    TRANSITIONS[State.EXPECTING_DOT_1.ordinal()][Symbol.DOT.ordinal()] = State.EXPECTING_TOLOWERCASE_OR_TOUPPERCASE;
    TRANSITIONS[State.EXPECTING_TOLOWERCASE_OR_TOUPPERCASE.ordinal()][Symbol.TOLOWERCASE_OR_TOUPPERCASE.ordinal()] = State.EXPECTING_LPAR_1;
    TRANSITIONS[State.EXPECTING_LPAR_1.ordinal()][Symbol.LPAR.ordinal()] = State.EXPECTING_RPAR_1;
    TRANSITIONS[State.EXPECTING_RPAR_1.ordinal()][Symbol.RPAR.ordinal()] = State.EXPECTING_DOT_2;
    TRANSITIONS[State.EXPECTING_DOT_2.ordinal()][Symbol.DOT.ordinal()] = State.EXPECTING_EQUALS;

    TRANSITIONS[State.EXPECTING_EQUALS.ordinal()][Symbol.EQUALS.ordinal()] = State.EXPECTING_LPAR_2;
    TRANSITIONS[State.EXPECTING_EQUALS.ordinal()][Symbol.TOLOWERCASE_OR_TOUPPERCASE.ordinal()] = State.EXPECTING_LPAR_1;

    TRANSITIONS[State.EXPECTING_LPAR_2.ordinal()][Symbol.LPAR.ordinal()] = State.FOUND_ISSUE;
  }

  private State currentState;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.ARGUMENTS);
  }

  @Override
  public void visitFile(@Nullable AstNode node) {
    currentState = State.EXPECTING_DOT_1;
  }

  @Override
  public void visitToken(Token token) {
    currentState = TRANSITIONS[currentState.ordinal()][getSymbol(token.getOriginalValue()).ordinal()];

    if (currentState == State.FOUND_ISSUE) {
      createIssue(token.getLine());
      currentState = State.EXPECTING_DOT_1;
    }
  }

  private static Symbol getSymbol(String value) {
    Symbol result = Symbol.OTHER;

    if (value.length() == 1) {
      if (".".equals(value)) {
        result = Symbol.DOT;
      } else if ("(".equals(value)) {
        result = Symbol.LPAR;
      } else if (")".equals(value)) {
        result = Symbol.RPAR;
      }
    } else if ("toLowerCase".equals(value) || "toUpperCase".equals(value)) {
      result = Symbol.TOLOWERCASE_OR_TOUPPERCASE;
    } else if ("equals".equals(value)) {
      result = Symbol.EQUALS;
    }

    return result;
  }

  @Override
  public void visitNode(AstNode node) {
    if (isPreviousTokenEquals(node) &&
      hasOneExpression(node) &&
      endsWithToUpperCaseOrToLowerCaseCall(node.getFirstChild(JavaGrammar.EXPRESSION))) {
      createIssue(node.getTokenLine());
    }
  }

  private static boolean isPreviousTokenEquals(AstNode node) {
    return "equals".equals(node.getPreviousAstNode().getLastToken().getOriginalValue());
  }

  private static boolean hasOneExpression(AstNode node) {
    return node.hasDirectChildren(JavaGrammar.EXPRESSION) &&
      !node.hasDirectChildren(JavaPunctuator.COMMA);
  }

  private static boolean endsWithToUpperCaseOrToLowerCaseCall(AstNode node) {
    StringBuilder sb = new StringBuilder();

    for (Token token : node.getTokens()) {
      sb.append(token.getOriginalValue());
    }

    String s = sb.toString();

    return s.endsWith(".toLowerCase()") || s.endsWith(".toUpperCase()");
  }

  private void createIssue(int line) {
    getContext().createLineViolation(this, "Replace this toUpperCase()/toLowerCase() and equals() calls by a single equalsIgnoreCase() one.", line);
  }

}
