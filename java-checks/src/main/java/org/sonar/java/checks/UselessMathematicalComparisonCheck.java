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
import java.util.Map;
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

@Rule(key = "S2198")
public class UselessMathematicalComparisonCheck extends IssuableSubscriptionVisitor {

  private static final Map<Type.Primitives, long[]> TYPE_BOUNDS = Map.of(
    Type.Primitives.BYTE, new long[] {Byte.MIN_VALUE, Byte.MAX_VALUE},
    Type.Primitives.SHORT, new long[] {Short.MIN_VALUE, Short.MAX_VALUE},
    Type.Primitives.CHAR, new long[] {Character.MIN_VALUE, Character.MAX_VALUE},
    Type.Primitives.INT, new long[] {Integer.MIN_VALUE, Integer.MAX_VALUE}
  );

  @Override
  public List<Kind> nodesToVisit() {
    return List.of(
      Kind.GREATER_THAN,
      Kind.GREATER_THAN_OR_EQUAL_TO,
      Kind.LESS_THAN,
      Kind.LESS_THAN_OR_EQUAL_TO,
      Kind.EQUAL_TO,
      Kind.NOT_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpression = (BinaryExpressionTree) tree;
    ExpressionTree leftOperand = ExpressionUtils.skipParentheses(binaryExpression.leftOperand());
    ExpressionTree rightOperand = ExpressionUtils.skipParentheses(binaryExpression.rightOperand());

    // Try left=variable, right=constant
    Boolean result = evaluate(leftOperand, rightOperand, binaryExpression.kind(), false);
    if (result == null) {
      // Try left=constant, right=variable (reversed)
      result = evaluate(rightOperand, leftOperand, binaryExpression.kind(), true);
    }

    if (result != null) {
      reportIssue(binaryExpression, "Remove this comparison; it will always return " + result + ".");
    }
  }

  @Nullable
  private static Boolean evaluate(ExpressionTree variableCandidate, ExpressionTree constantCandidate, Kind operatorKind, boolean reversed) {
    long[] bounds = resolveTypeBounds(variableCandidate);
    if (bounds == null) {
      return null;
    }
    Long constantValue = resolveConstantValue(constantCandidate);
    if (constantValue == null) {
      return null;
    }
    long min = bounds[0];
    long max = bounds[1];
    Kind normalizedKind = reversed ? reverseOperator(operatorKind) : operatorKind;
    return evaluateComparison(normalizedKind, min, max, constantValue);
  }

  @Nullable
  private static long[] resolveTypeBounds(ExpressionTree expression) {
    Type type = expression.symbolType();
    for (Map.Entry<Type.Primitives, long[]> entry : TYPE_BOUNDS.entrySet()) {
      if (type.isPrimitive(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Nullable
  private static Long resolveConstantValue(ExpressionTree expression) {
    Long literal = LiteralUtils.longLiteralValue(expression);
    if (literal != null) {
      return literal;
    }
    Object constant = ExpressionUtils.resolveAsConstant(expression);
    if (constant instanceof Integer intVal) {
      return intVal.longValue();
    }
    if (constant instanceof Long longVal) {
      return longVal;
    }
    return null;
  }

  private static Kind reverseOperator(Kind kind) {
    return switch (kind) {
      case GREATER_THAN -> Kind.LESS_THAN;
      case GREATER_THAN_OR_EQUAL_TO -> Kind.LESS_THAN_OR_EQUAL_TO;
      case LESS_THAN -> Kind.GREATER_THAN;
      case LESS_THAN_OR_EQUAL_TO -> Kind.GREATER_THAN_OR_EQUAL_TO;
      default -> kind; // EQUAL_TO, NOT_EQUAL_TO are symmetric
    };
  }

  @Nullable
  private static Boolean evaluateComparison(Kind normalizedKind, long min, long max, long constant) {
    return switch (normalizedKind) {
      // var > constant
      case GREATER_THAN -> {
        if (constant >= max) yield Boolean.FALSE;
        else if (constant < min) yield Boolean.TRUE;
        else yield null;
      }
      // var >= constant
      case GREATER_THAN_OR_EQUAL_TO -> {
        if (constant > max) yield Boolean.FALSE;
        else if (constant <= min) yield Boolean.TRUE;
        else yield null;
      }
      // var < constant
      case LESS_THAN -> {
        if (constant <= min) yield Boolean.FALSE;
        else if (constant > max) yield Boolean.TRUE;
        else yield null;
      }
      // var <= constant
      case LESS_THAN_OR_EQUAL_TO -> {
        if (constant < min) yield Boolean.FALSE;
        else if (constant >= max) yield Boolean.TRUE;
        else yield null;
      }
      // var == constant
      case EQUAL_TO -> {
        if (constant < min || constant > max) yield Boolean.FALSE;
        else yield null;
      }
      // var != constant
      case NOT_EQUAL_TO -> {
        if (constant < min || constant > max) yield Boolean.TRUE;
        else yield null;
      }
      default -> null;
    };
  }
}
