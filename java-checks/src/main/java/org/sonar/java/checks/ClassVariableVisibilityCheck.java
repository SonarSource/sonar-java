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
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "ClassVariableVisibilityCheck",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ClassVariableVisibilityCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.FIELD_DECLARATION);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode classBodyDeclaration = node.getFirstAncestor(JavaGrammar.CLASS_BODY_DECLARATION);

    if (isPublic(classBodyDeclaration) && !isConstant(classBodyDeclaration) && !isAnnotated(classBodyDeclaration)) {
      getContext().createLineViolation(this, "Make this class variable field non-public and provide accessors if needed.", node);
    }
  }

  private static boolean isConstant(AstNode node) {
    return hasModifier(node, JavaKeyword.STATIC) &&
      hasModifier(node, JavaKeyword.FINAL);
  }

  private static boolean isPublic(AstNode node) {
    return hasModifier(node, JavaKeyword.PUBLIC);
  }

  private static boolean hasModifier(AstNode node, AstNodeType modifier) {
    return node.select()
        .children(JavaGrammar.MODIFIER)
        .children(modifier)
        .isNotEmpty();
  }

  private static boolean isAnnotated(AstNode node) {
    return hasModifier(node, JavaGrammar.ANNOTATION);
  }

}
