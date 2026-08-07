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

  /**
   * Only the integral types. A float or double variable can also hold +/-Infinity, so a constant outside the
   * finite range of the type does not make a relational comparison constant: "f > 1e40" is true when f is
   * +Infinity, and "d > Double.MAX_VALUE" is a legitimate way to test for +Infinity. Equality would be
   * decidable, but every equality test on a float or double is already reported by S1244.
   */
  private static final Map<Type.Primitives, long[]> INTEGRAL_BOUNDS = Map.of(
    Type.Primitives.BYTE, new long[] {Byte.MIN_VALUE, Byte.MAX_VALUE},
    Type.Primitives.SHORT, new long[] {Short.MIN_VALUE, Short.MAX_VALUE},
    Type.Primitives.CHAR, new long[] {Character.MIN_VALUE, Character.MAX_VALUE},
    Type.Primitives.INT, new long[] {Integer.MIN_VALUE, Integer.MAX_VALUE},
    Type.Primitives.LONG, new long[] {Long.MIN_VALUE, Long.MAX_VALUE}
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
    Number constantValue = resolveConstantValue(constantCandidate);
    if (constantValue == null) {
      return null;
    }
    long[] bounds = resolveTypeBounds(variableCandidate);
    if (bounds == null) {
      return null;
    }
    Kind normalizedKind = reversed ? reverseOperator(operatorKind) : operatorKind;
    if (isFloatingPoint(constantValue)) {
      double value = constantValue.doubleValue();
      if (Double.isNaN(value)) {
        // Every comparison with NaN is false, which the range reasoning does not model.
        return null;
      }
      // The variable is promoted to double before the comparison, so both bounds are converted to double as
      // well. That is exact for byte, short, char and int, and rounds to +/-2^63 for long, the very values
      // (double) Long.MIN_VALUE and (double) Long.MAX_VALUE can take.
      return evaluateComparison(normalizedKind, compare(value, bounds[0]), compare(value, bounds[1]));
    }
    long value = constantValue.longValue();
    return evaluateComparison(normalizedKind, Long.compare(value, bounds[0]), Long.compare(value, bounds[1]));
  }

  @Nullable
  private static long[] resolveTypeBounds(ExpressionTree expression) {
    Type type = expression.symbolType();
    for (Map.Entry<Type.Primitives, long[]> entry : INTEGRAL_BOUNDS.entrySet()) {
      if (type.isPrimitive(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  private static boolean isFloatingPoint(Number constant) {
    return constant instanceof Double || constant instanceof Float;
  }

  /**
   * Ordering consistent with the Java comparison operators, unlike {@link Double#compare}, which orders -0.0
   * before 0.0 while "-0.0 == 0.0" evaluates to true. NaN must be excluded by the caller.
   */
  private static int compare(double value, double bound) {
    if (value < bound) {
      return -1;
    }
    return value > bound ? 1 : 0;
  }

  @Nullable
  private static Number resolveConstantValue(ExpressionTree expression) {
    // Try int literal first: intLiteralValue correctly handles hex/octal/binary int literals with the sign bit set
    // (e.g. 0xCAFEBABE = -889275714), while longLiteralValue would interpret them as large positive longs, causing
    // false positives when compared against int variable bounds.
    Integer intLiteral = LiteralUtils.intLiteralValue(expression);
    if (intLiteral != null) {
      return intLiteral.longValue();
    }
    Long longLiteral = LiteralUtils.longLiteralValue(expression);
    if (longLiteral != null) {
      return longLiteral;
    }
    Double doubleLiteral = LiteralUtils.doubleLiteralValue(expression);
    if (doubleLiteral != null) {
      return doubleLiteral;
    }
    Object constant = ExpressionUtils.resolveAsConstant(expression);
    if (constant instanceof Integer || constant instanceof Long || constant instanceof Float || constant instanceof Double) {
      return (Number) constant;
    }
    return null;
  }

  private static Kind reverseOperator(Kind kind) {
    return switch (kind) {
      case GREATER_THAN -> Kind.LESS_THAN;
      case GREATER_THAN_OR_EQUAL_TO -> Kind.LESS_THAN_OR_EQUAL_TO;
      case LESS_THAN -> Kind.GREATER_THAN;
      case LESS_THAN_OR_EQUAL_TO -> Kind.GREATER_THAN_OR_EQUAL_TO;
      // EQUAL_TO, NOT_EQUAL_TO are symmetric
      default -> kind;
    };
  }

  /**
   * Decides "variable op constant", where the variable ranges over [min, max] and both bounds are reachable.
   *
   * @param toMin the sign of "constant - min"
   * @param toMax the sign of "constant - max"
   * @return the constant result of the comparison, or null when it depends on the value of the variable
   */
  @Nullable
  private static Boolean evaluateComparison(Kind normalizedKind, int toMin, int toMax) {
    return switch (normalizedKind) {
      case GREATER_THAN -> constantResult(toMax >= 0, toMin < 0);
      case GREATER_THAN_OR_EQUAL_TO -> constantResult(toMax > 0, toMin <= 0);
      case LESS_THAN -> constantResult(toMin <= 0, toMax > 0);
      case LESS_THAN_OR_EQUAL_TO -> constantResult(toMin < 0, toMax >= 0);
      case EQUAL_TO -> constantResult(toMin < 0 || toMax > 0, false);
      case NOT_EQUAL_TO -> constantResult(false, toMin < 0 || toMax > 0);
      default -> null;
    };
  }

  @Nullable
  private static Boolean constantResult(boolean alwaysFalse, boolean alwaysTrue) {
    if (alwaysFalse) {
      return Boolean.FALSE;
    }
    return alwaysTrue ? Boolean.TRUE : null;
  }
}
