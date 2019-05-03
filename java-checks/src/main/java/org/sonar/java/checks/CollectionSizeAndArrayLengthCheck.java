/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S3981")
public class CollectionSizeAndArrayLengthCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher COLLECTION_SIZE = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.util.Collection")).name("size").withoutParameter();
  private static final String COLLECTION_ISSUE_MSG = "The size of %s is always \">=0\", so update this test to use isEmpty().";
  private static final String ARRAY_ISSUE_MSG = "The length of %s is always \">=0\", so update this test to either \"==0\" or \">0\".";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
      Tree.Kind.GREATER_THAN,
      Tree.Kind.LESS_THAN,
      Tree.Kind.LESS_THAN_OR_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    BinaryExpressionTree bet = (BinaryExpressionTree) tree;
    ExpressionTree leftOperand = ExpressionUtils.skipParentheses(bet.leftOperand());
    ExpressionTree rightOperand = ExpressionUtils.skipParentheses(bet.rightOperand());
    boolean leftIsZero = isZero(leftOperand);
    boolean rightIsZero = isZero(rightOperand);
    if (!leftIsZero && !rightIsZero) {
      return;
    }
    ExpressionTree testedValue = leftIsZero ? rightOperand : leftOperand;
    if (testedValue.is(Tree.Kind.METHOD_INVOCATION)) {
      checkCollectionSize((MethodInvocationTree) testedValue, bet, leftIsZero);
    } else if (testedValue.is(Tree.Kind.MEMBER_SELECT)) {
      checkArrayLength((MemberSelectExpressionTree) testedValue, bet, leftIsZero);
    }
  }

  private static boolean isZero(ExpressionTree expr) {
    return expr.is(Tree.Kind.INT_LITERAL) && "0".equals(((LiteralTree) expr).value());
  }

  private void checkCollectionSize(MethodInvocationTree testedValue, BinaryExpressionTree bet, boolean leftIsZero) {
    if (!COLLECTION_SIZE.matches(testedValue)) {
      return;
    }
    reportIssue(bet, leftIsZero, COLLECTION_ISSUE_MSG, collectionName(testedValue.methodSelect()));
  }

  private static String collectionName(ExpressionTree sizeMethodSelect) {
    if (sizeMethodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree collectionAccess = ((MemberSelectExpressionTree) sizeMethodSelect).expression();
      if (collectionAccess.is(Tree.Kind.IDENTIFIER)) {
        return escape(((IdentifierTree) collectionAccess).name());
      }
    }
    return "a collection";
  }

  private static String escape(String s) {
    return "\"" + s + "\"";
  }

  private void reportIssue(BinaryExpressionTree bet, boolean leftIsZero, String message, String itemName) {
    if ((leftIsZero && bet.is(Tree.Kind.GREATER_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO))
      || (!leftIsZero && bet.is(Tree.Kind.LESS_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO))) {
      reportIssue(bet, String.format(message, itemName));
    }
  }

  private void checkArrayLength(MemberSelectExpressionTree testedValue, BinaryExpressionTree bet, boolean leftIsZero) {
    if (!"length".equals(testedValue.identifier().name())) {
      return;
    }
    ExpressionTree expression = testedValue.expression();
    if (!expression.symbolType().isArray()) {
      return;
    }
    reportIssue(bet, leftIsZero, ARRAY_ISSUE_MSG, arrayName(expression));
  }

  private static String arrayName(ExpressionTree expr) {
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      return escape(((IdentifierTree) expr).name());
    }
    if (expr.is(Tree.Kind.MEMBER_SELECT)) {
      return escape(((MemberSelectExpressionTree) expr).identifier().name());
    }
    return "an array";
  }

}
