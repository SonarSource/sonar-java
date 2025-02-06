/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.checks.helpers.UnitTestUtils.ASSERTJ_ASSERTION_METHODS_PREDICATE;


class UnitTestUtilsTest {

  @Test
  void testAssertJAssertionMethodPattern() {
    assertTrue(ASSERTJ_ASSERTION_METHODS_PREDICATE.test("returns"));
    assertTrue(ASSERTJ_ASSERTION_METHODS_PREDICATE.test("contains"));
    assertTrue(ASSERTJ_ASSERTION_METHODS_PREDICATE.test("containsAString"));
    assertTrue(ASSERTJ_ASSERTION_METHODS_PREDICATE.test("doesNotThrow"));
    assertTrue(ASSERTJ_ASSERTION_METHODS_PREDICATE.test("allMatchFoo"));
    assertFalse(ASSERTJ_ASSERTION_METHODS_PREDICATE.test("hasten"));
  }
}
