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

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "ClassVariableVisibilityCheck",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ClassVariableVisibilityCheck extends SquidCheck<LexerlessGrammar> {

  private static final String DEFAULT_AUTHORIZED_VISIBILITY = "private";

  @RuleProperty(
    key = "authorizedVisibility",
    defaultValue = "" + DEFAULT_AUTHORIZED_VISIBILITY)
  public String authorizedVisibility = DEFAULT_AUTHORIZED_VISIBILITY;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.FIELD_DECLARATION);

    Preconditions.checkArgument(
        "private".equals(authorizedVisibility) ||
          "package".equals(authorizedVisibility) ||
          "protected".equals(authorizedVisibility),
        "Unexpected authorized visibility '" + authorizedVisibility + "', expected one of: 'private', 'package' or 'protected'.");
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode classBodyDeclaration = node.getFirstAncestor(JavaGrammar.CLASS_BODY_DECLARATION);

    if (!isConstant(classBodyDeclaration) && !hasAllowedVisibility(classBodyDeclaration)) {
      getContext().createLineViolation(this, "Make this class member " + authorizedVisibility + " visible.", node);
    }
  }

  private boolean hasAllowedVisibility(AstNode node) {
    boolean result;

    if ("private".equals(authorizedVisibility)) {
      result = isPrivate(node);
    } else if ("package".equals(authorizedVisibility)) {
      result = isPrivate(node) || isPackage(node);
    } else {
      result = isPrivate(node) || isPackage(node) || isProtected(node);
    }

    return result;
  }

  private static boolean isConstant(AstNode node) {
    return hasModifier(node, JavaKeyword.STATIC) &&
      hasModifier(node, JavaKeyword.FINAL);
  }

  private static boolean isPrivate(AstNode node) {
    return hasModifier(node, JavaKeyword.PRIVATE);
  }

  private static boolean isProtected(AstNode node) {
    return hasModifier(node, JavaKeyword.PROTECTED);
  }

  private static boolean isPublic(AstNode node) {
    return hasModifier(node, JavaKeyword.PUBLIC);
  }

  private static boolean isPackage(AstNode node) {
    return !isProtected(node) &&
      !isPublic(node) &&
      !isPrivate(node);
  }

  private static boolean hasModifier(AstNode node, JavaKeyword modifier) {
    return node.select()
        .children(JavaGrammar.MODIFIER)
        .children(modifier)
        .isNotEmpty();
  }

}
