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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1155",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CollectionIsEmptyCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.EQUALITY_EXPRESSION);
    subscribeTo(JavaGrammar.RELATIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isEmptyComparison(node) && hasSizeCallingOperand(node)) {
      getContext().createLineViolation(this, "Use isEmpty() to check whether the collection is empty or not.", node);
    }
  }

  private static boolean isEmptyComparison(AstNode node) {
    return isEqualityExpressionWithZeroLiteral(node) ||
      isRelationalExpressionWithZeroOrOne(node);
  }

  private static boolean isRelationalExpressionWithZeroOrOne(AstNode node) {
    if (!node.is(JavaGrammar.RELATIONAL_EXPRESSION)) {
      return false;
    }

    AstNode operand1 = node.getChild(0);
    AstNode operator = node.getChild(1);
    AstNode operand2 = node.getChild(2);

    return isBadRelation(operand1, operator.getType(), operand2);
  }

  private static boolean isBadRelation(AstNode operand1, AstNodeType operatorType, AstNode operand2) {
    boolean result;

    if (operatorType == JavaPunctuator.GE || operatorType == JavaPunctuator.LT) {
      result = isZero(operand1) || isOne(operand2);
    } else if (operatorType == JavaPunctuator.GT || operatorType == JavaPunctuator.LE) {
      result = isOne(operand1) || isZero(operand2);
    } else {
      result = false;
    }

    return result;
  }

  private static boolean isEqualityExpressionWithZeroLiteral(AstNode node) {
    return node.is(JavaGrammar.EQUALITY_EXPRESSION) &&
      hasZeroLiteral(node);
  }

  private static boolean hasZeroLiteral(AstNode node) {
    for (AstNode child : node.getChildren()) {
      if (child.hasDescendant(JavaTokenType.INTEGER_LITERAL) && isZero(child)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isZero(AstNode node) {
    return node.getToken().equals(node.getLastToken()) &&
      "0".equals(node.getTokenOriginalValue());
  }

  private static boolean isOne(AstNode node) {
    return node.getToken().equals(node.getLastToken()) &&
      "1".equals(node.getTokenOriginalValue());
  }

  private static boolean hasSizeCallingOperand(AstNode node) {
    AstNode operand1 = node.getChild(0);
    AstNode operand2 = node.getChild(2);

    return isSizeCall(operand1) ||
      isSizeCall(operand2);
  }

  private static boolean isSizeCall(AstNode node) {
    if (!node.is(JavaGrammar.PRIMARY)) {
      return false;
    }

    AstNode identifierSuffix = node.getFirstChild(JavaGrammar.IDENTIFIER_SUFFIX);
    if (identifierSuffix == null) {
      return false;
    }

    AstNode arguments = identifierSuffix.getFirstChild(JavaGrammar.ARGUMENTS);
    if (arguments == null || arguments.getNumberOfChildren() != 2) {
      return false;
    }

    return isSizeQualifiedIdentifier(node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER));
  }

  private static boolean isSizeQualifiedIdentifier(AstNode node) {
    return node != null &&
      "size".equals(node.getLastChild().getTokenOriginalValue());
  }

}
