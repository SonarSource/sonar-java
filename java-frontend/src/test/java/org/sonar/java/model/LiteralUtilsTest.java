/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.sonar.sslr.api.typed.ActionParser;
import java.lang.reflect.Constructor;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class LiteralUtilsTest {

  private final ActionParser<Tree> p = JavaParser.createParser();

  private static List<VariableTree> variables;

  @BeforeClass
  public static void setUp() {
    File file = new File("src/test/java/org/sonar/java/model/LiteralUtilsTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    ClassTree classTree = (ClassTree) tree.types().get(0);
    variables = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .collect(Collectors.toList());
  }

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = LiteralUtils.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    // call for coverage
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void test_int_and_long_value() throws Exception {
    Integer[] expectedIntegerValues = {42, -7, 3, null, null, 0xff, 0b0100, 5678, 0xFF, 0b1100110};
    Long[] expectedLongValues = {42L, 42L, -7L, -7L, +3L, +3L, null, null, 0xFFL, null, null, null,
      Long.MAX_VALUE, Long.MAX_VALUE, 0b11010010_01101001_10010100_10010010L, 10010L, 0xFFL, 0b1100110L};
    int i = 0;
    int j = 0;

    for (VariableTree variableTree : variables) {
      if (variableTree.simpleName().name().startsWith("x")) {
        assertThat(LiteralUtils.intLiteralValue(variableTree.initializer())).isEqualTo(expectedIntegerValues[i++]);
      } else if (variableTree.simpleName().name().startsWith("y")) {
        assertThat(LiteralUtils.longLiteralValue(variableTree.initializer())).isEqualTo(expectedLongValues[j++]);
      }
    }
  }

  @Test
  public void testTrimLongSuffix() throws Exception {
    assertThat(LiteralUtils.trimLongSuffix("")).isEqualTo("");
    String longValue = "12345";
    assertThat(LiteralUtils.trimLongSuffix(longValue)).isEqualTo(longValue);
    assertThat(LiteralUtils.trimLongSuffix(longValue + "l")).isEqualTo(longValue);
    assertThat(LiteralUtils.trimLongSuffix(longValue + "L")).isEqualTo(longValue);
  }

  @Test
  public void testEmptyString() {
    boolean[] expectedStringEmptyResult = {true, false, false, false};
    int i = 0;
    for (VariableTree variableTree : variables) {
      if (variableTree.simpleName().name().startsWith("s")) {
        assertThat(LiteralUtils.isEmptyString(variableTree.initializer())).isEqualTo(expectedStringEmptyResult[i++]);
      }
    }
    Optional<VariableTree> nonStringVariable = variables.stream().filter(v -> v.simpleName().name().startsWith("x")).findFirst();
    assertThat(nonStringVariable).isPresent();
    assertThat(LiteralUtils.isEmptyString(nonStringVariable.get().initializer())).isFalse();
  }

  @Test
  public void testTrimQuotes() {
    assertThat(LiteralUtils.trimQuotes("\"test\"")).isEqualTo("test");
  }

  @Test
  public void hasValue_withNonStringLiteral_returnsFalse() {
    ExpressionTree tree = getFirstExpression("void foo(java.util.Properties props){ props.setProperty(\"myKey\", \"myValue\"); }");
    boolean result = LiteralUtils.hasValue(tree, "expected");
    assertThat(result).isFalse();
  }

  @Test
  public void hasValue_withOtherValue_returnsFalse() {
    LiteralTree tree = (LiteralTree) getReturnExpression("void foo(){ return \"other than expected\"; }");
    boolean result = LiteralUtils.hasValue(tree, "expected");
    assertThat(result).isFalse();
  }

  @Test
  public void hasValue_withExpectedValue_returnsTrue() {
    LiteralTree tree = (LiteralTree) getReturnExpression("void foo(){ return \"expected\"; }");
    boolean result = LiteralUtils.hasValue(tree, "expected");
    assertThat(result).isTrue();
  }

  @Test
  public void is_0xff() {
    ExpressionTree tree = getReturnExpression("int foo() { return 0xFF; }");
    assertThat(LiteralUtils.is0xff(tree)).isTrue();
    tree = getReturnExpression("int foo() { return 0x01; }");
    assertThat(LiteralUtils.is0xff(tree)).isFalse();
    tree = getReturnExpression("int foo() { return 0Xff; }");
    assertThat(LiteralUtils.is0xff(tree)).isTrue();
    tree = getReturnExpression("char foo() { return '0'; }");
    assertThat(LiteralUtils.is0xff(tree)).isFalse();
  }

  @Test
  public void isTrue_withNonBooleanLiteral_returnsFalse() {
    ExpressionTree tree = getFirstExpression("void foo(java.util.Properties props){ props.setProperty(\"myKey\", \"myValue\"); }");
    assertThat(LiteralUtils.isTrue(tree)).isFalse();
  }

  @Test
  public void isFalse_withNonBooleanLiteral_returnsFalse() {
    ExpressionTree tree = getFirstExpression("void foo(java.util.Properties props){ props.setProperty(\"myKey\", \"myValue\"); }");
    assertThat(LiteralUtils.isFalse(tree)).isFalse();
  }

  @Test
  public void isTrue_withFalseValue_returnsFalse() {
    LiteralTree falseTree = (LiteralTree) getReturnExpression("void foo(){ return false; }");
    assertThat(LiteralUtils.isTrue(falseTree)).isFalse();
  }

  @Test
  public void isFalse_withTrueValue_returnsFalse() {
    LiteralTree trueTree = (LiteralTree) getReturnExpression("void foo(){ return true; }");
    assertThat(LiteralUtils.isFalse(trueTree)).isFalse();
  }

  @Test
  public void isTrue_withExpectedValue_returnsTrue() {
    LiteralTree trueTree = (LiteralTree) getReturnExpression("void foo(){ return true; }");
    assertThat(LiteralUtils.isTrue(trueTree)).isTrue();
  }

  @Test
  public void isFalse_withExpectedValue_returnsTrue() {
    LiteralTree falseTree = (LiteralTree) getReturnExpression("void foo(){ return false; }");
    assertThat(LiteralUtils.isFalse(falseTree)).isTrue();
  }

  private ExpressionTree getFirstExpression(String code) {
    ClassTree firstType = getClassTree(code);
    StatementTree firstStatement = ((MethodTree) firstType.members().get(0)).block().body().get(0);
    return ((ExpressionStatementTree) firstStatement).expression();
  }

  private ExpressionTree getReturnExpression(String code) {
    ClassTree firstType = getClassTree(code);
    ReturnStatementTree returnExpression = (ReturnStatementTree) ((MethodTree) firstType.members().get(0)).block().body().get(0);
    return returnExpression.expression();
  }

  private ClassTree getClassTree(String code) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse("class A { " + code + "}");
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    return (ClassTree) compilationUnitTree.types().get(0);
  }
}
