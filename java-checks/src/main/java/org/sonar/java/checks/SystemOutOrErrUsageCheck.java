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
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.sslr.parser.LexerlessGrammar;

import javax.annotation.Nullable;

@Rule(
  key = "S106",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class SystemOutOrErrUsageCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  private static enum State {
    EXPECTING_SYSTEM,
    EXPECTING_DOT,
    EXPECTING_OUT_OR_ERR,
    FOUND_ISSUE
  }

  private static enum Symbol {
    OTHER,
    SYSTEM,
    DOT,
    OUT_OR_ERR
  }

  private static final State[][] TRANSITIONS = new State[State.values().length][Symbol.values().length];
  static {
    for (int i = 0; i < TRANSITIONS.length; i++) {
      for (int j = 0; j < TRANSITIONS[i].length; j++) {
        TRANSITIONS[i][j] = State.EXPECTING_SYSTEM;
      }
    }

    TRANSITIONS[State.EXPECTING_SYSTEM.ordinal()][Symbol.SYSTEM.ordinal()] = State.EXPECTING_DOT;
    TRANSITIONS[State.EXPECTING_DOT.ordinal()][Symbol.DOT.ordinal()] = State.EXPECTING_OUT_OR_ERR;
    TRANSITIONS[State.EXPECTING_OUT_OR_ERR.ordinal()][Symbol.OUT_OR_ERR.ordinal()] = State.FOUND_ISSUE;
  }

  private State currentState;

  @Override
  public void visitFile(@Nullable AstNode node) {
    currentState = State.EXPECTING_SYSTEM;
  }

  @Override
  public void visitToken(Token token) {
    currentState = TRANSITIONS[currentState.ordinal()][getSymbol(token.getOriginalValue()).ordinal()];

    if (currentState == State.FOUND_ISSUE) {
      getContext().createLineViolation(this, "Replace this usage of System.out or System.err by a logger.", token);
      currentState = State.EXPECTING_SYSTEM;
    }
  }

  private static Symbol getSymbol(String value) {
    Symbol result = Symbol.OTHER;

    if (".".equals(value)) {
      result = Symbol.DOT;
    } else if ("System".equals(value)) {
      result = Symbol.SYSTEM;
    } else if ("out".equals(value) || "err".equals(value)) {
      result = Symbol.OUT_OR_ERR;
    }

    return result;
  }

}
