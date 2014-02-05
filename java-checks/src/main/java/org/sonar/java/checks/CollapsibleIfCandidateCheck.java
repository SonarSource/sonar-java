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
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import javax.annotation.Nullable;

@Rule(
  key = "S1066",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CollapsibleIfCandidateCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.IF_STATEMENT);
  }

  @Override
  public void visitNode(AstNode node) {
    if (!hasElseClause(node)) {
      AstNode enclosingIfStatement = getEnclosingIfStatement(node);
      if (enclosingIfStatement != null && !hasElseClause(enclosingIfStatement) && hasSingleTrueStatement(enclosingIfStatement)) {
        getContext().createLineViolation(this, "Merge this if statement with the enclosing one.", node);
      }
    }
  }

  private static boolean hasElseClause(AstNode node) {
    return node.hasDirectChildren(JavaKeyword.ELSE);
  }

  @Nullable
  private static AstNode getEnclosingIfStatement(AstNode node) {
    AstNode grandParent = node.getParent().getParent();
    if (grandParent.is(JavaGrammar.IF_STATEMENT)) {
      return grandParent;
    } else if (!grandParent.is(JavaGrammar.BLOCK_STATEMENT)) {
      return null;
    }

    AstNode statement = grandParent.getFirstAncestor(JavaGrammar.BLOCK, JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP).getParent();
    if (!statement.is(JavaGrammar.STATEMENT)) {
      return null;
    }

    AstNode enclosingStatement = statement.getParent();
    return enclosingStatement.is(JavaGrammar.IF_STATEMENT) ? enclosingStatement : null;
  }

  private static boolean hasSingleTrueStatement(AstNode node) {
    AstNode statement = node.getFirstChild(JavaGrammar.STATEMENT);

    return statement.hasDirectChildren(JavaGrammar.BLOCK) ?
        statement.getFirstChild(JavaGrammar.BLOCK).getFirstChild(JavaGrammar.BLOCK_STATEMENTS).getChildren().size() == 1 :
        true;
  }

}
