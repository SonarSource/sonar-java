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
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1132",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class StringLiteralInsideEqualsCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.SELECTOR);
    subscribeTo(JavaGrammar.PRIMARY);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isEquals(node)) {
      String literal = getStringLiteralArgument(node);
      if (literal != null) {
        getContext().createLineViolation(this, "Move the " + literal + " string literal on the left side of this string comparison.", node);
      }
    }
  }

  private static boolean isEquals(AstNode node) {
    return isSelectorEquals(node) || isQualifiedIdentifierEquals(node);
  }

  private static boolean isSelectorEquals(AstNode node) {
    AstNode identifier = node.getFirstChild(JavaTokenType.IDENTIFIER);
    return identifier != null && isEqualsMethod(identifier.getTokenOriginalValue());
  }

  private static boolean isQualifiedIdentifierEquals(AstNode node) {
    AstNode qualifiedIdentifier = node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
    return qualifiedIdentifier != null && isEqualsMethod(qualifiedIdentifier.getLastToken().getOriginalValue());
  }

  private static boolean isEqualsMethod(String s) {
    return "equals".equals(s) ||
      "equalsIgnoreCase".equals(s);
  }

  private static String getStringLiteralArgument(AstNode node) {
    AstNode arguments = getArgumentsNode(node);
    if (arguments == null || arguments.hasDirectChildren(JavaPunctuator.COMMA)) {
      return null;
    }

    AstNode expression = arguments.getFirstChild(JavaGrammar.EXPRESSION);

    return expression != null &&
      expression.getToken().getOriginalValue().startsWith("\"") &&
      expression.getToken().equals(expression.getLastToken()) ?
        expression.getTokenOriginalValue() : null;
  }

  private static AstNode getArgumentsNode(AstNode node) {
    AstNode result;

    if (node.is(JavaGrammar.PRIMARY)) {
      AstNode identifierSuffix = node.getFirstChild(JavaGrammar.IDENTIFIER_SUFFIX);
      result = identifierSuffix == null ? null : identifierSuffix.getFirstChild(JavaGrammar.ARGUMENTS);
    } else {
      result = node.getFirstChild(JavaGrammar.ARGUMENTS);
    }

    return result;
  }

}
