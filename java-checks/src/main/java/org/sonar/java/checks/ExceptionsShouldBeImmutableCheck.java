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
  key = "S1165",
  priority = Priority.MAJOR,
  tags={"error-handling"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ExceptionsShouldBeImmutableCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_DECLARATION);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isException(node)) {
      for (AstNode field : getFields(node)) {
        if (!isFinal(field)) {
          for (AstNode variableDeclarator : field.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS).getChildren(JavaGrammar.VARIABLE_DECLARATOR)) {
            getContext().createLineViolation(this, "Make this \"" + variableDeclarator.getTokenOriginalValue() + "\" field final.", field);
          }
        }
      }
    }
  }

  private static boolean isException(AstNode node) {
    String name = node.getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue();
    return name.endsWith("Exception") ||
      name.endsWith("Error");
  }

  private static Iterable<AstNode> getFields(AstNode node) {
    return node.select()
      .children(JavaGrammar.CLASS_BODY)
      .children(JavaGrammar.CLASS_BODY_DECLARATION)
      .children(JavaGrammar.MEMBER_DECL)
      .children(JavaGrammar.FIELD_DECLARATION);
  }

  private static boolean isFinal(AstNode node) {
    return node.select()
      .firstAncestor(JavaGrammar.CLASS_BODY_DECLARATION)
      .children(JavaGrammar.MODIFIER)
      .children(JavaKeyword.FINAL)
      .isNotEmpty();
  }

}
