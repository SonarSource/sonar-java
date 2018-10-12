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
package org.sonar.java.checks.helpers;

import org.junit.Test;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentifierUtilsTest extends JavaParserHelper {
  @Test
  public void simpleAssignment() {
    String code = newCode( "int foo() {",
      "boolean a;",
      "a = true;",
      "return a;",
      "}");
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(code, true);
  }

  @Test
  public void simpleInitializer() {
    String code = newCode( "int foo() {",
      "boolean a = true;",
      "return a;",
      "}");
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(code, true);
  }

  @Test
  public void andAssignement() {
    String code = newCode( "int foo() {",
      "boolean a = true;",
      "a &= false;",
      "return a;",
      "}");
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(code, null);
  }

  @Test
  public void selfAssigned() {
    String code = newCode( "int foo() {",
      "boolean a = a;",
      "return a;",
      "}");
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(code, null);
  }

  @Test
  public void unknownValue() {
    String code = newCode( "int foo(boolean a) {",
      "return a;",
      "}");
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(code, null);
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
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(code, null);
  }

  private <T> void assertThatLastReassignmentsOfReturnedVariableIsEqualTo(String code, T target) {
    MethodTree method = methodTree(code);
    IdentifierTree a = variableFromLastReturnStatement(method.block().body());
    Boolean value = IdentifierUtils.getValue(a, ConstantUtils::resolveAsBooleanConstant);
    assertThat(value).isEqualTo(target);
  }
}