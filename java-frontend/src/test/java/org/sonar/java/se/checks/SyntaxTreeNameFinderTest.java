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
package org.sonar.java.se.checks;

import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class SyntaxTreeNameFinderTest {

  public static ActionParser<Tree> parser = JavaParser.createParser();

  private static MethodTree buildSyntaxTree(String methodCode) {
    CompilationUnitTree cut = (CompilationUnitTree) parser.parse("class A { " + methodCode + " }");
    return ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
  }

  @Test
  public void testClassCast() {
    MethodTree tree = buildSyntaxTree("public boolean equals(Object obj) {((String) obj).length();}");
    BlockTree block = tree.block();
    StatementTree statementTree = block.body().get(0);
    MethodInvocationTree mit = (MethodInvocationTree) ((ExpressionStatementTree) statementTree).expression();
    MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
    assertThat(SyntaxTreeNameFinder.getName(mse)).isEqualTo("obj");
  }

  @Test
  public void testSwitch() {
    MethodTree tree = buildSyntaxTree("public void test() {int i; switch (i) { case 0: break;}}");
    BlockTree block = tree.block();
    StatementTree statementTree = block.body().get(1);
    assertThat(SyntaxTreeNameFinder.getName(statementTree)).isEqualTo("i");
  }

  @Test
  public void testMethodInvocationOnIdentifier() {
    MethodTree tree = buildSyntaxTree("public void test() {String s; s.length();}");
    BlockTree block = tree.block();
    StatementTree statementTree = block.body().get(1);
    assertThat(SyntaxTreeNameFinder.getName(statementTree)).isEqualTo("length");
  }

  @Test
  public void testMethodInvocationOnOtherInvocation() {
    MethodTree tree = buildSyntaxTree("public void test() {int i = checkForNullMethod().length();}");
    BlockTree block = tree.block();
    StatementTree statementTree = block.body().get(0);
    assertThat(SyntaxTreeNameFinder.getName(statementTree)).isEqualTo("length");
  }

  @Test
  public void testMemberSelectOnMethodInvocation() {
    MethodTree tree = buildSyntaxTree("public void test() {int i = checkForNullMethod().length;}");
    BlockTree block = tree.block();
    StatementTree statementTree = block.body().get(0);
    assertThat(SyntaxTreeNameFinder.getName(statementTree)).isEqualTo("checkForNullMethod");
  }

  @Test
  public void testCatchParameter() {
    MethodTree tree = buildSyntaxTree("public void test() {try {} catch (Exception ex) {} }");
    assertThat(SyntaxTreeNameFinder.getName(tree)).isEqualTo("ex");
  }

  @Test
  public void testVariableWithInitializer() {
    MethodTree tree = buildSyntaxTree("public void test() {int i = length;}");
    assertThat(SyntaxTreeNameFinder.getName(tree)).isEqualTo("length");
  }

  @Test
  public void testFieldAccess() {
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
