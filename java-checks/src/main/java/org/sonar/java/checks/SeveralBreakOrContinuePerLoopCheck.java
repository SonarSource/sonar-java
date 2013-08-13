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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Stack;

@Rule(
  key = "S135",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class SeveralBreakOrContinuePerLoopCheck extends SquidCheck<LexerlessGrammar> {

  private static final int IS_SWITCH = -1;

  private final Stack<Integer> loopLines = new Stack<Integer>();
  private final Stack<Integer> breakAndContinueCounter = new Stack<Integer>();

  private static final JavaGrammar[] LOOP_NODES = new JavaGrammar[] {
    JavaGrammar.FOR_STATEMENT,
    JavaGrammar.WHILE_STATEMENT,
    JavaGrammar.DO_STATEMENT
  };

  @Override
  public void init() {
    subscribeTo(JavaGrammar.SWITCH_STATEMENT);
    subscribeTo(LOOP_NODES);
    subscribeTo(JavaGrammar.BREAK_STATEMENT);
    subscribeTo(JavaGrammar.CONTINUE_STATEMENT);
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.SWITCH_STATEMENT)) {
      enterSwitch();
    } else if (node.is(LOOP_NODES)) {
      enterLoop(node.getTokenLine());
    } else if (isInsideLoopOrSwitch() && !node.is(JavaGrammar.BREAK_STATEMENT) || isInLoop()) {
      incrementLoopBreakAndContinueCounter();
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (!node.is(JavaGrammar.CONTINUE_STATEMENT, JavaGrammar.BREAK_STATEMENT)) {
      if (isInLoop() && breakAndContinueCounter.peek() > 1) {
        getContext().createLineViolation(this, "Reduce the number of break and continue statement of this loop from " + breakAndContinueCounter.peek() + " to at most 1.", node);
      }

      leave();
    }
  }

  private boolean isInsideLoopOrSwitch() {
    for (Integer line : loopLines) {
      if (line != IS_SWITCH) {
        return true;
      }
    }

    return false;
  }

  private void incrementLoopBreakAndContinueCounter() {
    for (int i = loopLines.size() - 1; i >= 0; i--) {
      if (loopLines.get(i) != IS_SWITCH) {
        int count = breakAndContinueCounter.get(i);
        breakAndContinueCounter.set(i, count + 1);
      }
    }
  }

  private void enterSwitch() {
    enter(IS_SWITCH);
  }

  private void enterLoop(int line) {
    enter(line);
  }

  private void enter(int line) {
    loopLines.push(line);
    breakAndContinueCounter.push(0);
  }

  private boolean isInLoop() {
    return !loopLines.isEmpty() &&
      loopLines.peek() != IS_SWITCH;
  }

  private void leave() {
    loopLines.pop();
    breakAndContinueCounter.pop();
  }

}
