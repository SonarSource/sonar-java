/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.LineUtils.splitLines;

class LineUtilsTest {

  @Test
  void split_lines() {
    assertThat(splitLines(""))
      .containsExactly("");

    assertThat(splitLines("  foo"))
      .containsExactly("  foo");

    assertThat(splitLines("\n"))
      .containsExactly("");

    assertThat(splitLines("foo\n"))
      .containsExactly("foo");

    assertThat(splitLines("\nfoo\n"))
      .containsExactly("", "foo");

    assertThat(splitLines("a\nb"))
      .containsExactly("a", "b");

    assertThat(splitLines("a\nb\n"))
      .containsExactly("a", "b");

    assertThat(splitLines("a\nb\nc"))
      .containsExactly("a", "b", "c");

    assertThat(splitLines("a\nb\nc\n"))
      .containsExactly("a", "b", "c");

    assertThat(splitLines("a\n\nb\r\rc\r\n\r\nd\n\r\n\r"))
      .containsExactly("a", "", "b", "", "c", "", "d", "", "");
  }


}
