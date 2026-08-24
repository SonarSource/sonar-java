/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S9358")
public class TernaryOperatorSameOperationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    var conditional = (org.sonar.plugins.java.api.tree.ConditionalExpressionTree) tree;
    var trueExpr = conditional.trueExpression();
    var falseExpr = conditional.falseExpression();

    if (hasSameOperationStructure(trueExpr, falseExpr)) {
      reportIssue(conditional, "Move the conditional expression inside this operation.");
    }
  }

  private static boolean hasSameOperationStructure(ExpressionTree left, ExpressionTree right) {
    if (left == null || right == null) {
      return false;
    }

    if (left.is(Tree.Kind.METHOD_INVOCATION) && right.is(Tree.Kind.METHOD_INVOCATION)) {
      return sameMethodInvocation((MethodInvocationTree) left, (MethodInvocationTree) right);
    }
    if (left.is(Tree.Kind.NEW_CLASS) && right.is(Tree.Kind.NEW_CLASS)) {
      return sameNewClass((NewClassTree) left, (NewClassTree) right);
    }
    if (left.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION) && right.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
      return sameArrayAccess((ArrayAccessExpressionTree) left, (ArrayAccessExpressionTree) right);
    }
    return false;
  }

  private static boolean sameMethodInvocation(MethodInvocationTree left, MethodInvocationTree right) {
    if (!sameMethodSelect(left.methodSelect(), right.methodSelect())) {
      return false;
    }

    var leftArgs = (List<ExpressionTree>) left.arguments();
    var rightArgs = (List<ExpressionTree>) right.arguments();
    if (leftArgs.size() != rightArgs.size()) {
      return false;
    }

    for (int i = 0; i < leftArgs.size(); i++) {
      if (sameExpression(leftArgs.get(i), rightArgs.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean sameMethodSelect(ExpressionTree left, ExpressionTree right) {
    if (!left.is(right.kind())) {
      return false;
    }
    if (left.is(Tree.Kind.MEMBER_SELECT)) {
      var leftMember = (MemberSelectExpressionTree) left;
      var rightMember = (MemberSelectExpressionTree) right;
      return sameMethodSelect(leftMember.expression(), rightMember.expression())
          && sameIdentifier(leftMember.identifier(), rightMember.identifier());
    }
    if (left.is(Tree.Kind.IDENTIFIER)) {
      return sameIdentifier((IdentifierTree) left, (IdentifierTree) right);
    }
    return left.toString().equals(right.toString());
  }

  private static boolean sameIdentifier(IdentifierTree left, IdentifierTree right) {
    return left.name().equals(right.name());
  }

  private static boolean sameNewClass(NewClassTree left, NewClassTree right) {
    if (!sameTree(left.identifier(), right.identifier())) {
      return false;
    }

    var leftTypeArgs = left.typeArguments();
    var rightTypeArgs = right.typeArguments();
    if ((leftTypeArgs == null) != (rightTypeArgs == null)) {
      return false;
    }
    if (leftTypeArgs != null && !sameTypeArguments(leftTypeArgs, rightTypeArgs)) {
      return false;
    }

    var leftArgs = (List<ExpressionTree>) left.arguments();
    var rightArgs = (List<ExpressionTree>) right.arguments();
    if (leftArgs.size() != rightArgs.size()) {
      return false;
    }

    for (int i = 0; i < leftArgs.size(); i++) {
      if (sameExpression(leftArgs.get(i), rightArgs.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean sameArrayAccess(ArrayAccessExpressionTree left, ArrayAccessExpressionTree right) {
    if (!sameExpression(left.expression(), right.expression())) {
      return false;
    }
    var leftIndex = getArrayIndex(left);
    var rightIndex = getArrayIndex(right);
    if (leftIndex == null || rightIndex == null) {
      return false;
    }
    return !sameExpression(leftIndex, rightIndex);
  }

  private static ExpressionTree getArrayIndex(ArrayAccessExpressionTree arrayAccess) {
    var dimension = arrayAccess.dimension();
    if (dimension != null && dimension.expression() != null) {
      return dimension.expression();
    }
    return null;
  }

  private static boolean sameExpression(ExpressionTree left, ExpressionTree right) {
    if (left == null || right == null) {
      return false;
    }
    if (!left.is(right.kind())) {
      return false;
    }
    return left.toString().equals(right.toString());
  }

  private static boolean sameTree(Tree left, Tree right) {
    if (left == null || right == null) {
      return false;
    }
    if (!left.is(right.kind())) {
      return false;
    }
    return left.toString().equals(right.toString());
  }

  private static boolean sameTypeArguments(TypeArguments left, TypeArguments right) {
    if (left.size() != right.size()) {
      return false;
    }
    for (int i = 0; i < left.size(); i++) {
      if (!left.get(i).toString().equals(right.get(i).toString())) {
        return false;
      }
    }
    return true;
  }
}
