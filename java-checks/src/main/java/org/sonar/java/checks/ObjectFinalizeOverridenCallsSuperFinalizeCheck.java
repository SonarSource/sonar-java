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
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.ast.AstSelect;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
    key = "ObjectFinalizeOverridenCallsSuperFinalizeCheck",
    priority = Priority.BLOCKER,
    tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
public class ObjectFinalizeOverridenCallsSuperFinalizeCheck extends SquidCheck<LexerlessGrammar> {

  private AstNode lastSuperFinalizeStatement;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.MEMBER_DECL);
    subscribeTo(JavaGrammar.PRIMARY);
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.hasDirectChildren(JavaGrammar.VOID_METHOD_DECLARATOR_REST)) {

      if (isObjectFinalize(node)) {
        lastSuperFinalizeStatement = null;
      }
    } else if (isSuperFinalize(node)) {
      lastSuperFinalizeStatement = node.getFirstAncestor(JavaGrammar.STATEMENT);
    }
  }

  private static boolean isSuperFinalize(AstNode node) {
    return node.is(JavaGrammar.PRIMARY) &&
        node.hasDirectChildren(JavaKeyword.SUPER) &&
        isFinalizeCallSuffix(node.getFirstChild(JavaGrammar.SUPER_SUFFIX));
  }

  private static boolean isFinalizeCallSuffix(AstNode node) {
    AstNode identifier = node.getFirstChild(JavaTokenType.IDENTIFIER);
    return identifier != null &&
        "finalize".equals(identifier.getTokenOriginalValue()) &&
        node.hasDirectChildren(JavaGrammar.ARGUMENTS);
  }

  private boolean isObjectFinalize(AstNode node) {
    AstNode identifier = node.getFirstChild(JavaTokenType.IDENTIFIER);
    return "finalize".equals(identifier.getTokenValue()) &&
        !node.getFirstDescendant(JavaGrammar.FORMAL_PARAMETERS).hasDirectChildren(JavaGrammar.FORMAL_PARAMETER_DECLS);
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.hasDirectChildren(JavaGrammar.VOID_METHOD_DECLARATOR_REST) && isObjectFinalize(node)) {
      AstSelect methodBlockStatement = node.select()
          .children(JavaGrammar.VOID_METHOD_DECLARATOR_REST)
          .children(JavaGrammar.METHOD_BODY)
          .children(JavaGrammar.BLOCK)
          .children(JavaGrammar.BLOCK_STATEMENTS)
          .children(JavaGrammar.BLOCK_STATEMENT);

      if (lastSuperFinalizeStatement == null) {
        getContext().createLineViolation(this, "Add a call to super.finalize() at the end of this Object.finalize() implementation.", node.getFirstChild(JavaTokenType.IDENTIFIER));
      } else if (!lastSuperFinalizeStatement.equals(getLastEffectiveStatement(getLastBlockStatement(methodBlockStatement)))) {
        getContext().createLineViolation(this, "Move this super.finalize() call to the end of this Object.finalize() implementation.", lastSuperFinalizeStatement);
      }
    }
  }

  private static AstNode getLastBlockStatement(Iterable<AstNode> blockStatements) {
    AstNode result = null;

    for (AstNode blockStatement : blockStatements) {
      AstNode statement = blockStatement.getFirstChild(JavaGrammar.STATEMENT);
      if (statement != null) {
        result = statement;
      }
    }

    return result;
  }

  private static AstNode getLastEffectiveStatement(AstNode node) {
    AstNode tryStatement = node.getFirstChild(JavaGrammar.TRY_STATEMENT);
    if (tryStatement == null) {
      return node;
    }

    AstSelect query = tryStatement.select()
        .children(JavaGrammar.FINALLY_)
        .children(JavaGrammar.BLOCK)
        .children(JavaGrammar.BLOCK_STATEMENTS)
        .children(JavaGrammar.BLOCK_STATEMENT);

    return getLastBlockStatement(query);
  }

}
