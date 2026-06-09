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
package org.sonar.java.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  void testFlatten() {
    assertThat(StringUtils.flatten()).isEmpty();
    assertThat(StringUtils.flatten("a", "b", "c")).containsExactly("a", "b", "c");

    assertThat(StringUtils.flatten(new String[] {"a", "b"}, "c")).containsExactly("a", "b", "c");
    assertThat(StringUtils.flatten("a", new String[] {"b", "c"})).containsExactly("a", "b", "c");

    assertThat(StringUtils.flatten(List.of("A", "B"), "a", new String[] {"b", "c"}))
      .containsExactly("A", "B", "a", "b", "c");
  }

  @Test
  void testFlatten_exceptions() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> StringUtils.flatten("a", 2))
      .withMessageContaining("Unsupported argument type:");

    assertThatIllegalArgumentException()
      .isThrownBy(() -> StringUtils.flatten((Object) new int[]{4, 5}))
      .withMessageContaining("Unsupported argument type:");

    var list = List.of("b", List.of("c", "d"));
    assertThatThrownBy(() -> StringUtils.flatten("a", list))
      .isInstanceOf(ArrayStoreException.class);
  }
}
