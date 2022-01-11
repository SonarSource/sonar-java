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
package org.sonar.java.model.location;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.location.Range;

import static org.assertj.core.api.Assertions.assertThat;

class InternalRangeTest {

  @Test
  void construction() {
    Position start = Position.at(42, 12);
    Position end = Position.at(43, 17);
    InternalRange range = new InternalRange(start, end);
    assertThat(range.start()).isSameAs(start);
    assertThat(range.end()).isSameAs(end);
    assertThat(range)
      .isEqualTo(Range.at(start, end));
  }

  @Test
  void construction_from_text() {
    Position start = Position.at(42, 12);
    assertThat(Range.at(start, "")).isEqualTo(Range.at(42, 12, 42, 12));
    assertThat(Range.at(start, "a")).isEqualTo(Range.at(42, 12, 42, 13));
    assertThat(Range.at(start, "a b c")).isEqualTo(Range.at(42, 12, 42, 17));
    assertThat(Range.at(start, "a\nb\n")).isEqualTo(Range.at(42, 12, 43, 2));
    assertThat(Range.at(start, "a\nb\nc")).isEqualTo(Range.at(42, 12, 44, 2));
    assertThat(Range.at(start, "a\r\nb\r\n")).isEqualTo(Range.at(42, 12, 43, 2));
  }

  @Test
  void to_string() {
    assertThat(Range.at(Position.at(42, 12), Position.at(43, 17)))
      .hasToString("(42:12)-(43:17)");
  }

  @Test
  void equals() {
    Position p1 = Position.at(42, 12);
    Position p2 = Position.at(43, 17);
    Position p3 = Position.at(44, 1);

    Range range_1_3 = Range.at(p1, p3);

    assertThat(range_1_3.equals(Range.at(p1, p3))).isTrue();
    assertThat(range_1_3).hasSameHashCodeAs(Range.at(p1, p3));

    assertThat(range_1_3.equals(range_1_3)).isTrue();
    assertThat(range_1_3.equals(Range.at(p1, p2))).isFalse();
    assertThat(range_1_3.equals(Range.at(p2, p3))).isFalse();
    assertThat(range_1_3.equals(null)).isFalse();
    assertThat(range_1_3.equals(new Object())).isFalse();

  }

}
