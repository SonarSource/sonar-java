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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeprecatedCheckerHelperTest {

  @Test
  void getAnnotationAttributeValue() {
    assertValue("@Deprecated(forRemoval = true)", "forRemoval", true, Boolean.class);
    assertValue("@Deprecated(forRemoval = false)", "forRemoval", false, Boolean.class);
    assertValue("@Deprecated(value = \"test\")", "value", "test", String.class);
    assertValue("@Deprecated(since = 42)", "since", 42, Integer.class);
    assertValue("@Deprecated(\"Descr\")", "value", "Descr", String.class);
  }

  private <T> void assertValue(String annotationSourceCode, String attributeName, T expectedValue, Class<T> type) {
    var classTree = parseClass(annotationSourceCode + " class A {}");
    var annotation = DeprecatedCheckerHelper.deprecatedAnnotation(classTree);
    T value = DeprecatedCheckerHelper.getAnnotationAttributeValue(annotation, attributeName, type).orElse(null);
    assertEquals(expectedValue, value);
  }

  private ClassTree parseClass(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(code);
    return (ClassTree) compilationUnitTree.types().get(0);
  }

}
