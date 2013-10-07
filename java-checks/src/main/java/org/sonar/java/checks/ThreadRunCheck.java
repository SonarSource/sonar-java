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
import com.sonar.sslr.api.Token;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1217",
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ThreadRunCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  enum State {
    EXPECT_RUN,
    EXPECT_LPAREN,
    EXPECT_RPAREN,
    EXPECT_SEMI
  }

  private State state = State.EXPECT_RUN;

  @Override
  public void visitToken(Token token) {
    switch (state) {
      case EXPECT_RUN:
        transitionIfMatch(token, "run", State.EXPECT_LPAREN);
        break;
      case EXPECT_LPAREN:
        transitionIfMatch(token, "(", State.EXPECT_RPAREN);
        break;
      case EXPECT_RPAREN:
        transitionIfMatch(token, ")", State.EXPECT_SEMI);
        break;
      case EXPECT_SEMI:
        if (";".equals(token.getOriginalValue())) {
          getContext().createLineViolation(this, "Call the method Thread.start() to execute the content of the run() method in a dedicated thread.", token);
        }
        state = State.EXPECT_RUN;
        break;
      default:
        throw new IllegalStateException();
    }
  }

  private void transitionIfMatch(Token token, String expected, State target) {
    state = expected.equals(token.getOriginalValue()) ? target : State.EXPECT_RUN;
  }

}
