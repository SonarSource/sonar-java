/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StaticInitializerTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonar.java.model.ExpressionUtils.isInvocationOnVariable;
import static org.sonar.java.model.assertions.TreeAssert.assertThat;

class ExpressionUtilsTest {

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

  @Test
  void test_skip_parenthesis() throws Exception {
    File file = new File("src/test/java/org/sonar/java/model/ExpressionUtilsTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(0);
    ExpressionTree parenthesis = ((ReturnStatementTree) methodTree.block().body().get(0)).expression();

    assertThat(parenthesis).is(Tree.Kind.PARENTHESIZED_EXPRESSION);
    ExpressionTree skipped = ExpressionUtils.skipParentheses(parenthesis);
    assertThat(skipped).is(Tree.Kind.CONDITIONAL_AND);
    assertThat(ExpressionUtils.skipParentheses(((BinaryExpressionTree) skipped).leftOperand())).is(Tree.Kind.IDENTIFIER);
  }

  @Test
  void test_simple_assignments() throws Exception {
    File file = new File("src/test/java/org/sonar/java/model/ExpressionUtilsTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(1);
    List<AssignmentExpressionTree> assignments = findAssignmentExpressionTrees(methodTree);

    assertThat(assignments).hasSize(4);
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(0))).isTrue();
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(1))).isTrue();
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(2))).isFalse();
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(3))).isFalse();
  }

  @Test
  void method_name() throws Exception {
    File file = new File("src/test/files/model/ExpressionUtilsMethodNameTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(0);

    MethodInvocationTree firstMIT = (MethodInvocationTree) ((ExpressionStatementTree) methodTree.block().body().get(0)).expression();
    MethodInvocationTree secondMIT = (MethodInvocationTree) ((ExpressionStatementTree) methodTree.block().body().get(1)).expression();

    assertThat(ExpressionUtils.methodName(firstMIT).name()).isEqualTo("foo");
    assertThat(ExpressionUtils.methodName(secondMIT).name()).isEqualTo("foo");
  }

  @Test
  void private_constructor() throws Exception {
    assertThat(isFinal(ExpressionUtils.class.getModifiers())).isTrue();
    Constructor<ExpressionUtils> constructor = ExpressionUtils.class.getDeclaredConstructor();
    assertThat(isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void test_extract_identifier_mixed_access() throws Exception {
    File file = new File("src/test/files/model/ExpressionUtilsTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(1);
    List<AssignmentExpressionTree> assignments = findAssignmentExpressionTrees(methodTree);

    // This should reflect method 'mixedReference'.
    assertThat(assignments).hasSize(5);
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(0))).isTrue();
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(1))).isTrue();
    // Contains method invocation.
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(2))).isFalse();
    // Compound assignment
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(2))).isFalse();

    // The returned identifier should have the same symbol regardless of the explicit usage of this.
    assertThat(ExpressionUtils.extractIdentifier(assignments.get(0)).symbol())
      .isEqualTo(ExpressionUtils.extractIdentifier(assignments.get(1)).symbol());

  }

  @Test
  void test_cannot_extract_identifier() throws Exception {
    File file = new File("src/test/files/model/ExpressionUtilsTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(1);
    List<AssignmentExpressionTree> assignments = findAssignmentExpressionTrees(methodTree);
    AssignmentExpressionTree assignment = assignments.get(4);
    assertThrows(IllegalArgumentException.class, () -> ExpressionUtils.extractIdentifier(assignment));
  }

  @Test
  void test_get_assigned_symbol() throws Exception {
    File file = new File("src/test/files/model/ExpressionUtilsTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(1);
    List<AssignmentExpressionTree> assignments = findAssignmentExpressionTrees(methodTree);

    // This should reflect method 'mixedReference'.
    assertThat(assignments).hasSize(5);

    assertThat(ExpressionUtils.getAssignedSymbol(assignments.get(0).expression())).isPresent();
    assertThat(ExpressionUtils.getAssignedSymbol(assignments.get(1).expression())).isPresent();

    assertThat(ExpressionUtils.getAssignedSymbol(assignments.get(0).expression())).
    contains(ExpressionUtils.getAssignedSymbol(assignments.get(1).expression()).get());

    assertThat(ExpressionUtils.getAssignedSymbol(assignments.get(2).expression())).isNotPresent();
    assertThat(ExpressionUtils.getAssignedSymbol(assignments.get(3).expression())).isNotPresent();
    assertThat(ExpressionUtils.getAssignedSymbol(assignments.get(4).expression())).isNotPresent();

    List<VariableTree> variables = findVariableTrees(methodTree);
    assertThat(variables).hasSize(2);
    assertThat(ExpressionUtils.getAssignedSymbol(variables.get(1).initializer())).isPresent();

  }

  @Test
  void test_invocation_on_same_variable() {
    CompilationUnitTree tree = JParserTestUtils.parse(
      "class A {\n" +
        "  static {\n" +
        "    String s1 = \"a\";" +
        "    String s2 = \"b\";" +
        "    s1.toString();\n" +
        "    s2.toString();\n" +
        "    toString();\n" +
        "    Optional.of(s1).get().toString();\n" +
        "    Optional.of(s2).get().toString();\n" +
        "  }\n" +
        "}");

    StaticInitializerTree staticInitializer = (StaticInitializerTree) ((ClassTree) tree.types().get(0)).members().get(0);
    List<Symbol> variablesSymbols = staticInitializer.body().stream()
      .filter(t -> t instanceof VariableTree)
      .map(VariableTree.class::cast)
      .map(VariableTree::symbol)
      .collect(Collectors.toList());

    List<MethodInvocationTree> invocations = staticInitializer.body().stream()
      .filter(t -> t instanceof ExpressionStatementTree)
      .map(ExpressionStatementTree.class::cast)
      .map(ExpressionStatementTree::expression)
      .map(MethodInvocationTree.class::cast)
      .collect(Collectors.toList());

    assertThat(isInvocationOnVariable(invocations.get(0), variablesSymbols.get(0), true)).isTrue();
    assertThat(isInvocationOnVariable(invocations.get(0), variablesSymbols.get(1), true)).isFalse();
    assertThat(isInvocationOnVariable(invocations.get(1), variablesSymbols.get(0), true)).isFalse();
    assertThat(isInvocationOnVariable(invocations.get(1), variablesSymbols.get(1), true)).isTrue();

    // We report the default value if we can not compare two symbol
    assertThat(isInvocationOnVariable(invocations.get(2), variablesSymbols.get(0), true)).isTrue();
    assertThat(isInvocationOnVariable(invocations.get(2), variablesSymbols.get(1), true)).isTrue();
    assertThat(isInvocationOnVariable(invocations.get(3), variablesSymbols.get(0), true)).isTrue();
    assertThat(isInvocationOnVariable(invocations.get(3), variablesSymbols.get(1), true)).isTrue();
    assertThat(isInvocationOnVariable(invocations.get(4), variablesSymbols.get(0), true)).isTrue();
    assertThat(isInvocationOnVariable(invocations.get(4), variablesSymbols.get(1), true)).isTrue();

    assertThat(isInvocationOnVariable(invocations.get(2), variablesSymbols.get(0), false)).isFalse();
    assertThat(isInvocationOnVariable(invocations.get(2), variablesSymbols.get(1), false)).isFalse();
    assertThat(isInvocationOnVariable(invocations.get(3), variablesSymbols.get(0), false)).isFalse();
    assertThat(isInvocationOnVariable(invocations.get(3), variablesSymbols.get(1), false)).isFalse();
    assertThat(isInvocationOnVariable(invocations.get(4), variablesSymbols.get(0), false)).isFalse();
    assertThat(isInvocationOnVariable(invocations.get(4), variablesSymbols.get(1), false)).isFalse();

    assertThat(isInvocationOnVariable(invocations.get(4), null, false)).isFalse();
    assertThat(isInvocationOnVariable(invocations.get(4), null, true)).isTrue();
  }


  @Test
  void securing_byte() {
    CompilationUnitTree tree = JParserTestUtils.parse(
      "class A {\n" +
        "  static {\n" +
        "    int i1 = 12;\n" +
        "    int i2 = 12 & 0xFF;\n" +
        "    int i3 = 0xff & 12;\n" +
        "    int i4 = 12 & 12;\n" +
        "  }\n" +
        "}");

    StaticInitializerTree staticInitializer = (StaticInitializerTree) ((ClassTree) tree.types().get(0)).members().get(0);
    List<ExpressionTree> expressions = staticInitializer.body().stream()
      .map(VariableTree.class::cast)
      .map(VariableTree::initializer)
      .collect(Collectors.toList());

    assertThat(ExpressionUtils.isSecuringByte(expressions.get(0))).isFalse();
    assertThat(ExpressionUtils.isSecuringByte(expressions.get(1))).isTrue();
    assertThat(ExpressionUtils.isSecuringByte(expressions.get(2))).isTrue();
    assertThat(ExpressionUtils.isSecuringByte(expressions.get(3))).isFalse();
  }

  private List<AssignmentExpressionTree> findAssignmentExpressionTrees(MethodTree methodTree) {
    return methodTree.block().body().stream()
          .filter(s -> s.is(Tree.Kind.EXPRESSION_STATEMENT))
          .map(ExpressionStatementTree.class::cast)
          .map(ExpressionStatementTree::expression)
          .filter(e -> e instanceof AssignmentExpressionTree)
          .map(AssignmentExpressionTree.class::cast)
          .collect(Collectors.toList());
  }

  private List<VariableTree> findVariableTrees(MethodTree methodTree) {
    return methodTree.block().body().stream()
      .filter(s -> s.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .collect(Collectors.toList());
  }

  @Test
  void enclosing_method_test() {
    File file = new File("src/test/files/model/ExpressionEnclosingMethodTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    FindAssignment findAssignment = new FindAssignment();
    tree.accept(findAssignment);
    findAssignment.assignments.forEach(a -> {
      String expectedName = a.firstToken().trivias().get(0).comment().substring(3);
      MethodTree enclosingMethod = ExpressionUtils.getEnclosingMethod(a);
      if ("null".equals(expectedName)) {
        assertThat(enclosingMethod).isNull();
      } else {
        assertThat(enclosingMethod.simpleName().name()).isEqualTo(expectedName);
      }
    });
  }

  private static class FindAssignment extends BaseTreeVisitor {
    private List<AssignmentExpressionTree> assignments = new ArrayList<>();

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      assignments.add(tree);
      super.visitAssignmentExpression(tree);
    }
  }

  @Test
  void resolve_as_constant() {
    assertResolveAsConstant("0", 0);
    assertResolveAsConstant("1", 1);
    assertResolveAsConstant("+1", +1);
    assertResolveAsConstant("-(0x01 + 2L)", -(0x01 + 2L));
    assertResolveAsConstant("0x01 | 0xF0", 0x01 | 0xF0);
    assertResolveAsConstant("-1", -1);
    assertResolveAsConstant("0L", 0L);
    assertResolveAsConstant("1L", 1L);
    assertResolveAsConstant("-1L", -1L);
    assertResolveAsConstant("true", true);
    assertResolveAsConstant("!true", !true);
    assertResolveAsConstant("false", false);
    assertResolveAsConstant("!false", !false);
    assertResolveAsConstant("(1)", (1));
    assertResolveAsConstant("-(1L)", -(1L));
    assertResolveAsConstant("-(-1L)", -(-1L));
    assertResolveAsConstant("-(-(1L))", -(-(1L)));
    assertResolveAsConstant("-0x25L", -0x25L);
    assertResolveAsConstant("~42", ~42);
    assertResolveAsConstant("~42L", ~42L);
    assertResolveAsConstant("\"abc\"", "abc");
    assertResolveAsConstant("(\"abc\")", ("abc"));
    assertResolveAsConstant("Boolean.TRUE", true);
    assertResolveAsConstant("Boolean.FALSE", false);
    // not yet supported
    assertResolveAsConstant("true || true", null);
    assertResolveAsConstant("2 * 2", null);
    // unknown
    assertResolveAsConstant("x", null);
    assertResolveAsConstant("-x", null);
    assertResolveAsConstant("~x", null);
    assertResolveAsConstant("!x", null);
    assertResolveAsConstant("++x", null);
    assertResolveAsConstant("x.y", null);
  }

  private void assertResolveAsConstant(String code, @Nullable Object expected) {
    CompilationUnitTree unit = JParserTestUtils.parse("class A { Object f = " + code + "; }");
    ExpressionTree expression = ((VariableTree)((ClassTree) unit.types().get(0)).members().get(0)).initializer();
    Object actual = ExpressionUtils.resolveAsConstant(expression);
    if (expected == null) {
      assertThat(actual).isNull();
    } else {
      assertThat(actual)
        .hasSameClassAs(expected)
        .isEqualTo(expected);
    }
  }

}
