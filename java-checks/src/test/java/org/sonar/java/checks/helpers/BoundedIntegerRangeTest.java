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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;

import static org.assertj.core.api.Assertions.assertThat;

class BoundedIntegerRangeTest {

  @Test
  void private_constructor() throws Exception {
    Constructor<BoundedIntegerRange> constructor = BoundedIntegerRange.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void both_operands_bounded_cannot_overflow() {
    assertThat(subtractionCannotOverflow(
      "int compare(String left, String right) { return left.length() - right.length(); }")).isTrue();
  }

  @Test
  void unbounded_right_operand_can_overflow() {
    assertThat(subtractionCannotOverflow(
      "int compare(String left, Object right) { return left.length() - right.hashCode(); }")).isFalse();
  }

  @Test
  void unbounded_left_operand_can_overflow() {
    assertThat(subtractionCannotOverflow(
      "int compare(Object left, String right) { return left.hashCode() - right.length(); }")).isFalse();
  }

  @Test
  void masked_byte_operands_cannot_overflow() {
    assertThat(subtractionCannotOverflow(
      "int compare(byte[] left, byte[] right) { return (left[0] & 0xff) - (right[0] & 0xff); }")).isTrue();
  }

  @Test
  void masking_with_no_constant_operand_is_not_resolved() {
    assertThat(subtractionCannotOverflow(
      "int compare(int left, int mask, int right) { return (left & mask) - right; }")).isFalse();
  }

  @Test
  void sign_bit_hex_literal_operand_can_overflow() {
    // left.length() is [0, MAX_VALUE], but MIN_VALUE (0x80000000 as an int literal) makes the subtraction overflow.
    assertThat(subtractionCannotOverflow(
      "int compare(String left, String right) { return left.length() - 0x80000000; }")).isFalse();
  }

  @Test
  void long_constant_operands_cannot_overflow() {
    // Reached directly, bypassing the check's int/long dispatch, to exercise the long-constant branch of constantRange.
    List<StatementTree> statements = JParserTestUtils.methodBody(JParserTestUtils.newCode("void m() { long x = 5L - 3L; }"));
    ExpressionTree initializer = JParserTestUtils.initializerFromVariableDeclarationStatement(statements.get(0));
    BinaryExpressionTree binary = (BinaryExpressionTree) initializer;
    assertThat(BoundedIntegerRange.subtractionCannotOverflow(binary.leftOperand(), binary.rightOperand())).isTrue();
  }

  @Test
  void deeply_chained_single_write_locals_are_not_resolved() {
    // The resolution depth cap keeps this conservative: a long chain of single-write locals is treated as unbounded
    // rather than resolved all the way back to the bounded String.length() call.
    assertThat(subtractionCannotOverflow(
      "int compare(String left, String right) {",
      "  int a = left.length();",
      "  int b = a;",
      "  int c = b;",
      "  int d = c;",
      "  int e = d;",
      "  return e - right.length();",
      "}")).isFalse();
  }

  @Test
  void reassignment_by_increment_is_not_a_single_write() {
    // i++ is not seen as a reassignment by getSingleWriteUsage, so without the isNotReassigned guard this would
    // incorrectly resolve i to its initializer value 0.
    assertThat(subtractionCannotOverflow(
      "int compare(Node left, Node right) {",
      "  int i = 0;",
      "  for (Node n = left; n != null; n = n.parent) { i++; }",
      "  return i - right.hashCode();",
      "}",
      "static class Node { Node parent; }")).isFalse();
  }

  private static boolean subtractionCannotOverflow(String... classMembers) {
    MethodTree method = JParserTestUtils.methodTree(JParserTestUtils.newCode(classMembers));
    List<StatementTree> statements = method.block().body();
    ReturnStatementTree returnStatement = (ReturnStatementTree) statements.get(statements.size() - 1);
    BinaryExpressionTree binary = (BinaryExpressionTree) returnStatement.expression();
    return BoundedIntegerRange.subtractionCannotOverflow(binary.leftOperand(), binary.rightOperand());
  }

}
