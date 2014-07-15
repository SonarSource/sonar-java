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
  key = "S1141",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class NestedTryCatchCheck extends SquidCheck<LexerlessGrammar> {

  private int nestingLevel;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.BLOCK);
  }

  @Override
  public void visitFile(AstNode node) {
    nestingLevel = 0;
  }

  @Override
  public void visitNode(AstNode node) {
    if (isTryCatchBlock(node)) {
      nestingLevel++;

      if (nestingLevel > 1) {
        getContext().createLineViolation(this, "Extract this nested try block into a separate method.", node);
      }
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (isTryCatchBlock(node)) {
      nestingLevel--;
    }
  }

  private static boolean isTryCatchBlock(AstNode node) {
    return node.getParent().is(JavaGrammar.TRY_STATEMENT) &&
      node.getParent().hasDirectChildren(JavaGrammar.CATCH_CLAUSE);
  }

}
