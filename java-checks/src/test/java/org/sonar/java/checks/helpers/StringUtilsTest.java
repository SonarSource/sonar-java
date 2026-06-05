/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

  @Test
  void testStringArgs() {
    assertThat(StringUtils.stringArgs()).isEmpty();
    assertThat(StringUtils.stringArgs("a", "b", "c")).containsExactly("a", "b", "c");

    assertThat(StringUtils.stringArgs(new String[] {"a", "b"}, "c")).containsExactly("a", "b", "c");
    assertThat(StringUtils.stringArgs("a", new String[] {"b", "c"})).containsExactly("a", "b", "c");

    assertThat(StringUtils.stringArgs(List.of("A", "B"), "a", new String[] {"b", "c"}))
      .containsExactly("A", "B", "a", "b", "c");
  }

  @Test
  void testStringArgs_exceptions() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> StringUtils.stringArgs("a", 2))
      .withMessageContaining("Unsupported argument type:");

    assertThatIllegalArgumentException()
      .isThrownBy(() -> StringUtils.stringArgs(new int[]{4, 5}))
      .withMessageContaining("Unsupported argument type:");

    var list = List.of("b", List.of("c", "d"));
    assertThatIllegalArgumentException()
      .isThrownBy(() -> StringUtils.stringArgs("a", list))
      .withMessageContaining("Unsupported collection element type:");
  }
}
