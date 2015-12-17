/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Charsets;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.fest.assertions.Assertions.assertThat;

public class SyntaxTreeNameFinderTest {

  public static final ActionParser<Tree> parser = JavaParser.createParser(Charsets.UTF_8);

  private static MethodTree buildSyntaxTree(final String methodCode) {
    final CompilationUnitTree cut = (CompilationUnitTree) parser.parse("class A { " + methodCode + " }");
    return ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
  }

  @Test
  public void testClassCast() {
    final MethodTree tree = buildSyntaxTree("public boolean equals(Object obj) {((String) obj).toString();}");
    final BlockTree block = tree.block();
    final StatementTree statementTree = block.body().get(0);
    final MethodInvocationTree methodInvocation = (MethodInvocationTree) ((ExpressionStatementTree) statementTree).expression();
    assertThat(SyntaxTreeNameFinder.getName(methodInvocation)).isEqualTo("obj");
  }

  @Test
  public void testSwitch() {
    final MethodTree tree = buildSyntaxTree("public void test() {int i; switch (i) { case 0: break;}}");
    final BlockTree block = tree.block();
    final StatementTree statementTree = block.body().get(1);
    assertThat(SyntaxTreeNameFinder.getName(statementTree)).isEqualTo("i");
  }

  @Test
  public void testMethodInvocationOnIdentifier() {
    final MethodTree tree = buildSyntaxTree("public void test() {String s; s.length();}");
    final BlockTree block = tree.block();
    final StatementTree statementTree = block.body().get(1);
    assertThat(SyntaxTreeNameFinder.getName(statementTree)).isEqualTo("s");
  }

  @Test
  public void testMethodInvocationOnOtherInvocation() {
    final MethodTree tree = buildSyntaxTree("public void test() {int i = checkForNullMethod().length();}");
    final BlockTree block = tree.block();
    final StatementTree statementTree = block.body().get(0);
    assertThat(SyntaxTreeNameFinder.getName(statementTree)).isEqualTo("checkForNullMethod");
  }

  @Test
  public void testMemberSelectOnMethodInvocation() {
    final MethodTree tree = buildSyntaxTree("public void test() {int i = checkForNullMethod().length;}");
    final BlockTree block = tree.block();
    final StatementTree statementTree = block.body().get(0);
    assertThat(SyntaxTreeNameFinder.getName(statementTree)).isEqualTo("checkForNullMethod");
  }
}
