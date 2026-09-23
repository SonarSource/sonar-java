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

import java.math.BigInteger;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

public final class IntegerOverflowRange {

  private static final int MAX_DEPTH = 6;
  private static final BigInteger INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
  private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
  private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
  private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
  private static final Range NON_NEGATIVE_INT = new Range(BigInteger.ZERO, INT_MAX);
  private static final MethodMatchers NON_NEGATIVE_INT_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofSubTypes("java.lang.CharSequence").names("length").addWithoutParametersMatcher().build(),
    MethodMatchers.create().ofSubTypes("java.util.Collection").names("size").addWithoutParametersMatcher().build(),
    MethodMatchers.create().ofSubTypes("java.util.Map").names("size").addWithoutParametersMatcher().build(),
    MethodMatchers.create().ofSubTypes("java.lang.Enum").names("ordinal").addWithoutParametersMatcher().build());
  private static final MethodMatchers BIT_COUNT_32 = MethodMatchers.create()
    .ofTypes("java.lang.Integer").names("bitCount", "numberOfLeadingZeros", "numberOfTrailingZeros").addParametersMatcher("int").build();
  private static final MethodMatchers BIT_COUNT_64 = MethodMatchers.create()
    .ofTypes("java.lang.Long").names("bitCount", "numberOfLeadingZeros", "numberOfTrailingZeros").addParametersMatcher("long").build();

  private IntegerOverflowRange() {
  }

  @CheckForNull
  public static Range rangeOf(ExpressionTree expression) {
    return rangeOf(expression, 0);
  }

  /**
   * @return true when both operands have a provable range whose difference is guaranteed to fit in an int.
   */
  public static boolean intSubtractionCannotOverflow(ExpressionTree left, ExpressionTree right) {
    Range leftRange = rangeOf(left);
    if (leftRange == null) {
      return false;
    }
    Range rightRange = rangeOf(right);
    return rightRange != null && !leftRange.subtract(rightRange).exceeds(INT_MIN, INT_MAX);
  }

  @CheckForNull
  private static Range rangeOf(ExpressionTree expression, int depth) {
    if (depth > MAX_DEPTH || expression.symbolType().isUnknown()) {
      return null;
    }
    ExpressionTree tree = ExpressionUtils.skipParentheses(expression);
    Range leaf = leafRange(tree, depth);
    if (leaf != null) {
      return leaf;
    }
    if (tree.is(Tree.Kind.PLUS, Tree.Kind.MINUS, Tree.Kind.MULTIPLY)) {
      BinaryExpressionTree binary = (BinaryExpressionTree) tree;
      Range left = rangeOf(binary.leftOperand(), depth + 1);
      Range right = rangeOf(binary.rightOperand(), depth + 1);
      if (left == null || right == null) {
        return null;
      }
      Range mathematical = arithmeticRange(binary, left, right);
      return mathematical.runtimeRange(tree.symbolType());
    }
    if (tree.is(Tree.Kind.UNARY_MINUS)) {
      Range operand = rangeOf(((UnaryExpressionTree) tree).expression(), depth + 1);
      return operand == null ? null : operand.negate().runtimeRange(tree.symbolType());
    }
    return null;
  }

  private static Range arithmeticRange(BinaryExpressionTree tree, Range left, Range right) {
    if (tree.is(Tree.Kind.PLUS)) {
      return left.add(right);
    }
    if (tree.is(Tree.Kind.MINUS)) {
      return left.subtract(right);
    }
    return left.multiply(right);
  }

  @CheckForNull
  private static Range leafRange(ExpressionTree tree, int depth) {
    Range range = constantRange(tree);
    if (range != null) {
      return range;
    }
    range = arrayLengthRange(tree);
    if (range != null) {
      return range;
    }
    range = methodInvocationRange(tree);
    if (range != null) {
      return range;
    }
    range = bitwiseAndRange(tree, depth);
    if (range != null) {
      return range;
    }
    return identifierRange(tree, depth);
  }

  @CheckForNull
  private static Range constantRange(ExpressionTree tree) {
    if (tree.is(Tree.Kind.PLUS, Tree.Kind.MINUS, Tree.Kind.MULTIPLY, Tree.Kind.UNARY_MINUS)) {
      return null;
    }
    Integer intValue = tree.asConstant(Integer.class).orElse(null);
    if (intValue != null) {
      return Range.exact(BigInteger.valueOf(intValue));
    }
    Long longValue = tree.asConstant(Long.class).orElse(null);
    return longValue == null ? null : Range.exact(BigInteger.valueOf(longValue));
  }

  @CheckForNull
  private static Range arrayLengthRange(ExpressionTree tree) {
    if (!tree.is(Tree.Kind.MEMBER_SELECT)) {
      return null;
    }
    MemberSelectExpressionTree select = (MemberSelectExpressionTree) tree;
    return select.expression().symbolType().isArray() && "length".equals(select.identifier().name()) ? NON_NEGATIVE_INT : null;
  }

  @CheckForNull
  private static Range methodInvocationRange(ExpressionTree tree) {
    if (!tree.is(Tree.Kind.METHOD_INVOCATION)) {
      return null;
    }
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (NON_NEGATIVE_INT_METHODS.matches(invocation)) {
      return NON_NEGATIVE_INT;
    }
    if (BIT_COUNT_32.matches(invocation)) {
      return new Range(BigInteger.ZERO, BigInteger.valueOf(32));
    }
    return BIT_COUNT_64.matches(invocation) ? new Range(BigInteger.ZERO, BigInteger.valueOf(64)) : null;
  }

  @CheckForNull
  private static Range bitwiseAndRange(ExpressionTree tree, int depth) {
    if (!tree.is(Tree.Kind.AND)) {
      return null;
    }
    BinaryExpressionTree and = (BinaryExpressionTree) tree;
    Range left = rangeOf(and.leftOperand(), depth + 1);
    if (exactNonNegative(left)) {
      return new Range(BigInteger.ZERO, left.high);
    }
    Range right = rangeOf(and.rightOperand(), depth + 1);
    return exactNonNegative(right) ? new Range(BigInteger.ZERO, right.high) : null;
  }

  @CheckForNull
  private static Range identifierRange(ExpressionTree tree, int depth) {
    if (!tree.is(Tree.Kind.IDENTIFIER)) {
      return null;
    }
    Symbol symbol = ((IdentifierTree) tree).symbol();
    if (symbol.isUnknown() || !ExpressionsHelper.isNotReassigned(symbol)) {
      return null;
    }
    ExpressionTree value = ExpressionsHelper.getSingleWriteUsage(symbol);
    return value == null ? null : rangeOf(value, depth + 1);
  }

  private static boolean exactNonNegative(@Nullable Range range) {
    return range != null && range.isExact() && range.low.signum() >= 0;
  }

  public record Range(BigInteger low, BigInteger high) {

    public static Range exact(BigInteger value) {
      return new Range(value, value);
    }

    public Range add(Range other) {
      return new Range(low.add(other.low), high.add(other.high));
    }

    public Range subtract(Range other) {
      return new Range(low.subtract(other.high), high.subtract(other.low));
    }

    public Range multiply(Range other) {
      BigInteger a = low.multiply(other.low);
      BigInteger b = low.multiply(other.high);
      BigInteger c = high.multiply(other.low);
      BigInteger d = high.multiply(other.high);
      return new Range(a.min(b).min(c).min(d), a.max(b).max(c).max(d));
    }

    public Range negate() {
      return new Range(high.negate(), low.negate());
    }

    public boolean isExact() {
      return low.equals(high);
    }

    /**
     * @return true when at least one value of this range does not fit in the given type.
     */
    public boolean exceeds(Type type) {
      return isLong(type) ? exceeds(LONG_MIN, LONG_MAX) : exceeds(INT_MIN, INT_MAX);
    }

    /**
     * @return true when no value of this range fits in the given type.
     */
    public boolean alwaysExceeds(Type type) {
      BigInteger minimum = isLong(type) ? LONG_MIN : INT_MIN;
      BigInteger maximum = isLong(type) ? LONG_MAX : INT_MAX;
      return low.compareTo(maximum) > 0 || high.compareTo(minimum) < 0;
    }

    private boolean exceeds(BigInteger minimum, BigInteger maximum) {
      return low.compareTo(minimum) < 0 || high.compareTo(maximum) > 0;
    }

    private static boolean isLong(Type type) {
      return type.isPrimitive(Type.Primitives.LONG);
    }

    @CheckForNull
    private Range runtimeRange(Type type) {
      if (!exceeds(type)) {
        return this;
      }
      if (!isExact()) {
        return null;
      }
      int bits = isLong(type) ? Long.SIZE : Integer.SIZE;
      BigInteger modulus = BigInteger.ONE.shiftLeft(bits);
      BigInteger wrapped = low.mod(modulus);
      if (wrapped.testBit(bits - 1)) {
        wrapped = wrapped.subtract(modulus);
      }
      return exact(wrapped);
    }
  }
}
