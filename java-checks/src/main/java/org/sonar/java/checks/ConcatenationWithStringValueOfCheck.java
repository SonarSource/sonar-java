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
  key = "S1153",
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class ConcatenationWithStringValueOfCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.ADDITIVE_EXPRESSION);
  }

  @Override
  public void visitNode(AstNode node) {
    boolean seenStringLiteral = false;

    for (AstNode child : node.getChildren()) {
      if (!seenStringLiteral) {
        seenStringLiteral = isStringLiteral(child);
      } else if (isStringValueOfCall(child)) {
        getContext().createLineViolation(this, "Directly append the argument of String.valueOf().", child);
      }
    }
  }

  private static boolean isStringLiteral(AstNode node) {
    return node.getToken().equals(node.getLastToken()) &&
      node.getTokenValue().startsWith("\"");
  }

  private static boolean isStringValueOfCall(AstNode node) {
    AstNode identifierSuffix = node.getFirstChild(JavaGrammar.IDENTIFIER_SUFFIX);

    return node.is(JavaGrammar.PRIMARY) &&
      identifierSuffix != null &&
      hasSingleArgumentIdentifierSuffix(identifierSuffix) &&
      isStringValueOfQualifiedIdentifier(node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER));
  }

  private static boolean hasSingleArgumentIdentifierSuffix(AstNode node) {
    AstNode arguments = node.getFirstChild(JavaGrammar.ARGUMENTS);
    return arguments != null &&
      arguments.hasDirectChildren(JavaGrammar.EXPRESSION) &&
      !arguments.hasDirectChildren(JavaPunctuator.COMMA);
  }

  private static boolean isStringValueOfQualifiedIdentifier(AstNode node) {
    return node.getNumberOfChildren() == 3 &&
      "String".equals(node.getTokenOriginalValue()) &&
      "valueOf".equals(node.getLastChild().getTokenOriginalValue());
  }

}
