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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1170",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ConstantsShouldBeStaticFinalCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.FIELD_DECLARATION);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode classMemberDeclaration = node.getFirstAncestor(JavaGrammar.CLASS_BODY_DECLARATION);

    if (isFinal(classMemberDeclaration) && !isStatic(classMemberDeclaration)) {
      for (AstNode variableDeclarator : node.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS).getChildren(JavaGrammar.VARIABLE_DECLARATOR)) {
        if (!isObjectInInnerClass(classMemberDeclaration) && hasConstantInitializer(variableDeclarator)) {
          getContext().createLineViolation(this, "Make this final field static too.", variableDeclarator);
        }
      }
    }
  }

  private boolean isObjectInInnerClass(AstNode classMemberDeclaration) {
    AstNode innerClassDeclaration = classMemberDeclaration.getFirstAncestor(JavaGrammar.CLASS_DECLARATION);
    if (innerClassDeclaration != null) {
      AstNode outerClassDeclaration = innerClassDeclaration.getFirstAncestor(JavaGrammar.CLASS_DECLARATION);
      if (outerClassDeclaration != null) {
        AstNode classType = classMemberDeclaration.getFirstDescendant(JavaGrammar.CLASS_TYPE);
        return classType != null && !isClassTypeString(classType);
      }
    }
    return false;
  }

  private boolean isClassTypeString(AstNode classType) {
    return "String".equals(classType.getTokenValue()) && classType.getNextSibling() == null;
  }

  private static boolean isFinal(AstNode node) {
    return node.select()
      .children(JavaGrammar.MODIFIER)
      .children(JavaKeyword.FINAL)
      .isNotEmpty();
  }

  private static boolean isStatic(AstNode node) {
    return node.select()
      .children(JavaGrammar.MODIFIER)
      .children(JavaKeyword.STATIC)
      .isNotEmpty();
  }

  private static boolean hasConstantInitializer(AstNode node) {
    AstNode variableInitializer = node.getFirstChild(JavaGrammar.VARIABLE_INITIALIZER);
    return variableInitializer != null &&
      !variableInitializer.hasDescendant(JavaPunctuator.LPAR, JavaPunctuator.LBRK);
  }

}
