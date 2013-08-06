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

import java.util.List;

@Rule(
  key = "S1164",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CatchWhichOnlyRethrowsCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CATCH_CLAUSE);
  }

  @Override
  public void visitNode(AstNode node) {
    List<AstNode> blockStatements = node.getFirstChild(JavaGrammar.BLOCK)
        .getFirstChild(JavaGrammar.BLOCK_STATEMENTS)
        .getChildren(JavaGrammar.BLOCK_STATEMENT);

    if (blockStatements.size() == 1 && isRethrowStatement(node, blockStatements.get(0))) {
      getContext().createLineViolation(this, "Remove this useless catch block.", node);
    }
  }

  private static boolean isRethrowStatement(AstNode catchClause, AstNode blockStatement) {
    return getCaughtVariable(catchClause).equals(getThrownVariable(blockStatement));
  }

  private static String getCaughtVariable(AstNode catchClause) {
    return catchClause.getFirstChild(JavaGrammar.CATCH_FORMAL_PARAMETER)
        .getFirstChild(JavaGrammar.VARIABLE_DECLARATOR_ID)
        .getTokenOriginalValue();
  }

  private static String getThrownVariable(AstNode blockStatement) {
    AstNode statement = blockStatement.getFirstChild(JavaGrammar.STATEMENT);
    if (statement == null) {
      return "";
    }

    AstNode throwStatement = statement.getFirstChild(JavaGrammar.THROW_STATEMENT);
    if (throwStatement == null) {
      return "";
    }

    AstNode expression = throwStatement.getFirstChild(JavaGrammar.EXPRESSION);

    return expression.getToken().equals(expression.getLastToken()) ? expression.getTokenOriginalValue() : "";
  }

}
