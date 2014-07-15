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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S00108",
  priority = Priority.MAJOR,
  tags={"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class EmptyBlock_S00108_Check extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(
      JavaGrammar.BLOCK,
      JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUPS);
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUPS)) {
      if (!node.hasChildren()) {
        getContext().createLineViolation(this, "Either remove or fill this block of code.", node.getParent());
      }
    } else {
      if (node.getParent().isNot(JavaGrammar.METHOD_BODY) && !hasStatements(node) && !hasCommentInside(node)) {
        getContext().createLineViolation(this, "Either remove or fill this block of code.", node.getParent());
      }
    }
  }

  private static boolean hasStatements(AstNode node) {
    return node.getFirstChild(JavaGrammar.BLOCK_STATEMENTS).hasDirectChildren(JavaGrammar.BLOCK_STATEMENT);
  }

  private static boolean hasCommentInside(AstNode node) {
    return !node.getFirstChild(JavaPunctuator.RWING).getToken().getTrivia().isEmpty();
  }

}
