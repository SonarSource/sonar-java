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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9358")
public class TernaryOperatorSameOperationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    var conditional = (ConditionalExpressionTree) tree;
    var trueExpr = ExpressionUtils.skipParentheses(conditional.trueExpression());
    var falseExpr = ExpressionUtils.skipParentheses(conditional.falseExpression());

    if (hasSameOperationStructure(trueExpr, falseExpr)) {
      reportIssue(conditional, "Move the conditional expression inside this operation.");
    }
  }

  private static boolean hasSameOperationStructure(ExpressionTree left, ExpressionTree right) {
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
    return sameMethodSelect(left.methodSelect(), right.methodSelect())
      && hasExactlyOneArgumentDifference(left.arguments(), right.arguments());
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
    return sameTree(left, right);
  }

  private static boolean sameIdentifier(IdentifierTree left, IdentifierTree right) {
    return left.name().equals(right.name());
  }

  private static boolean sameNewClass(NewClassTree left, NewClassTree right) {
    if (!sameTree(left.identifier(), right.identifier())) {
      return false;
    }
    return hasExactlyOneArgumentDifference(left.arguments(), right.arguments());
  }

  private static boolean hasExactlyOneArgumentDifference(List<? extends ExpressionTree> leftArgs, List<? extends ExpressionTree> rightArgs) {
    if (leftArgs.size() != rightArgs.size()) {
      return false;
    }
    int differences = 0;
    for (int i = 0; i < leftArgs.size(); i++) {
      if (!sameTree(leftArgs.get(i), rightArgs.get(i))) {
        differences++;
      }
    }
    return differences == 1;
  }

  private static boolean sameArrayAccess(ArrayAccessExpressionTree left, ArrayAccessExpressionTree right) {
    return sameTree(left.expression(), right.expression())
      && !sameTree(left.dimension().expression(), right.dimension().expression());
  }

  private static boolean sameTree(Tree left, Tree right) {
    if (!left.is(right.kind())) {
      return false;
    }
    return left.toString().equals(right.toString());
  }
}
