/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.se.checks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.java.se.utils.JParserTestUtils;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;

import static org.assertj.core.api.Assertions.assertThat;

class SyntaxTreeNameFinderTest {

  private static MethodTree buildSyntaxTree(String methodCode) {
    CompilationUnitTree cut = JParserTestUtils.parse("class A { " + methodCode + " }");
    return ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
  }

  @Test
  void testClassCast() {
    MethodTree tree = buildSyntaxTree("public boolean equals(Object obj) {((String) obj).length();}");
    BlockTree block = tree.block();
    StatementTree statementTree = block.body().get(0);
    MethodInvocationTree mit = (MethodInvocationTree) ((ExpressionStatementTree) statementTree).expression();
    MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
    assertThat(SyntaxTreeNameFinder.getName(mse)).isEqualTo("obj");
  }

  @ParameterizedTest(name="[{index}] Name of method content {1} should be {2} in method code: {0}")
  @CsvSource({
    "public void test() {int i; switch (i) { case 0: break;}}, 1, i",
    "public void test() {String s; s.length();}, 1, length",
    "public void test() {int i = checkForNullMethod().length();}, 0, length",
    "public void test() {int i = checkForNullMethod().length;}, 0, checkForNullMethod",
  })
  void testNameOfElementOfBlock(String methodCode, int blockPosition, String name) {
    MethodTree tree = buildSyntaxTree(methodCode);
    BlockTree block = tree.block();
    StatementTree statementTree = block.body().get(blockPosition);
    assertThat(SyntaxTreeNameFinder.getName(statementTree)).isEqualTo(name);
  }

  @Test
  void testCatchParameter() {
    MethodTree tree = buildSyntaxTree("public void test() {try {} catch (Exception ex) {} }");
    assertThat(SyntaxTreeNameFinder.getName(tree)).isEqualTo("ex");
  }

  @Test
  void testVariableWithInitializer() {
    MethodTree tree = buildSyntaxTree("public void test() {int i = length;}");
    assertThat(SyntaxTreeNameFinder.getName(tree)).isEqualTo("length");
  }

  @Test
  void testFieldAccess() {
    MethodTree tree = buildSyntaxTree("public void test() {this.field = value;} Object field;");
    BlockTree block = tree.block();
    AssignmentExpressionTree assignmentTree = (AssignmentExpressionTree) ((ExpressionStatementTree) block.body().get(0)).expression();
    assertThat(SyntaxTreeNameFinder.getName(assignmentTree.variable())).isEqualTo("field");

    tree = buildSyntaxTree("public void test() {super.field = value;}");
    block = tree.block();
    assignmentTree = (AssignmentExpressionTree) ((ExpressionStatementTree) block.body().get(0)).expression();
    assertThat(SyntaxTreeNameFinder.getName(assignmentTree.variable())).isEqualTo("field");

    tree = buildSyntaxTree("public void test() {A.field = value;}");
    block = tree.block();
    assignmentTree = (AssignmentExpressionTree) ((ExpressionStatementTree) block.body().get(0)).expression();
    assertThat(SyntaxTreeNameFinder.getName(assignmentTree.variable())).isEqualTo("A");

    tree = buildSyntaxTree("public void test() {foo().field = value;}");
    block = tree.block();
    assignmentTree = (AssignmentExpressionTree) ((ExpressionStatementTree) block.body().get(0)).expression();
    assertThat(SyntaxTreeNameFinder.getName(assignmentTree.variable())).isEqualTo("foo");
  }
}
