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

import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.checks.helpers.VariableTreeUtils.isClassField;
import static org.sonar.java.checks.helpers.VariableTreeUtils.isConstructorParameter;
import static org.sonar.java.checks.helpers.VariableTreeUtils.isSetterParameter;

class VariableTreeUtilsTest {

  private static final Set<String> CLASSES_WITH_SINGLE_FIELD = Set.of(
    "class A { private String field; }",
    "class A { private static String field; }",
    "class A { private final String field; }",
    "class A { private static final String FIELD; }");

  private static final Set<String> CLASSES_WITH_SINGLE_CONSTRUCTOR_METHOD = Set.of(
    "class A { public A(String field) {} }",
    "class A { public A(String field1, String field2) {} }");

  private static final Set<String> CLASSES_WITH_SINGLE_SETTER_METHOD = Set.of(
    "class A { public void setField(Object field) {} }");

  private static final Set<String> CLASSES_WITH_SINGLE_PARAMETERIZED_METHOD = Set.of(
    "class A { private void method(String param, Object field) {} }",
    "class A { private void setField(Object field) {} }",
    "class A { public static void method(String param, Object field) {} }",
    "class A { private <T> T method(Object object, Class<T> type) {} }");

  @Test
  void is_class_field() {
    assertOnClassField(classField -> assertTrue(isClassField(classField)));
    assertOnConstructor(constructorParameter -> assertFalse(isClassField(constructorParameter)));
    assertOnSetter(setterParameter -> assertFalse(isClassField(setterParameter)));
    assertOnMethod(methodParameter -> assertFalse(isClassField(methodParameter)));
  }

  @Test
  void is_constructor_parameter() {
    assertOnClassField(classField -> assertFalse(isConstructorParameter(classField)));
    assertOnConstructor(constructorParameter -> assertTrue(isConstructorParameter(constructorParameter)));
    assertOnMethod(methodParameter -> assertFalse(isConstructorParameter(methodParameter)));
    assertOnMethod(methodParameter -> assertFalse(isConstructorParameter(methodParameter)));
  }

  @Test
  void is_setter_parameter() {
    assertOnClassField(classField -> assertFalse(isSetterParameter(classField)));
    assertOnConstructor(constructorParameter -> assertFalse(isSetterParameter(constructorParameter)));
    assertOnSetter(setterParameter -> assertTrue(isSetterParameter(setterParameter)));
    assertOnMethod(methodParameter -> assertFalse(isSetterParameter(methodParameter)));
  }

  private static void assertOnClassField(Consumer<VariableTree> assertion) {
    CLASSES_WITH_SINGLE_FIELD.stream()
      .map(VariableTreeUtilsTest::parseField)
      .forEach(assertion);
  }

  private static void assertOnConstructor(Consumer<VariableTree> assertion) {
    assertOnMethod(CLASSES_WITH_SINGLE_CONSTRUCTOR_METHOD, assertion);
  }

  private static void assertOnSetter(Consumer<VariableTree> assertion) {
    assertOnMethod(CLASSES_WITH_SINGLE_SETTER_METHOD, assertion);
  }

  private static void assertOnMethod(Consumer<VariableTree> assertion) {
    assertOnMethod(CLASSES_WITH_SINGLE_PARAMETERIZED_METHOD, assertion);
  }

  private static void assertOnMethod(Set<String> methods, Consumer<VariableTree> assertion) {
    methods.stream()
      .map(VariableTreeUtilsTest::parseMethod)
      .flatMap(methodTree -> methodTree.parameters().stream())
      .forEach(assertion);
  }

  /**
   * Parse the given code of a class with a single method, as a MethodTree.
   */
  private static MethodTree parseMethod(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(code);
    ClassTree classTree = (ClassTree) compilationUnitTree.types().get(0);
    return (MethodTree) classTree.members().get(0);
  }

  /**
   * Parse the given code of a class with a single field, as a VariableTree.
   */
  private static VariableTree parseField(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(code);
    ClassTree classTree = (ClassTree) compilationUnitTree.types().get(0);
    return (VariableTree) classTree.members().get(0);
  }
}
