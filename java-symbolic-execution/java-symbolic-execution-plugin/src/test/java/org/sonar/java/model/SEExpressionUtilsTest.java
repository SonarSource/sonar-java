/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model;


import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.utils.JParserTestUtils;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SEExpressionUtilsTest {

  private boolean parenthesis(boolean b1, boolean b2) {
    return (((b1 && (b2))));
  }

  private void simpleAssignment() {
    int x;
    x = 14;
    (x) = 14;
    x += 1;

    int[] y = new int[5];
    y[x] = 42;
  }

  class MethodName {
    public void foo() {
      foo();
      this.foo();
    }
  }

  CompilationUnitTree classTree = JParserTestUtils.parse(new File("src/test/java/org/sonar/java/model/SEExpressionUtilsTest.java"));

  @Test
  void test_skip_parenthesis() {
    MethodTree methodTree = (MethodTree) ((ClassTree) classTree.types().get(0)).members().get(0);
    ExpressionTree parenthesis = ((ReturnStatementTree) methodTree.block().body().get(0)).expression();

    assertThat(parenthesis.is(Tree.Kind.PARENTHESIZED_EXPRESSION)).isTrue();
    ExpressionTree skipped = SEExpressionUtils.skipParentheses(parenthesis);
    assertThat(skipped.is(Tree.Kind.CONDITIONAL_AND)).isTrue();
    assertThat(SEExpressionUtils.skipParentheses(((BinaryExpressionTree) skipped).leftOperand()).is(Tree.Kind.IDENTIFIER)).isTrue();
  }

  @Test
  void test_simple_assignments() {
    MethodTree methodTree = (MethodTree) ((ClassTree) classTree.types().get(0)).members().get(1);
    List<AssignmentExpressionTree> assignments = findAssignmentExpressionTrees(methodTree);

    assertThat(assignments).hasSize(4);
    assertThat(SEExpressionUtils.isSimpleAssignment(assignments.get(0))).isTrue();
    assertThat(SEExpressionUtils.isSimpleAssignment(assignments.get(1))).isTrue();
    assertThat(SEExpressionUtils.isSimpleAssignment(assignments.get(2))).isFalse();
    assertThat(SEExpressionUtils.isSimpleAssignment(assignments.get(3))).isFalse();
  }

  @Test
  void private_constructor() throws Exception {
    assertThat(isFinal(SEExpressionUtils.class.getModifiers())).isTrue();
    Constructor<SEExpressionUtils> constructor = SEExpressionUtils.class.getDeclaredConstructor();
    assertThat(isPrivate(constructor.getModifiers())).isTrue();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void test_extract_identifier_mixed_access() {
    File file = new File("src/test/files/model/SEExpressionUtilsTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(1);
    List<AssignmentExpressionTree> assignments = findAssignmentExpressionTrees(methodTree);

    // This should reflect method 'mixedReference'.
    assertThat(assignments).hasSize(5);
    assertThat(SEExpressionUtils.isSimpleAssignment(assignments.get(0))).isTrue();
    assertThat(SEExpressionUtils.isSimpleAssignment(assignments.get(1))).isTrue();
    // Contains method invocation.
    assertThat(SEExpressionUtils.isSimpleAssignment(assignments.get(2))).isFalse();
    // Compound assignment
    assertThat(SEExpressionUtils.isSimpleAssignment(assignments.get(2))).isFalse();

    // The returned identifier should have the same symbol regardless of the explicit usage of this.
    assertThat(SEExpressionUtils.extractIdentifier(assignments.get(0)).symbol())
      .isEqualTo(SEExpressionUtils.extractIdentifier(assignments.get(1)).symbol());

  }

  @Test
  void test_cannot_extract_identifier() {
    File file = new File("src/test/files/model/SEExpressionUtilsTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(1);
    List<AssignmentExpressionTree> assignments = findAssignmentExpressionTrees(methodTree);
    AssignmentExpressionTree assignment = assignments.get(4);
    assertThrows(IllegalArgumentException.class, () -> SEExpressionUtils.extractIdentifier(assignment));
  }

  private List<AssignmentExpressionTree> findAssignmentExpressionTrees(MethodTree methodTree) {
    return methodTree.block().body().stream()
      .filter(s -> s.is(Tree.Kind.EXPRESSION_STATEMENT))
      .map(ExpressionStatementTree.class::cast)
      .map(ExpressionStatementTree::expression)
      .filter(e -> e instanceof AssignmentExpressionTree)
      .map(AssignmentExpressionTree.class::cast)
      .toList();
  }

  @Test
  void method_name() {
    ClassTree outerClass = (ClassTree) classTree.types().get(0);
    ClassTree innerClass = (ClassTree) outerClass.members().get(2);
    MethodTree methodTree = (MethodTree) innerClass.members().get(0);

    MethodInvocationTree firstMIT = (MethodInvocationTree) ((ExpressionStatementTree) methodTree.block().body().get(0)).expression();
    MethodInvocationTree secondMIT = (MethodInvocationTree) ((ExpressionStatementTree) methodTree.block().body().get(1)).expression();

    assertThat(SEExpressionUtils.methodName(firstMIT).name()).isEqualTo("foo");
    assertThat(SEExpressionUtils.methodName(secondMIT).name()).isEqualTo("foo");
  }

  @Test
  void resolve_as_int_constant() {
    assertResolveAsConstant("0", 0);
    assertResolveAsConstant("1", 1);
    assertResolveAsConstant("+1", +1);
    assertResolveAsConstant("0x01 | 0xF0", 0x01 | 0xF0);
    assertResolveAsConstant("-1", -1);
    assertResolveAsConstant("(1)", (1));
    assertResolveAsConstant("~42", ~42);
  }

  @Test
  void resolve_as_long_constant() {
    assertResolveAsConstant("-(0x01 + 2L)", -(0x01 + 2L));
    assertResolveAsConstant("0L", 0L);
    assertResolveAsConstant("1L", 1L);
    assertResolveAsConstant("-1L", -1L);
    assertResolveAsConstant("-(1L)", -(1L));
    assertResolveAsConstant("-(-1L)", -(-1L));
    assertResolveAsConstant("-(-(1L))", -(-(1L)));
    assertResolveAsConstant("-0x25L", -0x25L);
    assertResolveAsConstant("~42L", ~42L);
  }

  @Test
  void resolve_as_boolean_constant() {
    assertResolveAsConstant("true", true);
    assertResolveAsConstant("!true", !true);
    assertResolveAsConstant("false", false);
    assertResolveAsConstant("!false", !false);
    assertResolveAsConstant("Boolean.TRUE", true);
    assertResolveAsConstant("Boolean.FALSE", false);
  }

  @Test
  void resolve_as_string_constant() {
    assertResolveAsConstant("\"abc\"", "abc");
    assertResolveAsConstant("(\"abc\")", ("abc"));
  }

  @Test
  void resolve_as_constant_not_yet_supported() {
    assertResolveAsConstant("true || true", null);
  }

  @Test
  void resolve_as_constant_arithmetic_operations() {
    assertResolveAsConstant("1 + 1 - 1", 1);
    assertResolveAsConstant("8 - 3 + 2 * 2", 9);
    assertResolveAsConstant("8 - (3 + 2) * 2", -2);
    assertResolveAsConstant("8 - (3 + 2) / 5 * 2", 6);
    assertResolveAsConstant("8 - (3 + 2) % 5 * 2", 8);
    assertResolveAsConstant("8 - (x + 2) % 5 * 2", null);
    assertResolveAsConstant("8 - (3 + x) % 5 * 2", null);
    assertResolveAsConstant("8 - (x + x) % 5 * 2", null);
  }

  @Test
  void resolve_as_constant_division_by_zero() {
    assertResolveAsConstant("5 / 0", null);
    assertResolveAsConstant("5L / 0", null);
    assertResolveAsConstant("5D / 0", null);

    assertResolveAsConstant("5 / 0L", null);
    assertResolveAsConstant("5L / 0L", null);
    assertResolveAsConstant("5D / 0L", null);

    assertResolveAsConstant("5 / 0D", null);
    assertResolveAsConstant("5L / 0D", null);
    assertResolveAsConstant("5D / 0D", null);
  }

  @Test
  void resolve_as_constant_unknown_symbol() {
    assertResolveAsConstant("x", null);
    assertResolveAsConstant("-x", null);
    assertResolveAsConstant("~x", null);
    assertResolveAsConstant("!x", null);
    assertResolveAsConstant("++x", null);
    assertResolveAsConstant("x.y", null);
  }

  private void assertResolveAsConstant(String code, @Nullable Object expected) {
    CompilationUnitTree unit = JParserTestUtils.parse("class A { Object f = " + code + "; }");
    ExpressionTree expression = ((VariableTree) ((ClassTree) unit.types().get(0)).members().get(0)).initializer();
    Object actual = SEExpressionUtils.resolveAsConstant(expression);
    if (expected == null) {
      assertThat(actual).isNull();
    } else {
      assertThat(actual)
        .hasSameClassAs(expected)
        .isEqualTo(expected);
    }
  }

}
