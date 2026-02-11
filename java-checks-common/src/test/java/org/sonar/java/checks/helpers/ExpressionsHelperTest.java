/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import java.lang.reflect.Constructor;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpressionsHelperTest extends JParserTestUtils {

  @Test
  void private_constructor() throws Exception {
    Constructor<ExpressionsHelper> constructor = ExpressionsHelper.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void concatenate_null() {
    assertThat(ExpressionsHelper.concatenate(null)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "import A;, A",
    "import java.util.List;, java.util.List",
    "import java.util.regex.Pattern;, java.util.regex.Pattern",
    "import java.util.*;, java.util.*",
    "import static java.util.Collections.emptyList;, java.util.Collections.emptyList",
    "import static java.util.Collections.*;, java.util.Collections.*",
    "import module java.base;, java.base"
  })
  void concatenate(String importStatement, String expected) {
    String code = importStatement + "\nclass C {}";
    ExpressionTree qualifiedId = getImportQualifiedIdentifier(code, 0);
    assertThat(ExpressionsHelper.concatenate(qualifiedId)).isEqualTo(expected);
  }

  private ExpressionTree getImportQualifiedIdentifier(String code, int importIndex) {
    CompilationUnitTree compilationUnit = parse(code);
    ImportTree importTree = (ImportTree) compilationUnit.imports().get(importIndex);
    return (ExpressionTree) importTree.qualifiedIdentifier();
  }

  @Test
  void simpleAssignment() {
    String code = newCode( "int foo() {",
      "boolean a;",
      "a = true;",
      "return a;",
      "}");
    assertValueResolution(code, true);
  }

  @Test
  void initializerAndAssignment() {
    String code = newCode( "int foo() {",
      "boolean a = false;",
      "a = true;",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  void simpleInitializer() {
    String code = newCode( "int foo() {",
      "boolean a = true;",
      "return a;",
      "}");
    assertValueResolution(code, true);
  }

  @Test
  void andAssignment() {
    String code = newCode( "int foo() {",
      "boolean a;",
      "a &= false;",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  void selfAssigned() {
    String code = newCode( "int foo() {",
      "boolean a = a;",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  void unknownValue() {
    String code = newCode( "int foo(boolean a) {",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  void notAnIdentifier() {
    String code = newCode( "int foo() {",
      "boolean a = bar();",
      "return a;",
      "}",
      "boolean bar() {",
      "return true;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  void moreThanOneAssignment() {
    String code = newCode( "int foo() {",
      "boolean a;",
      "a = true;",
      "a = false;",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  void variableSwapSOE() {
    String code = newCode("String foo(String a, String b) {",
      "String c = a;",
      "a = b;",
      "b = c;",
      "return a;}");
    assertValueResolution(code, null);
  }

  @Test
  void isNotReassignedTest(){
    Symbol.VariableSymbol symbol = mock(Symbol.VariableSymbol.class);

    when(symbol.isFinal()).thenReturn(true);
    assertThat(ExpressionsHelper.isNotReassigned(symbol)).isTrue();

    when(symbol.isFinal()).thenReturn(false);
    assertThat(ExpressionsHelper.isNotReassigned(symbol)).isFalse();

    when(symbol.isVariableSymbol()).thenReturn(true);
    assertThat(ExpressionsHelper.isNotReassigned(symbol)).isFalse();

    when(symbol.isEffectivelyFinal()).thenReturn(true);
    assertThat(ExpressionsHelper.isNotReassigned(symbol)).isTrue();
  }

  private <T> void assertValueResolution(String code, @Nullable T target) {
    MethodTree method = methodTree(code);
    IdentifierTree a = variableFromLastReturnStatement(method.block().body());
    Boolean value = ExpressionsHelper.getConstantValueAsBoolean(a).value();
    assertThat(value).isEqualTo(target);
  }

  @Test
  void isNonSerializable_nonSerializable() {
    String code = newCode(
      "static class C {}",
      "private C c;",
      "void f() {",
      "  System.out.println(c);",
      "}"
    );
    ExpressionTree expr = getCallArgument(code);
    assertThat(ExpressionsHelper.isNotSerializable(expr)).isTrue();
  }

  @Test
  void isNonSerializable_javaIoSerializable() {
    String code = newCode(
      "static class C implements java.io.Serializable {}",
      "private C c;",
      "void f() {",
      "  System.out.println(c);",
      "}"
    );
    ExpressionTree expr = getCallArgument(code);
    assertThat(ExpressionsHelper.isNotSerializable(expr)).isFalse();
  }

  @Test
  void isNonSerializable_missingImportSerializable() {
    String code = newCode(
      "static class C implements Serializable {}",
      "private C c;",
      "void f() {",
      "  System.out.println(c);",
      "}"
    );
    ExpressionTree expr = getCallArgument(code);
    // We want "false" in case we cannot resolve implemented interfaces,
    // to avoid FPs in the checks that use this helper.
    assertThat(ExpressionsHelper.isNotSerializable(expr)).isFalse();
  }

  /** Returns the {@code c} argument to {@code System.out.println(c)}. */
  private static ExpressionTree getCallArgument(String code) {
    var methodTree = (MethodTree) classTree(code).members().get(2);
    var exprStmtTree = (ExpressionStatementTree) methodTree.block().body().get(0);
    var methodInvocationTree =  (MethodInvocationTree) exprStmtTree.expression();
    return methodInvocationTree.arguments().get(0);
  }
}
