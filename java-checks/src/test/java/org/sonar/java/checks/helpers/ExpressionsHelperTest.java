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

import javax.annotation.Nullable;
import org.junit.Test;

import java.lang.reflect.Constructor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpressionsHelperTest extends JavaParserHelper {

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = ExpressionsHelper.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void simpleAssignment() {
    String code = newCode( "int foo() {",
      "boolean a;",
      "a = true;",
      "return a;",
      "}");
    assertValueResolution(code, true);
  }

  @Test
  public void initializerAndAssignment() {
    String code = newCode( "int foo() {",
      "boolean a = false;",
      "a = true;",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  public void simpleInitializer() {
    String code = newCode( "int foo() {",
      "boolean a = true;",
      "return a;",
      "}");
    assertValueResolution(code, true);
  }

  @Test
  public void andAssignement() {
    String code = newCode( "int foo() {",
      "boolean a;",
      "a &= false;",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  public void selfAssigned() {
    String code = newCode( "int foo() {",
      "boolean a = a;",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  public void unknownValue() {
    String code = newCode( "int foo(boolean a) {",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  public void notAnIdentifier() {
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
  public void moreThanOneAssignment() {
    String code = newCode( "int foo() {",
      "boolean a;",
      "a = true;",
      "a = false;",
      "return a;",
      "}");
    assertValueResolution(code, null);
  }

  @Test
  public void variableSwapSOE() {
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
