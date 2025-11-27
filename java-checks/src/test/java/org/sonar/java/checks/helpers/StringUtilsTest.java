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

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {
  @Test
  void testIsEmpty() {
    assertThat(StringUtils.isEmpty(null)).isTrue();
    assertThat(StringUtils.isEmpty("")).isTrue();
    assertThat(StringUtils.isEmpty(" ")).isFalse();
    assertThat(StringUtils.isEmpty("abc")).isFalse();
  }

  @Test
  void testCountMatches() {
    assertThat(StringUtils.countMatches(null, "ab")).isZero();
    assertThat(StringUtils.countMatches("", "ab")).isZero();
    assertThat(StringUtils.countMatches("ab", null)).isZero();
    assertThat(StringUtils.countMatches("ab", "")).isZero();

    assertThat(StringUtils.countMatches("abababab", "cccc")).isZero();
    assertThat(StringUtils.countMatches("abababab", "ab")).isEqualTo(4);

    assertThat(StringUtils.countMatches("abaTaba", "aba")).isEqualTo(2);
    assertThat(StringUtils.countMatches("abababa", "aba")).isEqualTo(2);
  }
}
