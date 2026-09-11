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
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9390")
public class ObjectsEqualsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    ConditionalExpressionTree conditional = (ConditionalExpressionTree) tree;
    ExpressionTree condition = ExpressionUtils.skipParentheses(conditional.condition());
    ExpressionTree trueExpr = ExpressionUtils.skipParentheses(conditional.trueExpression());
    ExpressionTree falseExpr = ExpressionUtils.skipParentheses(conditional.falseExpression());

    if (matchesPattern(condition, trueExpr, falseExpr)) {
      reportIssue(conditional, "Replace this manual null check with \"Objects.equals()\".");
    }
  }

  private static boolean matchesPattern(ExpressionTree condition, ExpressionTree trueExpr, ExpressionTree falseExpr) {
    if (!isNotEqualToNull(condition)) {
      return false;
    }

    ExpressionTree conditionVar = getIdentifierFromNotEqualToNull(condition);
    if (conditionVar == null) {
      return false;
    }

    EqualsMethodCall equalsCall = extractEqualsCall(trueExpr);
    if (equalsCall == null) {
      return false;
    }

    if (!matchesReceiver(conditionVar, equalsCall.receiver)) {
      return false;
    }

    if (!isEqualToNull(falseExpr, equalsCall.argument)) {
      return false;
    }

    return true;
  }

  private static boolean matchesReceiver(ExpressionTree conditionVar, ExpressionTree receiver) {
    if (conditionVar.is(Tree.Kind.METHOD_INVOCATION) || receiver.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    return SyntacticEquivalence.areEquivalent(conditionVar, receiver);
  }

  private static boolean isNotEqualToNull(ExpressionTree tree) {
    if (!tree.is(Tree.Kind.NOT_EQUAL_TO)) {
      return false;
    }
    BinaryExpressionTree binary = (BinaryExpressionTree) tree;
    ExpressionTree left = ExpressionUtils.skipParentheses(binary.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(binary.rightOperand());
    return isNullLiteral(right) || isNullLiteral(left);
  }

  private static ExpressionTree getIdentifierFromNotEqualToNull(ExpressionTree tree) {
    if (!isNotEqualToNull(tree)) {
      return null;
    }
    BinaryExpressionTree binary = (BinaryExpressionTree) tree;
    ExpressionTree left = ExpressionUtils.skipParentheses(binary.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(binary.rightOperand());

    if (isNullLiteral(right)) {
      return left;
    }
    if (isNullLiteral(left)) {
      return right;
    }
    return null;
  }

  private static boolean isEqualToNull(ExpressionTree tree, ExpressionTree expectedArg) {
    if (!tree.is(Tree.Kind.EQUAL_TO)) {
      return false;
    }
    BinaryExpressionTree binary = (BinaryExpressionTree) tree;
    ExpressionTree left = ExpressionUtils.skipParentheses(binary.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(binary.rightOperand());

    if (isNullLiteral(right)) {
      return SyntacticEquivalence.areEquivalent(left, expectedArg);
    }
    if (isNullLiteral(left)) {
      return SyntacticEquivalence.areEquivalent(right, expectedArg);
    }
    return false;
  }

  private static EqualsMethodCall extractEqualsCall(ExpressionTree tree) {
    if (!tree.is(Tree.Kind.METHOD_INVOCATION)) {
      return null;
    }
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (!"equals".equals(mit.methodSymbol().name())) {
      return null;
    }
    List<? extends ExpressionTree> args = mit.arguments();
    if (args.size() != 1) {
      return null;
    }
    ExpressionTree receiver = ExpressionUtils.skipParentheses(mit.methodSelect());
    if (receiver.is(Tree.Kind.MEMBER_SELECT)) {
      receiver = ((MemberSelectExpressionTree) receiver).expression();
    }
    return new EqualsMethodCall(receiver, args.get(0));
  }

  private static boolean isNullLiteral(ExpressionTree tree) {
    return tree.is(Tree.Kind.NULL_LITERAL);
  }

  private static class EqualsMethodCall {
    final ExpressionTree receiver;
    final ExpressionTree argument;

    EqualsMethodCall(ExpressionTree receiver, ExpressionTree argument) {
      this.receiver = receiver;
      this.argument = argument;
    }
  }
}
