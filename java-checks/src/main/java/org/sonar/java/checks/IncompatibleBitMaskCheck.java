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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S7438")
public class IncompatibleBitMaskCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return List.of(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree comparison = (BinaryExpressionTree) tree;
    ExpressionTree left = comparison.leftOperand();
    ExpressionTree right = comparison.rightOperand();
    check(left, right, comparison);
    check(right, left, comparison);
  }

  private void check(ExpressionTree possibleBitwiseOp, ExpressionTree possibleConstant, BinaryExpressionTree comparison) {
    ExpressionTree unwrapped = ExpressionUtils.skipParentheses(possibleBitwiseOp);
    if (!unwrapped.is(Kind.AND, Kind.OR)) {
      return;
    }
    BinaryExpressionTree bitwiseOp = (BinaryExpressionTree) unwrapped;
    Long mask = extractMask(bitwiseOp);
    Long value = signExtendedLongValue(possibleConstant);
    if (mask == null || value == null) {
      return;
    }
    if (isIntOperation(bitwiseOp, possibleConstant)) {
      mask = (long) mask.intValue();
      value = (long) value.intValue();
    }
    if (isIncompatible(unwrapped.kind(), mask, value)) {
      String message = comparison.is(Kind.EQUAL_TO)
        ? "This comparison is always false."
        : "This comparison is always true.";
      reportIssue(comparison.operatorToken(), message);
    }
  }

  @Nullable
  private static Long signExtendedLongValue(ExpressionTree operand) {
    Long value = LiteralUtils.longLiteralValue(operand);
    if (value != null && isIntLiteral(operand)) {
      value = (long) value.intValue();
    }
    return value;
  }

  @Nullable
  private static Long extractMask(BinaryExpressionTree bitwiseOp) {
    Long leftValue = maskOperandValue(bitwiseOp.leftOperand());
    if (leftValue != null) {
      return leftValue;
    }
    return maskOperandValue(bitwiseOp.rightOperand());
  }

  @Nullable
  private static Long maskOperandValue(ExpressionTree operand) {
    Long value = LiteralUtils.longLiteralValue(operand);
    if (value != null && isIntLiteral(operand)) {
      value = (long) value.intValue();
    }
    return value;
  }

  private static boolean isIntLiteral(ExpressionTree tree) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(tree);
    if (expr.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      expr = ((UnaryExpressionTree) expr).expression();
    }
    return expr.is(Kind.INT_LITERAL);
  }

  private static boolean isIntOperation(BinaryExpressionTree bitwiseOp, ExpressionTree comparisonValue) {
    String typeName = bitwiseOp.symbolType().fullyQualifiedName();
    if ("long".equals(typeName)) {
      return false;
    }
    if ("int".equals(typeName)) {
      return true;
    }
    // No semantic information: use heuristic based on literal kinds
    return !hasLongLiteral(bitwiseOp.leftOperand())
      && !hasLongLiteral(bitwiseOp.rightOperand())
      && !hasLongLiteral(comparisonValue);
  }

  private static boolean hasLongLiteral(ExpressionTree tree) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(tree);
    if (expr.is(Kind.UNARY_MINUS, Kind.UNARY_PLUS)) {
      expr = ((UnaryExpressionTree) expr).expression();
    }
    return expr.is(Kind.LONG_LITERAL);
  }

  private static boolean isIncompatible(Kind bitwiseKind, long mask, long value) {
    if (bitwiseKind == Kind.AND) {
      return (value & mask) != value;
    }
    // OR
    return (value | mask) != value;
  }
}
