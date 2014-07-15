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
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "IndentationCheck",
  priority = Priority.MAJOR,
  tags={"convention"})
public class IndentationCheck extends SquidCheck<LexerlessGrammar> {

  private static final AstNodeType[] BLOCK_TYPES = new AstNodeType[] {
    JavaGrammar.CLASS_BODY,
    JavaGrammar.ENUM_BODY,
    JavaGrammar.INTERFACE_BODY,
    JavaGrammar.BLOCK,
    JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUPS,
    JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP
  };

  private static final AstNodeType[] CHECKED_TYPES = new AstNodeType[] {
    JavaGrammar.TYPE_DECLARATION,
    JavaGrammar.CLASS_BODY_DECLARATION,
    JavaGrammar.INTERFACE_BODY_DECLARATION,
    JavaGrammar.BLOCK_STATEMENT
  };

  private static final int DEFAULT_INDENTATION_LEVEL = 2;

  @RuleProperty(
    key = "indentationLevel",
    defaultValue = "" + DEFAULT_INDENTATION_LEVEL)
  public int indentationLevel = DEFAULT_INDENTATION_LEVEL;

  private int expectedLevel;
  private boolean isBlockAlreadyReported;
  private int lastCheckedLine;

  @Override
  public void init() {
    subscribeTo(BLOCK_TYPES);
    subscribeTo(CHECKED_TYPES);
  }

  @Override
  public void visitFile(AstNode node) {
    expectedLevel = 0;
    isBlockAlreadyReported = false;
    lastCheckedLine = 0;
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(BLOCK_TYPES)) {
      expectedLevel += indentationLevel;
      isBlockAlreadyReported = false;
    } else if (node.getToken().getColumn() != expectedLevel && !isExcluded(node)) {
      getContext().createLineViolation(this, "Make this line start at column " + (expectedLevel + 1) + ".", node);
      isBlockAlreadyReported = true;
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(BLOCK_TYPES)) {
      expectedLevel -= indentationLevel;
      isBlockAlreadyReported = false;
    }

    Token lastToken = getLastToken(node);
    lastCheckedLine = lastToken.getLine();
  }

  private boolean isExcluded(AstNode node) {
    return isBlockAlreadyReported || !isLineFirstStatement(node) || isInAnnonymousClass(node);
  }

  private boolean isLineFirstStatement(AstNode node) {
    return lastCheckedLine != node.getTokenLine();
  }

  private static boolean isInAnnonymousClass(AstNode node) {
    return node.hasAncestor(JavaGrammar.CLASS_CREATOR_REST);
  }

  private static Token getLastToken(AstNode node) {
    AstNode lastNodeWithTokens = node;

    while (!lastNodeWithTokens.hasToken()) {
      lastNodeWithTokens = lastNodeWithTokens.getPreviousAstNode();
    }

    return lastNodeWithTokens.getLastToken();
  }

}
