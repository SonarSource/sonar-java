/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

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
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class B{}"), (l,r) -> false, true)).isFalse();
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class B{}"), (l,r) -> true, true)).isTrue();
  }

  @Test
  void extra_dismissive_equivalence() {
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class A{}"))).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class A{}"), (l,r) -> false, false)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), compilationUnitTree("class A{}"), (l,r) -> true, false)).isFalse();
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
