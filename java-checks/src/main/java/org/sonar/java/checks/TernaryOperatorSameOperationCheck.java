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
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

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
    if (!sameMethodSelect(left.methodSelect(), right.methodSelect())) {
      return false;
    }
    if (!hasExactlyOneArgumentDifference(left.arguments(), right.arguments())) {
      return false;
    }
    return sameMethodSymbol(left.methodSymbol(), right.methodSymbol());
  }

  private static boolean sameMethodSymbol(Symbol.MethodSymbol left, Symbol.MethodSymbol right) {
    if (left.isUnknown() || right.isUnknown()) {
      return true;
    }
    return left.equals(right);
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
    if (!hasExactlyOneArgumentDifference(left.arguments(), right.arguments())) {
      return false;
    }
    if (!sameMethodSymbol(left.methodSymbol(), right.methodSymbol())) {
      return false;
    }
    if (left.classBody() != null || right.classBody() != null) {
      return false;
    }
    var leftEnclosing = left.enclosingExpression();
    var rightEnclosing = right.enclosingExpression();
    if (leftEnclosing == null && rightEnclosing == null) {
      return true;
    }
    if (leftEnclosing == null || rightEnclosing == null) {
      return false;
    }
    return sameTree(leftEnclosing, rightEnclosing);
  }

  private static boolean hasExactlyOneArgumentDifference(List<? extends ExpressionTree> leftArgs, List<? extends ExpressionTree> rightArgs) {
    if (leftArgs.size() != rightArgs.size()) {
      return false;
    }
    int differences = 0;
    for (int i = 0; i < leftArgs.size(); i++) {
      ExpressionTree leftArg = leftArgs.get(i);
      ExpressionTree rightArg = rightArgs.get(i);
      if (!sameTree(leftArg, rightArg)) {
        if (!areInterchangeableOperands(leftArg, rightArg)) {
          return false;
        }
        differences++;
      }
    }
    return differences == 1;
  }

  private static boolean sameArrayAccess(ArrayAccessExpressionTree left, ArrayAccessExpressionTree right) {
    ExpressionTree leftIndex = left.dimension().expression();
    ExpressionTree rightIndex = right.dimension().expression();
    return sameTree(left.expression(), right.expression())
      && !sameTree(leftIndex, rightIndex)
      && areInterchangeableOperands(leftIndex, rightIndex);
  }

  /**
   * The differing operands can be moved into a conditional expression only when doing so keeps the code readable
   * and does not alter semantics: both operands must be simple (identifiers, literals or field accesses)
   * and have the same type, so that the resulting conditional expression does not involve boxing, unboxing or
   * a different overload resolution.
   */
  private static boolean areInterchangeableOperands(ExpressionTree left, ExpressionTree right) {
    return isSimpleOperand(left) && isSimpleOperand(right) && sameType(left.symbolType(), right.symbolType());
  }

  private static boolean isSimpleOperand(ExpressionTree tree) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(tree);
    if (expression instanceof LiteralTree || expression.is(Tree.Kind.IDENTIFIER)) {
      return true;
    }
    if (expression.is(Tree.Kind.UNARY_MINUS)) {
      return ((UnaryExpressionTree) expression).expression() instanceof LiteralTree;
    }
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      return isSimpleOperand(((MemberSelectExpressionTree) expression).expression());
    }
    return false;
  }

  private static boolean sameType(Type left, Type right) {
    if (left.isUnknown() || right.isUnknown()) {
      return true;
    }
    return left.equals(right);
  }

  private static boolean sameTree(Tree left, Tree right) {
    return SyntacticEquivalence.areEquivalent(left, right);
  }
}
