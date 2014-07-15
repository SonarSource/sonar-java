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
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1174",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ObjectFinalizeOverridenNotPublicCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.MEMBER_DECL);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isFinalizeMethodMember(node) && isPublic(node)) {
      getContext().createLineViolation(this, "Make this finalize() method protected.", node.getFirstChild(JavaTokenType.IDENTIFIER));
    }
  }

  private static boolean isFinalizeMethodMember(AstNode node) {
    return node.hasDirectChildren(JavaGrammar.VOID_METHOD_DECLARATOR_REST) &&
      "finalize".equals(node.getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue());
  }

  private static boolean isPublic(AstNode node) {
    return node.select()
        .firstAncestor(JavaGrammar.CLASS_BODY_DECLARATION)
        .children(JavaGrammar.MODIFIER)
        .children(JavaKeyword.PUBLIC)
        .isNotEmpty();
  }

}
