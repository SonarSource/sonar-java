/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static org.assertj.core.api.Assertions.assertThat;

public class ExpressionUtilsTest {

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
  public void test_skip_parenthesis() throws Exception {
    File file = new File("src/test/java/org/sonar/java/model/ExpressionUtilsTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser(StandardCharsets.UTF_8).parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(0);
    ExpressionTree parenthesis = ((ReturnStatementTree) methodTree.block().body().get(0)).expression();

    assertThat(parenthesis.is(Tree.Kind.PARENTHESIZED_EXPRESSION)).isTrue();
    ExpressionTree skipped = ExpressionUtils.skipParentheses(parenthesis);
    assertThat(skipped.is(Tree.Kind.CONDITIONAL_AND)).isTrue();
    assertThat(ExpressionUtils.skipParentheses(((BinaryExpressionTree) skipped).leftOperand()).is(Tree.Kind.IDENTIFIER)).isTrue();
  }

  @Test
  public void test_simple_assignments() throws Exception {
    File file = new File("src/test/java/org/sonar/java/model/ExpressionUtilsTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser(StandardCharsets.UTF_8).parse(file);
    MethodTree methodTree = (MethodTree) ((ClassTree) tree.types().get(0)).members().get(1);
    List<AssignmentExpressionTree> assignments = methodTree.block().body().stream()
      .filter(s -> s.is(Tree.Kind.EXPRESSION_STATEMENT))
      .map(ExpressionStatementTree.class::cast)
      .map(ExpressionStatementTree::expression)
      .filter(e -> e instanceof AssignmentExpressionTree)
      .map(AssignmentExpressionTree.class::cast)
      .collect(Collectors.toList());

    assertThat(assignments).hasSize(4);
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(0))).isTrue();
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(1))).isTrue();
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(2))).isFalse();
    assertThat(ExpressionUtils.isSimpleAssignment(assignments.get(3))).isFalse();
  }

  @Test
  public void private_constructor() throws Exception {
    assertThat(isFinal(ExpressionUtils.class.getModifiers())).isTrue();
    Constructor<ExpressionUtils> constructor = ExpressionUtils.class.getDeclaredConstructor();
    assertThat(isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }
}
