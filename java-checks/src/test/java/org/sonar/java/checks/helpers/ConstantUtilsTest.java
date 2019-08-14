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
package org.sonar.java.checks.helpers;

import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.helpers.ConstantUtils.resolveAsIntConstant;

public class ConstantUtilsTest {

  private final ActionParser<Tree> parser = JavaParser.createParser();
  private final ClassTree classTree = parse();

  @Test
  public void literals() {
    assertThat(resolveAsStrings("literals")).containsExactly("hello", null, null, null, null, null, null, null, null, null, null);
    assertThat(resolveAsInts("literals")).containsExactly(null, null, 43, +43, -43, null, null, null, null, 1000, null);
    assertThat(resolveAsLongs("literals")).containsExactly(null, null, 43L, +43L, -43L, 77L, null, null, null, 1000L, 0x99567L);
    assertThat(resolveAsBooleans("literals")).containsExactly(null, true, null, null, null, null, null, null, null, null, null);
  }

  @Test
  public void identifiers() {
    assertThat(resolveAsStrings("identifiers")).containsExactly("abc", "abcdef", null, null, null, null);
    assertThat(resolveAsInts("identifiers")).containsExactly(null, null, 42, null, null, null);
    assertThat(resolveAsLongs("identifiers")).containsExactly(null, null, 42L, 99L, null, null);
    assertThat(resolveAsBooleans("identifiers")).containsExactly(null, null, null, null, true, false);
  }

  @Test
  public void parentheses() {
    assertThat(resolveAsStrings("parentheses")).containsExactly("abc", null);
    assertThat(resolveAsInts("parentheses")).containsExactly(null, 42);
  }

  @Test
  public void memberSelect() {
    assertThat(resolveAsStrings("memberSelect")).containsExactly("abc", null, null);
    assertThat(resolveAsBooleans("memberSelect")).containsExactly(null, true, false);
  }

  @Test
  public void plus() {
    assertThat(resolveAsStrings("plus")).containsExactly("hello abc", null, null, "hello42", "42hello", null, null, null, null);
    assertThat(resolveAsInts("plus")).containsExactly(null, null, null, null, null, 43, null, null, null);
    assertThat(resolveAsLongs("plus")).containsExactly(null, null, null, null, null, 43L, 100L, 101L, 102L);
  }

  @Test
  public void other() {
    assertThat(resolveAsStrings("other")).containsExactly(null, null);
  }

  @Test
  public void uncompilable_expressions() {
    assertThat(resolveAsIntConstant(expression("42 + 1"))).isEqualTo(43);
    assertThat(resolveAsIntConstant(expression("42 + true"))).isNull();
    assertThat(resolveAsIntConstant(expression("42L + true"))).isNull();
    assertThat(resolveAsIntConstant(expression("true + 42"))).isNull();
    assertThat(resolveAsIntConstant(expression("unknownVar"))).isNull();
  }

  private ExpressionTree expression(String expressionAsString) {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) parser.parse("class A { Object obj = " + expressionAsString + "; } ");
    ClassTree classTree = (ClassTree) compilationUnit.types().get(0);
    VariableTree field = (VariableTree) classTree.members().get(0);
    return field.initializer();
  }

  private ClassTree parse() {
    File file = new File("src/test/java/org/sonar/java/checks/helpers/ClassWithConstants.java");
    CompilationUnitTree tree = (CompilationUnitTree) parser.parse(file);
    SemanticModel.createFor(tree, new SquidClassLoader(Collections.singletonList(new File("target/test-classes"))));
    return (ClassTree) tree.types().get(0);
  }

  private List<String> resolveAsStrings(String methodName) {
    return constantValuesInMethod(methodName, ConstantUtils::resolveAsStringConstant);
  }

  private List<Integer> resolveAsInts(String methodName) {
    return constantValuesInMethod(methodName, ConstantUtils::resolveAsIntConstant);
  }

  private List<Long> resolveAsLongs(String methodName) {
    return constantValuesInMethod(methodName, ConstantUtils::resolveAsLongConstant);
  }

  private List<Boolean> resolveAsBooleans(String methodName) {
    return constantValuesInMethod(methodName, ConstantUtils::resolveAsBooleanConstant);
  }

  private <T> List<T> constantValuesInMethod(String methodName, Function<ExpressionTree,T> resolver) {
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
