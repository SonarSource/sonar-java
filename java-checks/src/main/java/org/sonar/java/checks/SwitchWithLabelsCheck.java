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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1219",
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class SwitchWithLabelsCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode blockStatements = node.getFirstChild(JavaGrammar.BLOCK_STATEMENTS);
    for (AstNode blockStatement : blockStatements.getChildren(JavaGrammar.BLOCK_STATEMENT)) {
      if (isLabeledBlockStatement(blockStatement)) {
        getContext().createLineViolation(this, "Remove this misleading \"" + blockStatement.getTokenOriginalValue() + "\" label.", blockStatement);
      }
    }
  }

  private static boolean isLabeledBlockStatement(AstNode node) {
    AstNode statement = node.getFirstChild(JavaGrammar.STATEMENT);
    return statement != null &&
      statement.hasDirectChildren(JavaGrammar.LABELED_STATEMENT);
  }

}
