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

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;
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

  @ParameterizedTest(name = "[{index}] {1}")
  @MethodSource("legitimateDeprecationTestCases")
  void hasLegitimateDeprecationDocumentation(String javadoc, String description, boolean expectedResult) {
    assertThat(DeprecatedCheckerHelper.hasLegitimateDeprecationDocumentation(javadoc))
      .as(description)
      .isEqualTo(expectedResult);
  }

  private static Stream<Arguments> legitimateDeprecationTestCases() {
    return Stream.of(
      // Migration guidance - should be legitimate (true)
      Arguments.of(
        """
        /**
         * @deprecated Use the new DynEnum instead
         */""",
        "Migration guidance with 'use instead'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated Please use {@link NewClass} instead
         */""",
        "Migration guidance with {@link}",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated Replaced by newMethod()
         */""",
        "Migration guidance with 'replaced by'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated See {@link NewApi#betterMethod}
         */""",
        "Migration guidance with 'see'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated Prefer using modernMethod()
         */""",
        "Migration guidance with 'prefer'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated Migrate to the new API
         */""",
        "Migration guidance with 'migrate to'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated Use newApi() instead.
         */""",
        "Migration guidance with 'use' and 'instead'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated Use new implementation
         */""",
        "Migration guidance with 'use' and 'new'",
        true
      ),

      // Removal timeline - should be legitimate (true)
      Arguments.of(
        """
        /**
         * @deprecated Will be removed in Tomcat 10.
         */""",
        "Removal timeline with 'will be removed in'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated Scheduled for removal in version 2.0
         */""",
        "Removal timeline with 'scheduled for removal'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated Removed in version 3.0
         */""",
        "Removal timeline with 'removed in version'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated To be removed in future releases
         */""",
        "Removal timeline with 'to be removed in'",
        true
      ),
      Arguments.of(
        """
        /**
         * @deprecated deprecated since version 1.5, use newMethod() instead
         */""",
        "Removal timeline with 'deprecated since' and migration guidance",
        true
      ),

      // Multiline with legitimate documentation - should be legitimate (true)
      Arguments.of(
        """
        /**
         * @deprecated Use {@code ImmutableSet.<T>of().iterator()} instead; or for
         *     Java 7 or later, {@link Collections#emptyIterator}. This method is
         *     scheduled for removal in May 2016.
         */""",
        "Multiline with migration guidance and removal timeline",
        true
      ),
      Arguments.of(
        """
        /**
         * Some description
         * @deprecated Use
         *     newApi() instead.
         */""",
        "Multiline with migration guidance spanning lines",
        true
      ),

      // Invalid/vague deprecation - should NOT be legitimate (false)
      Arguments.of(
        """
        /**
         * @deprecated This is old and not useful
         */""",
        "Vague deprecation without guidance",
        false
      ),
      Arguments.of(
        """
        /**
         * @deprecated
         */""",
        "Empty deprecation tag",
        false
      ),
      Arguments.of(
        """
        /**
         * @deprecated This method is outdated
         */""",
        "Generic deprecation message",
        false
      ),
      Arguments.of(
        """
        /**
         * @deprecated Do not use this
         */""",
        "No migration guidance or timeline",
        false
      ),
      Arguments.of(
        """
        /**
         * @deprecated Bad method
         */""",
        "No actionable information",
        false
      ),

      // Edge cases
      Arguments.of(
        "/** @deprecated use something */",
        "Lowercase 'use' without 'instead' or 'new'",
        false
      ),
      Arguments.of(
        "",
        "Empty string",
        false
      ),
      Arguments.of(
        "/** No deprecated tag */",
        "No @deprecated tag",
        false
      )
    );
  }

}
