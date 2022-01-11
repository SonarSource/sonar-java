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
package org.sonar.java.checks.tests;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.helpers.JParserTestUtils;
import org.sonar.java.checks.tests.AssertionTypesCheck.Argument;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class AssertionTypesCheckTest {

  @Test
  void test_junit4() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/AssertionTypesCheck_JUnit4.java"))
      .withCheck(new AssertionTypesCheck())
      .verifyIssues();
  }

  @Test
  void test_junit4_unknown_symbol_coverage() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/AssertionTypesCheck_JUnit4.java"))
      .withCheck(new AssertionTypesCheck())
      .verifyIssues();
  }

  @Test
  void test_junit5() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/AssertionTypesCheck_JUnit5.java"))
      .withCheck(new AssertionTypesCheck())
      .verifyIssues();
  }

  @Test
  void test_assertj() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/AssertionTypesCheck_AssertJ.java"))
      .withCheck(new AssertionTypesCheck())
      .verifyIssues();
  }

  @Test
  void expected_argument_types() {
    MethodTree method = JParserTestUtils.methodTree(JParserTestUtils.newCode(
      "void foo() {",
      "  bar('a', 'b', null);",
      "}",
      "void bar(char a, Object... b) {}"
    ));
    ExpressionStatementTree statement = (ExpressionStatementTree) method.block().body().get(0);
    MethodInvocationTree asList = (MethodInvocationTree) statement.expression();

    Argument firstArgument = new Argument(asList, 0);
    assertThat(firstArgument.isPrimitive()).isTrue();
    assertThat(firstArgument.type.isPrimitive()).isTrue();
    assertThat(firstArgument.expressionType.fullyQualifiedName()).isEqualTo("char");
    assertThat(firstArgument.type.fullyQualifiedName()).isEqualTo("char");

    Argument secondArgument = new Argument(asList, 1);
    assertThat(secondArgument.isPrimitive()).isTrue();
    assertThat(secondArgument.type.isPrimitive()).isFalse();
    assertThat(secondArgument.expressionType.fullyQualifiedName()).isEqualTo("char");
    assertThat(secondArgument.type.fullyQualifiedName()).isEqualTo("java.lang.Character");

    Argument thirdArgument = new Argument(asList, 2);
    assertThat(thirdArgument.isPrimitive()).isFalse();
    assertThat(thirdArgument.isNullLiteral()).isTrue();
  }

  @Test
  void arguments_of_unknown_method() {
    MethodTree method = JParserTestUtils.methodTree(JParserTestUtils.newCode(
      "void foo() {",
      "  foo('a');",
      "}"
    ));
    ExpressionStatementTree statement = (ExpressionStatementTree) method.block().body().get(0);
    MethodInvocationTree asList = (MethodInvocationTree) statement.expression();
    Argument firstArgument = new Argument(asList, 0);
    assertThat(firstArgument.isPrimitive()).isTrue();
    assertThat(firstArgument.type.isPrimitive()).isTrue();
    assertThat(firstArgument.expressionType.fullyQualifiedName()).isEqualTo("char");
    assertThat(firstArgument.type.fullyQualifiedName()).isEqualTo("char");
  }

  @Test
  void wrapper_type_with_invalid_primitive() {
    Type invalid_primitive = mock(Type.class);
    when(invalid_primitive.isPrimitive()).thenReturn(true);
    when(invalid_primitive.fullyQualifiedName()).thenReturn("invalid_name");
    Type type = AssertionTypesCheck.wrapperType(invalid_primitive);
    assertThat(type).isSameAs(invalid_primitive);
  }

}
