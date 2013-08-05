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

  private enum State {
    EXPECTING_DOT_1,
    EXPECTING_TOLOWERCASE_OR_TOUPPERCASE,
    EXPECTING_LPAR_1,
    EXPECTING_RPAR_1,
    EXPECTING_DOT_2,
    EXPECTING_EQUALS,
    EXPECTING_LPAR_2
  }

  private State state;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.ARGUMENTS);
  }

  @Override
  public void visitFile(@Nullable AstNode node) {
    state = State.EXPECTING_DOT_1;
  }

  @Override
  public void visitToken(Token token) {
    String value = token.getOriginalValue();

    switch (state) {
      case EXPECTING_DOT_1:
        state = transitionIfMatch(value, ".", State.EXPECTING_TOLOWERCASE_OR_TOUPPERCASE);
        break;
      case EXPECTING_TOLOWERCASE_OR_TOUPPERCASE:
        state = transitionIfMatch(value, "toLowerCase", State.EXPECTING_LPAR_1);
        if (state != State.EXPECTING_LPAR_1) {
          state = transitionIfMatch(value, "toUpperCase", State.EXPECTING_LPAR_1);
        }
        break;
      case EXPECTING_LPAR_1:
        state = transitionIfMatch(value, "(", State.EXPECTING_RPAR_1);
        break;
      case EXPECTING_RPAR_1:
        state = transitionIfMatch(value, ")", State.EXPECTING_DOT_2);
        break;
      case EXPECTING_DOT_2:
        state = transitionIfMatch(value, ".", State.EXPECTING_EQUALS);
        break;
      case EXPECTING_EQUALS:
        state = transitionIfMatch(value, "equals", State.EXPECTING_LPAR_2);
        if (state != State.EXPECTING_LPAR_2) {
          state = transitionIfMatch(value, "toLowerCase", State.EXPECTING_LPAR_1);
          if (state != State.EXPECTING_LPAR_1) {
            state = transitionIfMatch(value, "toUpperCase", State.EXPECTING_LPAR_1);
          }
        }
        break;
      case EXPECTING_LPAR_2:
        if ("(".equals(value)) {
          createIssue(token.getLine());
        }

        state = State.EXPECTING_DOT_1;
        break;
      default:
        throw new IllegalStateException("Illegal state " + state);
    }
  }

  private static State transitionIfMatch(String actual, String expected, State ifEqual) {
    return actual.equals(expected) ? ifEqual : State.EXPECTING_DOT_1;
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
    getContext().createLineViolation(this, "Replace this equals() and toUpperCase()/toLowerCase() by equalsIgnoreCase().", line);
  }

}
