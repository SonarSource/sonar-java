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

import javax.annotation.CheckForNull;
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

/**
 * Computes a best-effort [lo, hi] bound for an integer-typed expression, used to prove that
 * subtracting two such expressions cannot produce a 32-bit overflow.
 * <p>
 * This is intentionally conservative: it only recognizes a small set of JDK APIs and operations
 * with a documented or structurally guaranteed range. Any expression it cannot classify is
 * treated as unbounded, which keeps every unrecognized case reported as before.
 */
public final class BoundedIntegerRange {

  private static final int MAX_SINGLE_WRITE_DEPTH = 3;

  private static final Range NON_NEGATIVE_INT = new Range(0, Integer.MAX_VALUE);

  private static final MethodMatchers CHAR_SEQUENCE_LENGTH = MethodMatchers.create()
    .ofSubTypes("java.lang.CharSequence").names("length").addWithoutParametersMatcher().build();

  private static final MethodMatchers COLLECTION_SIZE = MethodMatchers.create()
    .ofSubTypes("java.util.Collection").names("size").addWithoutParametersMatcher().build();

  private static final MethodMatchers MAP_SIZE = MethodMatchers.create()
    .ofSubTypes("java.util.Map").names("size").addWithoutParametersMatcher().build();

  private static final MethodMatchers ENUM_ORDINAL = MethodMatchers.create()
    .ofSubTypes("java.lang.Enum").names("ordinal").addWithoutParametersMatcher().build();

  private static final MethodMatchers BIT_COUNT_32 = MethodMatchers.create()
    .ofTypes("java.lang.Integer").names("bitCount", "numberOfLeadingZeros", "numberOfTrailingZeros").addParametersMatcher("int").build();

  private static final MethodMatchers BIT_COUNT_64 = MethodMatchers.create()
    .ofTypes("java.lang.Long").names("bitCount", "numberOfLeadingZeros", "numberOfTrailingZeros").addParametersMatcher("long").build();

  private BoundedIntegerRange() {
  }

  /**
   * @return true when both operands have a provable range whose difference is guaranteed to fit in an int,
   * i.e. cannot overflow regardless of the actual runtime values.
   */
  public static boolean subtractionCannotOverflow(ExpressionTree left, ExpressionTree right) {
    Range leftRange = rangeOf(left, 0);
    if (leftRange == null) {
      return false;
    }
    Range rightRange = rangeOf(right, 0);
    return rightRange != null && leftRange.fitsIntSubtraction(rightRange);
  }

  @CheckForNull
  private static Range rangeOf(ExpressionTree expression, int depth) {
    if (depth > MAX_SINGLE_WRITE_DEPTH) {
      return null;
    }
    ExpressionTree tree = ExpressionUtils.skipParentheses(expression);
    Range constant = constantRange(tree);
    if (constant != null) {
      return constant;
    }
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      return arrayLengthRange((MemberSelectExpressionTree) tree);
    }
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      return methodInvocationRange((MethodInvocationTree) tree);
    }
    if (tree.is(Tree.Kind.AND)) {
      return bitwiseAndRange((BinaryExpressionTree) tree, depth);
    }
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      return identifierRange((IdentifierTree) tree, depth);
    }
    return null;
  }

  @CheckForNull
  private static Range constantRange(ExpressionTree tree) {
    // Resolve through the compiler's own constant folding rather than re-parsing literal text, so the
    // value respects the expression's static type: an int-typed 0x80000000 is -2147483648, not +2147483648.
    Integer intValue = tree.asConstant(Integer.class).orElse(null);
    if (intValue != null) {
      return new Range(intValue, intValue);
    }
    Long longValue = tree.asConstant(Long.class).orElse(null);
    if (longValue != null) {
      return new Range(longValue, longValue);
    }
    return null;
  }

  @CheckForNull
  private static Range arrayLengthRange(MemberSelectExpressionTree memberSelect) {
    Type ownerType = memberSelect.expression().symbolType();
    String memberName = memberSelect.identifier().name();
    if (ownerType.isArray() && "length".equals(memberName)) {
      return NON_NEGATIVE_INT;
    }
    return null;
  }

  @CheckForNull
  private static Range methodInvocationRange(MethodInvocationTree invocation) {
    if (CHAR_SEQUENCE_LENGTH.matches(invocation) || COLLECTION_SIZE.matches(invocation)
      || MAP_SIZE.matches(invocation) || ENUM_ORDINAL.matches(invocation)) {
      return NON_NEGATIVE_INT;
    }
    if (BIT_COUNT_32.matches(invocation)) {
      return new Range(0, 32);
    }
    if (BIT_COUNT_64.matches(invocation)) {
      return new Range(0, 64);
    }
    return null;
  }

  @CheckForNull
  private static Range bitwiseAndRange(BinaryExpressionTree and, int depth) {
    Long nonNegativeConstant = nonNegativeConstant(and.leftOperand(), depth);
    if (nonNegativeConstant == null) {
      nonNegativeConstant = nonNegativeConstant(and.rightOperand(), depth);
    }
    if (nonNegativeConstant == null) {
      return null;
    }
    return new Range(0, nonNegativeConstant);
  }

  @CheckForNull
  private static Long nonNegativeConstant(ExpressionTree expression, int depth) {
    Range range = rangeOf(expression, depth + 1);
    if (range != null && range.lo() == range.hi() && range.lo() >= 0) {
      return range.lo();
    }
    return null;
  }

  @CheckForNull
  private static Range identifierRange(IdentifierTree identifier, int depth) {
    Symbol symbol = identifier.symbol();
    if (symbol.isUnknown() || !ExpressionsHelper.isNotReassigned(symbol)) {
      // getSingleWriteUsage only looks at AssignmentExpressionTree reassignments, so a variable
      // mutated by ++/-- would otherwise look like a single-write local pinned to its initializer.
      return null;
    }
    ExpressionTree singleWriteUsage = ExpressionsHelper.getSingleWriteUsage(symbol);
    return singleWriteUsage == null ? null : rangeOf(singleWriteUsage, depth + 1);
  }

  private record Range(long lo, long hi) {
    boolean fitsIntSubtraction(Range other) {
      return hi - other.lo() <= Integer.MAX_VALUE && lo - other.hi() >= Integer.MIN_VALUE;
    }
  }

}
