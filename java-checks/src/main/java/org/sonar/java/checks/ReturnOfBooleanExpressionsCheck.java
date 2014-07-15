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
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

@Rule(
  key = "S1126",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ReturnOfBooleanExpressionsCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.IF_STATEMENT);
  }

  @Override
  public void visitNode(AstNode node) {
    if (hasElse(node) && isReturnBooleanLiteral(getTrueStatement(node)) && isReturnBooleanLiteral(getFalseStatement(node))) {
      getContext().createLineViolation(this, "Replace this if-then-else statement by a single return statement.", node);
    }
  }

  private static boolean hasElse(AstNode node) {
    return node.hasDirectChildren(JavaKeyword.ELSE);
  }

  private static AstNode getTrueStatement(AstNode node) {
    return node.getFirstChild(JavaGrammar.STATEMENT);
  }

  private static AstNode getFalseStatement(AstNode node) {
    return node.getLastChild();
  }

  private static boolean isReturnBooleanLiteral(AstNode node) {
    return isBlockReturnBooleanLiteral(node) ||
      isSimpleReturnBooleanLiteral(node);
  }

  private static boolean isBlockReturnBooleanLiteral(AstNode node) {
    AstNode block = node.getFirstChild(JavaGrammar.BLOCK);
    if (block == null) {
      return false;
    }

    List<AstNode> blockStatements = block.getFirstChild(JavaGrammar.BLOCK_STATEMENTS).getChildren(JavaGrammar.BLOCK_STATEMENT);
    return blockStatements.size() == 1 &&
      blockStatements.get(0).hasDirectChildren(JavaGrammar.STATEMENT) &&
      isSimpleReturnBooleanLiteral(blockStatements.get(0).getFirstChild(JavaGrammar.STATEMENT));
  }

  private static boolean isSimpleReturnBooleanLiteral(AstNode node) {
    AstNode returnStatement = node.getFirstChild(JavaGrammar.RETURN_STATEMENT);
    return returnStatement != null &&
      returnStatement.hasDirectChildren(JavaGrammar.EXPRESSION) &&
      isBooleanLiteral(returnStatement.getFirstChild(JavaGrammar.EXPRESSION));
  }

  private static boolean isBooleanLiteral(AstNode node) {
    return hasSingleToken(node) &&
      node.hasDescendant(JavaKeyword.FALSE, JavaKeyword.TRUE);
  }

  private static boolean hasSingleToken(AstNode node) {
    return node.getToken().equals(node.getLastToken());
  }

}
