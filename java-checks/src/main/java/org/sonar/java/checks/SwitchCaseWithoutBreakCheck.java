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

@Rule(
  key = "S128",
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class SwitchCaseWithoutBreakCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode lastBlockStatement = getLastStatement(node);
    if (lastBlockStatement != null && !isBreakContinueReturnOrThrow(lastBlockStatement)) {
      getContext().createLineViolation(this, "End this switch case with an unconditional break, continue, return or throw statement.", node);
    }
  }

  private static AstNode getLastStatement(AstNode node) {
    return node.getFirstChild(JavaGrammar.BLOCK_STATEMENTS).getLastChild();
  }

  private static boolean isBreakContinueReturnOrThrow(AstNode blockStatement) {
    AstNode statement = blockStatement.getFirstChild(JavaGrammar.STATEMENT);
    if (statement == null) {
      return false;
    }

    return statement.hasDirectChildren(JavaGrammar.BREAK_STATEMENT, JavaGrammar.CONTINUE_STATEMENT, JavaGrammar.RETURN_STATEMENT, JavaGrammar.THROW_STATEMENT);
  }

}
