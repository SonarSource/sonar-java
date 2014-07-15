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
  key = "S1214",
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class InterfaceAsConstantContainerCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.INTERFACE_BODY);
  }

  @Override
  public void visitNode(AstNode node) {
    if (hasConstant(node)) {
      getContext().createLineViolation(this, "Move constants to a class or enum.", node);
    }
  }

  private static boolean hasConstant(AstNode node) {
    for (AstNode declaration : node.getChildren(JavaGrammar.INTERFACE_BODY_DECLARATION)) {
      if (isConstant(declaration)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isConstant(AstNode declaration) {
    AstNode memberDecl = declaration.getFirstChild(JavaGrammar.INTERFACE_MEMBER_DECL);
    if (memberDecl == null) {
      return false;
    }

    AstNode methodOrFieldDecl = declaration.getFirstChild(JavaGrammar.INTERFACE_MEMBER_DECL).getFirstChild(JavaGrammar.INTERFACE_METHOD_OR_FIELD_DECL);

    return methodOrFieldDecl != null &&
      methodOrFieldDecl.getFirstChild(JavaGrammar.INTERFACE_METHOD_OR_FIELD_REST).hasDirectChildren(JavaGrammar.CONSTANT_DECLARATORS_REST);
  }

}
