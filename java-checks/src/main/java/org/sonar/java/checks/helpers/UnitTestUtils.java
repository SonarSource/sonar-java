/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.HashSet;
import java.util.Set;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static java.util.Arrays.asList;

public final class UnitTestUtils {

  public static final MethodMatchers FAIL_METHOD_MATCHER = MethodMatchers.create()
    .ofTypes(
      // JUnit 5
      "org.junit.jupiter.api.Assertions",
      // JUnit 4
      "org.junit.Assert",
      // JUnit 3
      "junit.framework.Assert",
      // Fest assert
      "org.fest.assertions.Fail",
      // AssertJ
      "org.assertj.core.api.Fail"
    ).names("fail").withAnyParameters().build();

  public static final MethodMatchers ASSERTIONS_METHOD_MATCHER = MethodMatchers.or(
    // JUnit 3, 4 and 5
    MethodMatchers.create()
      .ofTypes("org.junit.Assert", "org.junit.jupiter.api.Assertions", "junit.framework.Assert", "junit.framework.TestCase")
      .name(name -> name.startsWith("assert"))
      .withAnyParameters()
      .build(),
    // Fest assert and AssertJ
    MethodMatchers.create()
      .ofTypes("org.assertj.core.api.Assertions", "org.fest.assertions.Assertions")
      .names("assertThat")
      .withAnyParameters()
      .build()
  );

  /**
   * Match when we are sure that the intention is to assert something, that will result in an AssertionError if the assertion fails.
   * The purpose is not to detect any assertion method (similar to S2699).
   */
  public static final MethodMatchers COMMON_ASSERTION_MATCHER = MethodMatchers.or(
    FAIL_METHOD_MATCHER, ASSERTIONS_METHOD_MATCHER
  );

  private static final Set<String> TEST_ANNOTATIONS = new HashSet<>(asList("org.junit.Test", "org.testng.annotations.Test"));
  private static final Set<String> JUNIT5_TEST_ANNOTATIONS = new HashSet<>(asList(
    "org.junit.jupiter.api.Test",
    "org.junit.jupiter.api.RepeatedTest",
    "org.junit.jupiter.api.TestFactory",
    "org.junit.jupiter.api.TestTemplate",
    "org.junit.jupiter.params.ParameterizedTest"));
  private static final String NESTED_ANNOTATION = "org.junit.jupiter.api.Nested";

  private UnitTestUtils() {
  }

  public static boolean hasNestedAnnotation(ClassTree tree) {
    SymbolMetadata metadata = tree.symbol().metadata();
    return metadata.isAnnotatedWith(NESTED_ANNOTATION);
  }

  public static boolean hasTestAnnotation(MethodTree tree) {
    SymbolMetadata symbolMetadata = tree.symbol().metadata();
    return TEST_ANNOTATIONS.stream().anyMatch(symbolMetadata::isAnnotatedWith) || hasJUnit5TestAnnotation(symbolMetadata);
  }

  public static boolean hasJUnit5TestAnnotation(MethodTree tree) {
    return hasJUnit5TestAnnotation(tree.symbol().metadata());
  }

  private static boolean hasJUnit5TestAnnotation(SymbolMetadata symbolMetadata) {
    return JUNIT5_TEST_ANNOTATIONS.stream().anyMatch(symbolMetadata::isAnnotatedWith);
  }

}
