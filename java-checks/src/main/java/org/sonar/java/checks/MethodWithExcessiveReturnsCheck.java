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
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Stack;

@Rule(
  key = "S1142",
  priority = Priority.MAJOR,
  tags={"brain-overload"})
public class MethodWithExcessiveReturnsCheck extends SquidCheck<LexerlessGrammar> {

  private static final int DEFAULT_MAX = 3;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private final Stack<Integer> returnStatementCounter = new Stack<Integer>();

  @Override
  public void init() {
    subscribeTo(JavaGrammar.RETURN_STATEMENT);
    subscribeTo(JavaGrammar.CLASS_BODY_DECLARATION);
  }

  @Override
  public void visitFile(AstNode node) {
    returnStatementCounter.clear();
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.RETURN_STATEMENT)) {
      setReturnStatementCounter(getReturnStatementCounter() + 1);
    } else {
      returnStatementCounter.push(0);
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.CLASS_BODY_DECLARATION)) {
      if (isMethod(node) && getReturnStatementCounter() > max) {
        getContext().createLineViolation(
            this,
            "Reduce the number of returns of this method " + getReturnStatementCounter() + ", down to the maximum allowed " + max + ".",
            node);
      }

      returnStatementCounter.pop();
    }
  }

  private static boolean isMethod(AstNode node) {
    return !node.hasDirectChildren(JavaGrammar.CLASS_INIT_DECLARATION);
  }

  private int getReturnStatementCounter() {
    return returnStatementCounter.peek();
  }

  private void setReturnStatementCounter(int value) {
    returnStatementCounter.pop();
    returnStatementCounter.push(value);
  }

}
