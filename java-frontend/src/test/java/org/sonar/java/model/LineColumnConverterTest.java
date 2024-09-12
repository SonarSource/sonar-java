/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

class LineColumnConverterTest {

  @Test
  void test_to_pos() {
    String source = """

      ab

      c

      """;

    var converter = new LineColumnConverter(source);
    StringBuilder out = new StringBuilder();
    for (int i = 0; i <= source.length(); i++) {
      if (i != 0) {
        out.append(", ");
      }
      var pos = converter.toPos(i);
      out.append(i).append(":(").append(pos.line()).append(",").append(pos.columnOffset()).append(")");
    }
    assertThat(out).hasToString(
      // absoluteIndex:(line, columnOffset), ...
      "0:(1,0), 1:(2,0), 2:(2,1), 3:(2,2), 4:(3,0), 5:(4,0), 6:(4,1), 7:(5,0), 8:(6,0)");
  }

}
