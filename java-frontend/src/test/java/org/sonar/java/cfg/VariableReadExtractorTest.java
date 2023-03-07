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
package org.sonar.java.cfg;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;

import static org.assertj.core.api.Assertions.assertThat;

class VariableReadExtractorTest {

  private static MethodTree buildMethodTree(String methodCode) {
    CompilationUnitTree cut = JParserTestUtils.parse("class A { int field1; int field2; " + methodCode + " }");
    return (MethodTree) ((ClassTree) cut.types().get(0)).members().get(2);
  }

  @ParameterizedTest(name = "[{index}] With includeFields={1}, {2} read variable should be extracted from method code: {0}]")
  @MethodSource("provideExtractionTest")
  void should_extract_correctly(String methodCode, boolean includeFields, int size) {
    MethodTree methodTree = buildMethodTree(methodCode);
    StatementTree statementTree = methodTree.block().body().get(0);
    VariableReadExtractor extractor = new VariableReadExtractor(methodTree.symbol(), includeFields);
    statementTree.accept(extractor);
    assertThat(extractor.usedVariables()).hasSize(size);
  }

  private static Stream<Arguments> provideExtractionTest() {
    return Stream.of(
      // should extract local variable read
      Arguments.of("void foo(boolean a) { new Object() { void foo() { System.out.println(a);} };  }", false, 1),
      // should not extract variable declared, only "a" should be detected
      Arguments.of("void foo(boolean a) { boolean b = a; }", true, 1),
      // should not extract fields read: local variable "a" and fields "field1" and "field2"
      Arguments.of("void foo(int a) { bar(p -> { System.out.println(a + field1); foo(this.field2); }); } void bar(java.util.function.Consumer<Object> consumer) {}"
        , true, 3),
      // should not extract fields written: local variable "a" and field "field"
      Arguments.of("void foo(boolean a) { new Object() { void foo() { new A().field1 = 0; a = false;} };  }", true, 0),
      // should extract local variable read
      Arguments.of("void foo(boolean a) { new Object() { void foo() { foo(a); bar(a); } }; }", false, 1)
    );
  }

  @Test
  void should_not_extract_local_vars_written() throws Exception {
    MethodTree methodTree = buildMethodTree("void foo(boolean a) { new Object() { void foo() { new A().field1 = 0; a = false;} };  }");
    StatementTree statementTree = methodTree.block().body().get(0);
    VariableReadExtractor extractor = new VariableReadExtractor(methodTree.symbol(), false);
    statementTree.accept(extractor);
    assertThat(extractor.usedVariables()).isEmpty();
    methodTree = buildMethodTree("void foo(boolean a) { new Object() { void foo() { a = !a;} };  }");
    statementTree = methodTree.block().body().get(0);
    extractor = new VariableReadExtractor(methodTree.symbol(), false);
    statementTree.accept(extractor);
    assertThat(extractor.usedVariables()).hasSize(1);
  }
}
