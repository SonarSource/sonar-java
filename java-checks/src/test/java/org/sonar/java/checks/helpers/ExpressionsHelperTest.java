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
package org.sonar.java.checks.helpers;

import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionsHelperTest extends JParserTestUtils {

  @Test
  void private_constructor() throws Exception {
    Constructor<ExpressionsHelper> constructor = ExpressionsHelper.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
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
  void andAssignement() {
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

  private <T> void assertValueResolution(String code, @Nullable T target) {
    MethodTree method = methodTree(code);
    IdentifierTree a = variableFromLastReturnStatement(method.block().body());
    Boolean value = ExpressionsHelper.getConstantValueAsBoolean(a).value();
    assertThat(value).isEqualTo(target);
  }
}
