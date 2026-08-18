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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
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
    Long value = LiteralUtils.longLiteralValue(possibleConstant);
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
  private static Long extractMask(BinaryExpressionTree bitwiseOp) {
    Long leftValue = LiteralUtils.longLiteralValue(bitwiseOp.leftOperand());
    if (leftValue != null) {
      return leftValue;
    }
    return LiteralUtils.longLiteralValue(bitwiseOp.rightOperand());
  }

  private static boolean isIntOperation(BinaryExpressionTree bitwiseOp, ExpressionTree comparisonValue) {
    Type type = bitwiseOp.symbolType();
    if (type.is("int")) {
      return true;
    }
    if (type.is("long")) {
      return false;
    }
    // Type is unknown (no-semantic mode): assume int if no long literals are involved
    return !hasLongLiteral(bitwiseOp) && !comparisonValue.is(Kind.LONG_LITERAL);
  }

  private static boolean hasLongLiteral(BinaryExpressionTree bitwiseOp) {
    return bitwiseOp.leftOperand().is(Kind.LONG_LITERAL) || bitwiseOp.rightOperand().is(Kind.LONG_LITERAL);
  }

  private static boolean isIncompatible(Kind bitwiseKind, long mask, long value) {
    if (bitwiseKind == Kind.AND) {
      return (value & mask) != value;
    }
    // OR
    return (value | mask) != value;
  }
}
