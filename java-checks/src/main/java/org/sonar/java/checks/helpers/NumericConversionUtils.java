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
package org.sonar.java.checks.helpers;

import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;

public final class NumericConversionUtils {

  private static final long FLOAT_MAX_EXACT_INT = 1L << 24;
  private static final long DOUBLE_MAX_EXACT_LONG = 1L << 53;

  private NumericConversionUtils() {
  }

  public static boolean isPotentiallyLossyWideningConversion(ExpressionTree expression, Type targetType) {
    ExpressionTree unwrapped = ExpressionUtils.skipParentheses(expression);
    return isLossyWideningConversion(unwrapped.symbolType(), targetType) && !isSafeConstant(unwrapped, targetType);
  }

  private static boolean isLossyWideningConversion(Type sourceType, Type targetType) {
    if (sourceType.isUnknown() || targetType.isUnknown()) {
      return false;
    }
    if (sourceType.isPrimitive(Type.Primitives.INT) && targetType.isPrimitive(Type.Primitives.FLOAT)) {
      return true;
    }
    if (sourceType.isPrimitive(Type.Primitives.LONG) && targetType.isPrimitive(Type.Primitives.FLOAT)) {
      return true;
    }
    return sourceType.isPrimitive(Type.Primitives.LONG) && targetType.isPrimitive(Type.Primitives.DOUBLE);
  }

  private static boolean isSafeConstant(ExpressionTree expr, Type targetType) {
    Object constant = ExpressionUtils.resolveAsConstant(expr);
    if (constant instanceof Integer intVal) {
      long absValue = Math.abs((long) intVal);
      return !targetType.isPrimitive(Type.Primitives.FLOAT) || absValue <= FLOAT_MAX_EXACT_INT;
    }
    if (constant instanceof Long longVal) {
      long absValue = Math.abs(longVal);
      if (targetType.isPrimitive(Type.Primitives.FLOAT)) {
        return absValue <= FLOAT_MAX_EXACT_INT;
      }
      if (targetType.isPrimitive(Type.Primitives.DOUBLE)) {
        return absValue <= DOUBLE_MAX_EXACT_LONG;
      }
    }
    return false;
  }
}
