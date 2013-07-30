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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1157",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CaseInsensitiveComparisonCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.UNARY_EXPRESSION);
    subscribeTo(JavaGrammar.PRIMARY);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isIllegalComparison(node)) {
      getContext().createLineViolation(this, "Replace this equals() and toUpperCase()/toLowerCase() by equalsIgnoreCase().", node);
    }
  }

  private static boolean isIllegalComparison(AstNode node) {
    return isToUpperCaseOrToLowerCaseFollowedByEquals(node) ||
      isEqualsFollowedByToUpperCaseOrToLowerCase(node);
  }

  private static boolean isToUpperCaseOrToLowerCaseFollowedByEquals(AstNode node) {
    return node.is(JavaGrammar.UNARY_EXPRESSION) &&
      isCallToToUpperCaseOrToLowerCase(node.getFirstChild(JavaGrammar.PRIMARY)) &&
      isSelectorCallToEquals(node);
  }

  private static boolean isEqualsFollowedByToUpperCaseOrToLowerCase(AstNode node) {
    return node.is(JavaGrammar.PRIMARY) &&
      isPrimaryCallToEquals(node);
  }

  private static boolean isCallToToUpperCaseOrToLowerCase(AstNode node) {
    return node != null &&
      hasArgumentsSuffix(node) &&
      hasToUpperCaseOrToLowerCaseQualifiedIdentifier(node);
  }

  private static boolean hasArgumentsSuffix(AstNode node) {
    AstNode identifierSuffix = node.getFirstChild(JavaGrammar.IDENTIFIER_SUFFIX);
    return identifierSuffix != null &&
      identifierSuffix.hasDirectChildren(JavaGrammar.ARGUMENTS);
  }

  private static boolean hasToUpperCaseOrToLowerCaseQualifiedIdentifier(AstNode node) {
    AstNode qualifiedIdentifier = node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
    if (qualifiedIdentifier == null) {
      return false;
    }

    String lastIdentifierValue = qualifiedIdentifier.getLastChild().getTokenOriginalValue();
    return "toUpperCase".equals(lastIdentifierValue) ||
      "toLowerCase".equals(lastIdentifierValue);
  }

  private static boolean isSelectorCallToEquals(AstNode node) {
    AstNode selector = node.getFirstChild(JavaGrammar.SELECTOR);
    if (selector == null) {
      return false;
    }

    AstNode identifier = selector.getFirstChild(JavaTokenType.IDENTIFIER);
    return "equals".equals(identifier.getTokenOriginalValue()) &&
      selector.hasDirectChildren(JavaGrammar.ARGUMENTS) &&
      !selector.getFirstChild(JavaGrammar.ARGUMENTS).hasDirectChildren(JavaPunctuator.COMMA);
  }

  private static boolean isPrimaryCallToEquals(AstNode node) {
    AstNode qualifiedIdentifier = node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
    if (qualifiedIdentifier == null) {
      return false;
    }

    String lastIdentifierValue = qualifiedIdentifier.getLastChild().getTokenOriginalValue();
    if (!"equals".equals(lastIdentifierValue)) {
      return false;
    }

    AstNode identifierSuffix = node.getFirstChild(JavaGrammar.IDENTIFIER_SUFFIX);
    if (identifierSuffix == null) {
      return false;
    }

    AstNode arguments = identifierSuffix.getFirstChild(JavaGrammar.ARGUMENTS);

    return arguments != null &&
      !arguments.hasDirectChildren(JavaPunctuator.COMMA) &&
      arguments.getFirstChild(JavaGrammar.EXPRESSION).hasDirectChildren(JavaGrammar.PRIMARY) &&
      isCallToToUpperCaseOrToLowerCase(arguments.getFirstChild(JavaGrammar.EXPRESSION).getFirstChild(JavaGrammar.PRIMARY));
  }

}
