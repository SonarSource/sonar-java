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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.ast.AstSelect;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

@Rule(
  key = "S1185",
  priority = Priority.MINOR,
  tags={"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class MethodOnlyCallsSuperCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.VOID_METHOD_DECLARATOR_REST);
    subscribeTo(JavaGrammar.METHOD_DECLARATOR_REST);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode singleBlockStatement = getSingleBlockStatement(node);
    if (singleBlockStatement != null && isSuperOrReturnOfSuperReference(singleBlockStatement)) {
      String methodName = getMethodName(node);
      List<String> parameters = getParameters(node);

      if (isUselessSuperCall(singleBlockStatement, methodName, parameters) && !hasAnnotationDifferentFromOverride(node)) {
        getContext().createLineViolation(this, "Remove this method to simply inherit it.", node);
      }
    }
  }

  private static AstNode getSingleBlockStatement(AstNode node) {
    AstNode methodBody = node.getFirstChild(JavaGrammar.METHOD_BODY);
    if (methodBody == null) {
      return null;
    }

    AstNode blockStatements = methodBody.getFirstChild(JavaGrammar.BLOCK).getFirstChild(JavaGrammar.BLOCK_STATEMENTS);
    return blockStatements.getNumberOfChildren() == 1 ?
      blockStatements.getFirstChild() :
      null;
  }

  private static boolean isSuperOrReturnOfSuperReference(AstNode node) {
    return isSuperReference(node) ||
      isReturnOfSuperReference(node);
  }

  private static boolean isSuperReference(AstNode node) {
    return "super".equals(node.getTokenOriginalValue());
  }

  private static boolean isReturnOfSuperReference(AstNode node) {
    return "return".equals(node.getTokenOriginalValue()) && hasSuperReferenceReturnedExpression(node);
  }

  private static boolean hasSuperReferenceReturnedExpression(AstNode node) {
    AstNode returnStatement = node.getFirstDescendant(JavaGrammar.RETURN_STATEMENT);
    AstNode expression = returnStatement.getFirstChild(JavaGrammar.EXPRESSION);

    return expression != null &&
      isSuperReference(expression);
  }

  private static String getMethodName(AstNode node) {
    return node.getParent().getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue();
  }

  private static List<String> getParameters(AstNode node) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();

    for (AstNode parameter : node.getFirstChild(JavaGrammar.FORMAL_PARAMETERS).getDescendants(JavaGrammar.VARIABLE_DECLARATOR_ID)) {
      builder.add(parameter.getTokenOriginalValue());
    }

    return builder.build();
  }

  private static boolean isUselessSuperCall(AstNode node, String methodName, List<String> parameters) {
    StringBuilder sb = new StringBuilder();
    for (Token token : node.getTokens()) {
      sb.append(token.getOriginalValue());
    }

    String actual = sb.toString();
    String expected = "super." + methodName + "(" + Joiner.on(',').join(parameters) + ");";

    return actual.equals(expected) ||
      actual.equals("return" + expected);
  }

  private static boolean hasAnnotationDifferentFromOverride(AstNode node) {
    AstSelect query = node.select()
      .firstAncestor(JavaGrammar.CLASS_BODY_DECLARATION)
      .children(JavaGrammar.MODIFIER)
      .children(JavaGrammar.ANNOTATION)
      .children(JavaGrammar.QUALIFIED_IDENTIFIER);

    for (AstNode qualifiedIdentifier : query) {
      if (!isOverride(qualifiedIdentifier)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isOverride(AstNode node) {
    return node.getToken().equals(node.getLastToken()) &&
      "Override".equals(node.getTokenOriginalValue());
  }

}
