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
package org.sonar.java.model.expression;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class AssessableExpressionTreeTest {

  private final ClassTree classTree = parse();

  @Test
  void literals() {
    assertThat(resolveAsStrings("literals")).containsExactly("hello", null, null, null, null, null, null, null, null, null, null);
    assertThat(resolveAsInts("literals")).containsExactly(null, null, 43, +43, -43, null, null, null, null, 1000, null);
    assertThat(resolveAsBooleans("literals")).containsExactly(null, true, null, null, null, null, null, null, null, null, null);
  }

  @Test
  void identifiers() {
    assertThat(resolveAsStrings("identifiers")).containsExactly("abc", "abcdef", null, null, null, null);
    assertThat(resolveAsInts("identifiers")).containsExactly(null, null, 42, null, null, null);
    assertThat(resolveAsBooleans("identifiers")).containsExactly(null, null, null, null, true, false);
  }

  @Test
  void parentheses() {
    assertThat(resolveAsStrings("parentheses")).containsExactly("abc", null);
    assertThat(resolveAsInts("parentheses")).containsExactly(null, 42);
  }

  @Test
  void memberSelect() {
    assertThat(resolveAsStrings("memberSelect")).containsExactly("abc", null, null);
    assertThat(resolveAsBooleans("memberSelect")).containsExactly(null, true, false);
  }

  @Test
  void plus() {
    assertThat(resolveAsStrings("plus")).containsExactly("hello abc", null, null, "hello42", "42hello", null, null, null, null);
    assertThat(resolveAsInts("plus")).containsExactly(null, null, null, null, null, 43, null, null, null);
  }

  @Test
  void other() {
    assertThat(resolveAsStrings("other")).containsExactly(null, null);
  }

  @Test
  void uncompilable_expressions() {
    assertThat(expression("42 + 1").asConstant(Integer.class)).isPresent().contains(43);
    assertThat(expression("42 | 1").asConstant(Integer.class)).isPresent().contains(43);
    assertThat(expression("42L | 1").asConstant(Long.class)).isPresent().contains(43L);
    assertThat(expression("42 | 1L").asConstant(Long.class)).isPresent().contains(43L);
    assertThat(expression("42L | 1L").asConstant(Long.class)).isPresent().contains(43L);
    assertThat(expression("42 + true").asConstant()).isEmpty();
    assertThat(expression("42 | true").asConstant()).isEmpty();
    assertThat(expression("42L + true").asConstant()).isEmpty();
    assertThat(expression("42L | true").asConstant()).isEmpty();
    assertThat(expression("true + 42").asConstant()).isEmpty();
    assertThat(expression("true | 42").asConstant()).isEmpty();
    assertThat(expression("unknownVar").asConstant()).isEmpty();
    assertThat(expression("unknownVar | 42").asConstant()).isEmpty();
    assertThat(expression("42 | unknownVar").asConstant()).isEmpty();
  }

  private ExpressionTree expression(String expressionAsString) {
    CompilationUnitTree compilationUnit = JParserTestUtils.parse("class A { Object obj = " + expressionAsString + "; } ");
    ClassTree classTree = (ClassTree) compilationUnit.types().get(0);
    VariableTree field = (VariableTree) classTree.members().get(0);
    return field.initializer();
  }

  private ClassTree parse() {
    File file = new File("src/test/java/org/sonar/java/model/expression/ClassWithConstants.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    return (ClassTree) tree.types().get(0);
  }

  private List<String> resolveAsStrings(String methodName) {
    return constantValuesInMethod(methodName, expr -> expr.asConstant(String.class).orElse(null));
  }

  private List<Integer> resolveAsInts(String methodName) {
    return constantValuesInMethod(methodName, expr -> expr.asConstant(Integer.class).orElse(null));
  }

  private List<Boolean> resolveAsBooleans(String methodName) {
    return constantValuesInMethod(methodName, expr -> expr.asConstant(Boolean.class).orElse(null));
  }

  private <T> List<T> constantValuesInMethod(String methodName, Function<ExpressionTree, T> resolver) {
    MethodTree method = classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(m -> methodName.equals(m.simpleName().name()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("no method called " + methodName));
    return method.block().body().stream()
      .map(ExpressionStatementTree.class::cast)
      .map(ExpressionStatementTree::expression)
      .map(MethodInvocationTree.class::cast)
      .map(m -> m.arguments().iterator().next())
      .map(resolver)
      .collect(Collectors.toList());
  }

}
