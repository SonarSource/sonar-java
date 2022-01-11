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

import static org.assertj.core.api.Assertions.assertThat;

class InternalPositionTest {

  @Test
  void construction() {
    InternalPosition position = new InternalPosition(42, 12);
    assertThat(position.line()).isEqualTo(42);
    assertThat(position.lineOffset()).isEqualTo(41);
    assertThat(position.column()).isEqualTo(12);
    assertThat(position.columnOffset()).isEqualTo(11);
    assertThat(position).isEqualTo(Position.at(42, 12));
  }

  @Test
  void to_string() {
    assertThat(Position.at(42, 12))
      .hasToString("42:12");
  }

  @Test
  void comparison() {
    assertThat(Position.at(1,1)).isEqualTo(Position.at(1, 1));
    assertThat(Position.at(1,1)).isNotEqualTo(Position.at(1, 2));
    assertThat(Position.at(1,1)).isNotEqualTo(Position.at(2, 1));

    Position first = Position.at(42, 11);
    Position second = Position.at(42, 12);

    assertThat(first)
      .isNotEqualTo(second)
      .isLessThan(second)
      .isLessThanOrEqualTo(second);

    assertThat(first.isBefore(second)).isTrue();
    assertThat(first.isAfter(second)).isFalse();

    assertThat(second)
      .isNotEqualTo(first)
      .isGreaterThan(first)
      .isGreaterThanOrEqualTo(first);

    assertThat(second.isBefore(first)).isFalse();
    assertThat(second.isAfter(first)).isTrue();

    first = Position.at(42, 11);
    second = Position.at(44, 5);

    assertThat(first)
      .isNotEqualTo(second)
      .isLessThan(second)
      .isLessThanOrEqualTo(second);

    assertThat(first.isBefore(second)).isTrue();
    assertThat(first.isAfter(second)).isFalse();
  }

  @Test
  void equals() {
    Position p1 = Position.at(42, 12);
    Position p2 = Position.at(43, 17);

    assertThat(p1.equals(Position.at(42, 12))).isTrue();
    assertThat(p1).hasSameHashCodeAs(Position.at(42, 12));

    assertThat(p1.equals(p1)).isTrue();
    assertThat(p1.equals(p2)).isFalse();
    assertThat(p1.equals(null)).isFalse();
    assertThat(p1.equals(new Object())).isFalse();
  }

}
