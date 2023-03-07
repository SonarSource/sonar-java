/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class SyntacticEquivalenceTest {

  @Test
  void null_equivalence() throws Exception {
    assertThat(SyntacticEquivalence.areEquivalent((Tree) null, null)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(null, compilationUnitTree("class A{}"))).isFalse();
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), null)).isFalse();

  }

  @Test
  void statement_list_equivalence() {
    assertAreEquivalent(Arrays.asList("foo()", "bar()"), Arrays.asList("foo()", "bar()"));
    assertAreNotEquivalent(Arrays.asList("foo()", "bar()"), Arrays.asList("foo()", "foo()"));
    assertAreNotEquivalent(Arrays.asList("foo()"), Arrays.asList("foo()", "foo()"));
  }

  @Test
  void test_equivalence() {
    assertAreEquivalent("foo()", "foo()");
    assertAreNotEquivalent("foo()", "bar()");
    assertAreEquivalent("int a", "int a");
    assertAreEquivalent("List<String> a", "List<String> a");
    assertAreNotEquivalent("int a", "int b");
    assertAreNotEquivalent("", "int b");
    assertAreEquivalent("foo(a, b, c)", "foo(a, b, c)");
  }

  @Test
  void lambda_equivalence() {
    assertAreEquivalent("foo(bar->0)", "foo(bar->0)");
    assertAreNotEquivalent("foo(qix->0)", "foo(bar->0)");
  }

  @Test
  void not_implemented_tree() {
    JavaTree.NotImplementedTreeImpl notImplementedTree = new JavaTree.NotImplementedTreeImpl();
    assertThat(SyntacticEquivalence.areEquivalent(notImplementedTree, notImplementedTree)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(notImplementedTree, new JavaTree.NotImplementedTreeImpl())).isFalse();
  }

  @Test
  void extra_permissive_equivalence() {
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class B{}"))).isFalse();
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class B{}"), (l, r) -> false, true)).isFalse();
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class B{}"), (l, r) -> true, true)).isTrue();
  }

  @Test
  void extra_dismissive_equivalence() {
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class A{}"))).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class A{}"), (l, r) -> false, false)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class A{}"), (l, r) -> true, false)).isFalse();
  }

  @Test
  void test_semantic_equivalence() {
    CompilationUnitTree compilationUnitTree = compilationUnitTree(
      "class A{" +
      "  void m(String o) {}" +
      "  void m(Object o) {}" +
      "  void m(Object o1, Object o2) {}" +
      "  void f1(String o) {" +
      "    m(o);" +
      "  }" +
      "  void f2(Object o) {" +
      "    m(o);" +
      "  }" +
      "  void f3(Integer o) {" +
      "    m(o);" +
      "  }" +
      "  void f4(Object o) {" +
      "    m(o, o);" +
      "  }" +
      "}");
    List<Tree> members = ((ClassTree) compilationUnitTree.types().get(0)).members();
    List<StatementTree> f1Body = ((MethodTree) members.get(3)).block().body();
    List<StatementTree> f2Body = ((MethodTree) members.get(4)).block().body();
    List<StatementTree> f3Body = ((MethodTree) members.get(5)).block().body();
    List<StatementTree> f4Body = ((MethodTree) members.get(6)).block().body();

    assertThat(SyntacticEquivalence.areEquivalent(f1Body, f2Body)).isTrue();
    assertThat(SyntacticEquivalence.areSemanticallyEquivalent(f1Body, f2Body)).isFalse();
    assertThat(SyntacticEquivalence.areSemanticallyEquivalent(f2Body, f1Body)).isFalse();

    assertThat(SyntacticEquivalence.areEquivalent(f1Body, f3Body)).isTrue();
    assertThat(SyntacticEquivalence.areSemanticallyEquivalent(f1Body, f3Body)).isFalse();

    assertThat(SyntacticEquivalence.areEquivalent(f2Body, f3Body)).isTrue();
    assertThat(SyntacticEquivalence.areSemanticallyEquivalent(f2Body, f3Body)).isTrue();

    assertThat(SyntacticEquivalence.areSemanticallyEquivalent(f1Body, f4Body)).isFalse();
  }

  private ExpressionTree getMethodArg(StatementTree tree) {
    return ((MethodInvocationTree) ((ExpressionStatementTree) tree).expression()).arguments().get(0);
  }

  @Test
  void test_equivalence_with_variables() {
    CompilationUnitTree compilationUnitTree = compilationUnitTree(
      "class A{" +
        "  Object o;" +
        "  void m() {" +
        "    System.out.println(o);" +
        "    System.out.println(o);" +
        "    String o = \"hello\";" +
        "    System.out.println(o);" +
        "  }" +
        "}");
    List<Tree> members = ((ClassTree) compilationUnitTree.types().get(0)).members();
    List<StatementTree> mBody = ((MethodTree) members.get(1)).block().body();

    StatementTree print1 = mBody.get(0);
    StatementTree print2 = mBody.get(1);
    StatementTree print3 = mBody.get(3);

    ExpressionTree o1 = getMethodArg(print1);
    ExpressionTree o2 = getMethodArg(print2);
    ExpressionTree o3 = getMethodArg(print3);

    assertThat(SyntacticEquivalence.areEquivalent(o1, o2)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(o2, o3)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(o1, o3)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(print1, print2)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(print2, print3)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(print1, print3)).isTrue();

    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(o1, o2)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(o1, o3)).isFalse();
    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(o2, o3)).isFalse();

    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(print1, print2)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(print1, print3)).isFalse();
    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(print2, print3)).isFalse();

    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(o1, print1)).isFalse();
    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(print1, o1)).isFalse();
  }

  @Test
  void test_equivalence_with_variables_with_unknown_symbols() {
    CompilationUnitTree compilationUnitTree = compilationUnitTree(
      "class A{" +
        "  void m() {" +
        "    System.out.println(o);" +
        "    System.out.println(o);" +
        "    String o = \"hello\";" +
        "    System.out.println(o);" +
        "  }" +
        "}");
    List<Tree> members = ((ClassTree) compilationUnitTree.types().get(0)).members();
    List<StatementTree> mBody = ((MethodTree) members.get(0)).block().body();

    ExpressionTree o1 = getMethodArg(mBody.get(0));
    ExpressionTree o2 = getMethodArg(mBody.get(1));
    ExpressionTree o3 = getMethodArg(mBody.get(3));

    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(o1, o2)).isFalse();
    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(o1, o3)).isFalse();
    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(o2, o3)).isFalse();
    assertThat(SyntacticEquivalence.areEquivalentIncludingSameVariables(o3, o1)).isFalse();
  }

  @Test
  void test_semantic_equivalence_unknown_symbol() {
    CompilationUnitTree compilationUnitTree = compilationUnitTree(
      "class A{" +
        "  void m(Object o) {}" +
        "  void f1(Unknown o) {" +
        "    m(o);" +
        "  }" +
        "  void f2(Unknown o) {" +
        "    unknown(o);" +
        "  }" +
        "}");
    List<Tree> members = ((ClassTree) compilationUnitTree.types().get(0)).members();
    List<StatementTree> f1Body = ((MethodTree) members.get(1)).block().body();
    List<StatementTree> f2Body = ((MethodTree) members.get(2)).block().body();

    assertThat(SyntacticEquivalence.areEquivalent(f1Body, f2Body)).isFalse();
    assertThat(SyntacticEquivalence.areSemanticallyEquivalent(f1Body, f2Body)).isFalse();
    assertThat(SyntacticEquivalence.areSemanticallyEquivalent(f2Body, f1Body)).isFalse();
  }

  @Test
  void test_semantic_equivalence_in_for_header() {
    CompilationUnitTree compilationUnitTree = compilationUnitTree(
      "class A{" +
        "  void foo() {}" +
        "  void f1(Object o) {" +
        "    for (int i = 0; i < 1; i++) {};" +
        "  }" +
        "  void f2(Object o) {" +
        "    for (int i = 0; i < 1; foo()) {};" +
        "  }" +
        "}");
    List<Tree> members = ((ClassTree) compilationUnitTree.types().get(0)).members();
    List<StatementTree> f1Body = ((MethodTree) members.get(1)).block().body();
    List<StatementTree> f2Body = ((MethodTree) members.get(2)).block().body();

    assertThat(SyntacticEquivalence.areEquivalent(f1Body, f2Body)).isFalse();
    assertThat(SyntacticEquivalence.areSemanticallyEquivalent(f1Body, f2Body)).isFalse();
    assertThat(SyntacticEquivalence.areSemanticallyEquivalent(f2Body, f1Body)).isFalse();
  }

  private void assertAreEquivalent(String statement1, String statement2) {
    assertAreEquivalent(Collections.singletonList(statement1), Collections.singletonList(statement2));
  }

  private void assertAreEquivalent(List<String> statement1, List<String> statement2) {
    getAssertion(statement1, statement2).isTrue();
  }

  private void assertAreNotEquivalent(List<String> statement1, List<String> statement2) {
    getAssertion(statement1, statement2).isFalse();
  }

  private void assertAreNotEquivalent(String statement1, String statement2) {
    assertAreNotEquivalent(Collections.singletonList(statement1), Collections.singletonList(statement2));
  }

  private AbstractBooleanAssert<?> getAssertion(List<String> statement1, List<String> statement2) {
    ;
    CompilationUnitTree compilationUnitTree = compilationUnitTree("class A { void method1() { " + String.join(";", statement1) + ";} " +
      "void method2(){ " + String.join(";", statement2) + ";} }");
    ClassTree classTree = ((ClassTree) compilationUnitTree.types().get(0));
    assertThat(classTree.members()).hasSize(2);
    return assertThat(SyntacticEquivalence.areEquivalent(((MethodTree) classTree.members().get(0)).block().body(), ((MethodTree) classTree.members().get(1)).block().body()));
  }

  private CompilationUnitTree compilationUnitTree(String code) {
    return JParserTestUtils.parse(code);
  }
}
