/*
 * Sonar Java
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

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

@Rule(
  key = "ObjectFinalizeOverridenCallsSuperFinalizeCheck",
  priority = Priority.BLOCKER)
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
public class ObjectFinalizeOverridenCallsSuperFinalizeCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.MEMBER_DECL);
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.hasDirectChildren(JavaGrammar.VOID_METHOD_DECLARATOR_REST)) {
      AstNode identifier = node.getFirstChild(JavaTokenType.IDENTIFIER);

      if ("finalize".equals(identifier.getTokenValue()) && !hasSuperFinalizeAsLastStatement(node)) {
        getContext().createLineViolation(this, "Add a call to 'super.finalize()' at the end of this Object.finalize() implementation.", identifier);
      }
    }
  }

  private static boolean hasSuperFinalizeAsLastStatement(AstNode node) {
    List<AstNode> blockStatements = ImmutableList.copyOf(node.select()
        .children(JavaGrammar.VOID_METHOD_DECLARATOR_REST)
        .children(JavaGrammar.METHOD_BODY)
        .children(JavaGrammar.BLOCK)
        .children(JavaGrammar.BLOCK_STATEMENTS)
        .children(JavaGrammar.BLOCK_STATEMENT).iterator());

    return !blockStatements.isEmpty() &&
      isSuperFinalize(blockStatements.get(blockStatements.size() - 1));
  }

  private static boolean isSuperFinalize(AstNode node) {
    List<Token> tokens = node.getTokens();

    return tokens.size() == 6 &&
      "super".equals(tokens.get(0).getValue()) &&
      ".".equals(tokens.get(1).getValue()) &&
      "finalize".equals(tokens.get(2).getValue()) &&
      "(".equals(tokens.get(3).getValue()) &&
      ")".equals(tokens.get(4).getValue()) &&
      ";".equals(tokens.get(5).getValue());
  }

}
