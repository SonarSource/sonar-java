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

import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.helpers.IntegerOverflowRange.Range;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;

import static org.assertj.core.api.Assertions.assertThat;

class IntegerOverflowRangeTest {

  private static final BigInteger INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
  private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);

  @Test
  void private_constructor() throws Exception {
    Constructor<IntegerOverflowRange> constructor = IntegerOverflowRange.class.getDeclaredConstructor();
    assertThat(constructor.canAccess(null)).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void long_constant_subtraction_cannot_overflow_int() {
    BinaryExpressionTree subtraction = (BinaryExpressionTree) initializer("void m() { long x = 5L - 3L; }");
    assertThat(IntegerOverflowRange.intSubtractionCannotOverflow(subtraction.leftOperand(), subtraction.rightOperand())).isTrue();
  }

  @Test
  void subtraction_with_unknown_operand_is_not_proven_safe() {
    BinaryExpressionTree unknownLeft = (BinaryExpressionTree) initializer("void m(int a) { int x = a - 1; }");
    assertThat(IntegerOverflowRange.intSubtractionCannotOverflow(unknownLeft.leftOperand(), unknownLeft.rightOperand())).isFalse();
    BinaryExpressionTree unknownRight = (BinaryExpressionTree) initializer("void m(int a) { int x = 1 - a; }");
    assertThat(IntegerOverflowRange.intSubtractionCannotOverflow(unknownRight.leftOperand(), unknownRight.rightOperand())).isFalse();
  }

  @Test
  void exact_overflow_wraps_like_the_runtime() {
    assertThat(IntegerOverflowRange.rangeOf(initializer("void m() { int x = Integer.MAX_VALUE + 1; }")))
      .isEqualTo(Range.exact(INT_MIN));
    assertThat(IntegerOverflowRange.rangeOf(initializer("void m() { long x = Long.MIN_VALUE - 1L; }")))
      .isEqualTo(Range.exact(BigInteger.valueOf(Long.MAX_VALUE)));
  }

  @Test
  void possible_overflow_of_a_bounded_range_is_unknown() {
    assertThat(IntegerOverflowRange.rangeOf(initializer("void m(String s) { int x = s.length() + 1; }"))).isNull();
    assertThat(IntegerOverflowRange.rangeOf(initializer("void m(String s) { int x = s.length() - 1; }")))
      .isEqualTo(new Range(BigInteger.ONE.negate(), INT_MAX.subtract(BigInteger.ONE)));
  }

  @Test
  void exceeds_and_always_exceeds() {
    Type intType = initializer("void m() { int x = 1; }").symbolType();
    Type longType = initializer("void m() { long x = 1L; }").symbolType();

    Range partlyAbove = new Range(BigInteger.ZERO, INT_MAX.add(BigInteger.ONE));
    assertThat(partlyAbove.exceeds(intType)).isTrue();
    assertThat(partlyAbove.alwaysExceeds(intType)).isFalse();
    assertThat(partlyAbove.exceeds(longType)).isFalse();

    Range fullyAbove = new Range(INT_MAX.add(BigInteger.ONE), INT_MAX.add(BigInteger.TWO));
    assertThat(fullyAbove.alwaysExceeds(intType)).isTrue();
    assertThat(fullyAbove.alwaysExceeds(longType)).isFalse();

    Range fullyBelow = new Range(INT_MIN.subtract(BigInteger.TWO), INT_MIN.subtract(BigInteger.ONE));
    assertThat(fullyBelow.alwaysExceeds(intType)).isTrue();

    Range fitting = new Range(INT_MIN, INT_MAX);
    assertThat(fitting.exceeds(intType)).isFalse();
    assertThat(fitting.alwaysExceeds(intType)).isFalse();
    assertThat(fitting.isExact()).isFalse();
    assertThat(Range.exact(BigInteger.TEN).isExact()).isTrue();
  }

  private static ExpressionTree initializer(String method) {
    List<StatementTree> statements = JParserTestUtils.methodBody(JParserTestUtils.newCode(method));
    return JParserTestUtils.initializerFromVariableDeclarationStatement(statements.get(0));
  }
}
