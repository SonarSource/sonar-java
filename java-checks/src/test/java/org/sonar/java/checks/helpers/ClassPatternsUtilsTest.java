/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassPatternsUtilsTest {

  @Test
  void is_private_inner_class() {
    assertFalse(ClassPatternsUtils.isPrivateInnerClass(parseClass("class A { public static void main(String[] args){} }")));
    assertTrue(ClassPatternsUtils.isPrivateInnerClass(parseInnerClass("class A { public static void main(String[] args){} private class B {}}")));
  }

  @Test
  void is_utility_class() {
    assertFalse(ClassPatternsUtils.isUtilityClass(parseClass("class A { public static void main(String[] args){} }")));
    assertTrue(ClassPatternsUtils.isUtilityClass(parseClass("class A { public static void a(){} private static void b(){}}")));
    assertFalse(ClassPatternsUtils.isUtilityClass(parseClass("enum A {}")));
    assertFalse(ClassPatternsUtils.isUtilityClass(parseClass("class A { enum B {}}")));
    assertFalse(ClassPatternsUtils.isUtilityClass(parseClass("class A { interface B {}}")));
    assertFalse(ClassPatternsUtils.isUtilityClass(parseClass("class A { @interface B {}}")));
    assertFalse(ClassPatternsUtils.isUtilityClass(parseClass("class A { public static void a(){} private static void b(){} private void c(){}}")));
  }

  private ClassTree parseClass(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(code);
    return (ClassTree) compilationUnitTree.types().get(0);
  }

  private ClassTree parseInnerClass(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(code);
    ClassTree mainClass = (ClassTree) compilationUnitTree.types().get(0);
    return (ClassTree) mainClass.members().get(1);
  }
}
