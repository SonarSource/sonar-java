/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SyntacticEquivalenceTest {

  private final ActionParser p = JavaParser.createParser();

  @Test
  public void null_equivalence() throws Exception {
    assertThat(SyntacticEquivalence.areEquivalent((Tree) null, null)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(null, compilationUnitTree("class A{}"))).isFalse();
    assertThat(SyntacticEquivalence.areEquivalent(compilationUnitTree("class A{}"), null)).isFalse();

  }

  @Test
  public void statement_list_equivalence() {
    assertAreEquivalent(Lists.newArrayList("foo()", "bar()"), Lists.newArrayList("foo()", "bar()"));
    assertAreNotEquivalent(Lists.newArrayList("foo()", "bar"), Lists.newArrayList("foo()", "foo()"));
    assertAreNotEquivalent(Lists.newArrayList("foo()"), Lists.newArrayList("foo()", "foo()"));
  }

  @Test
  public void test_equivalence() {
    assertAreEquivalent("foo()", "foo()");
    assertAreNotEquivalent("foo()", "bar()");
    assertAreEquivalent("int a", "int a");
    assertAreEquivalent("List<String> a", "List<String> a");
    assertAreNotEquivalent("int a", "int b");
    assertAreNotEquivalent("", "int b");
    assertAreEquivalent("foo(a, b, c)", "foo(a, b, c)");
  }

  @Test
  public void lambda_equivalence() {
    assertAreEquivalent("foo(bar->0)", "foo(bar->0)");
    assertAreNotEquivalent("foo(qix->0)", "foo(bar->0)");
  }

  @Test
  public void not_implemented_tree() {
    JavaTree.NotImplementedTreeImpl notImplementedTree = new JavaTree.NotImplementedTreeImpl();
    assertThat(SyntacticEquivalence.areEquivalent(notImplementedTree, notImplementedTree)).isTrue();
    assertThat(SyntacticEquivalence.areEquivalent(notImplementedTree, new JavaTree.NotImplementedTreeImpl())).isFalse();
  }

  private void assertAreEquivalent(String statement1, String statement2) {
    assertAreEquivalent(Lists.newArrayList(statement1), Lists.newArrayList(statement2));
  }

  private void assertAreEquivalent(List<String> statement1, List<String> statement2) {
    getAssertion(statement1, statement2).isTrue();
  }

  private void assertAreNotEquivalent(List<String> statement1, List<String> statement2) {
    getAssertion(statement1, statement2).isFalse();
  }

  private void assertAreNotEquivalent(String statement1, String statement2) {
    assertAreNotEquivalent(Lists.newArrayList(statement1), Lists.newArrayList(statement2));
  }

  private AbstractBooleanAssert<?> getAssertion(List<String> statement1, List<String> statement2) {
    CompilationUnitTree compilationUnitTree = compilationUnitTree("class A { void method1() { " + Joiner.on(";").join(statement1) + ";} " +
      "void method2(){ " + Joiner.on(";").join(statement2) + ";} }");
    ClassTree classTree = ((ClassTree) compilationUnitTree.types().get(0));
    assertThat(classTree.members()).hasSize(2);
    return assertThat(SyntacticEquivalence.areEquivalent(((MethodTree) classTree.members().get(0)).block().body(), ((MethodTree) classTree.members().get(1)).block().body()));
  }

  private CompilationUnitTree compilationUnitTree(String code) {
    return (CompilationUnitTree) p.parse(code);
  }
}
