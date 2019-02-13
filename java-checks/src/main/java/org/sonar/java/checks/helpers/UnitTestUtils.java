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

import java.util.HashSet;
import java.util.Set;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.MethodTree;

import static java.util.Arrays.asList;

public final class UnitTestUtils {

  private static final Set<String> TEST_ANNOTATIONS = new HashSet<>(asList("org.junit.Test", "org.testng.annotations.Test"));
  private static final Set<String> JUNIT5_TEST_ANNOTATIONS = new HashSet<>(asList(
    "org.junit.jupiter.api.Test",
    "org.junit.jupiter.api.RepeatedTest",
    "org.junit.jupiter.api.TestFactory",
    "org.junit.jupiter.api.TestTemplate",
    "org.junit.jupiter.params.ParameterizedTest"));

  private UnitTestUtils() {
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
