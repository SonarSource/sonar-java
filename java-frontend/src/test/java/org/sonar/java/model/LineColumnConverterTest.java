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
package org.sonar.java.model;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.location.Position;

import static org.assertj.core.api.Assertions.assertThat;

class LineColumnConverterTest {

  @Test
  void test_to_pos_linux_line_ending() {
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

  @Test
  void test_to_pos_windows_line_ending() {
    String source = "\r\nab\r\n\r\nc\r\n\r\n";

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
      "0:(1,0), 1:(1,1), 2:(2,0), 3:(2,1), 4:(2,2), 5:(2,3), 6:(3,0), 7:(3,1), 8:(4,0), 9:(4,1), 10:(4,2), 11:(5,0), 12:(5,1), 13:(6,0)");
  }

  @Test
  void test_to_pos_with_a_lot_of_lines() {
    String source = "a\n".repeat(200);
    var converter = new LineColumnConverter(source);
    var out = new ArrayList<String>();
    for (int i = 0; i <= source.length(); i++) {
      var pos = converter.toPos(i);
      out.add(i + ":(" + pos.line() + "," + pos.columnOffset() + ")");
    }
    assertThat(out).contains(
      "0:(1,0)", "1:(1,1)",
      "2:(2,0)", "3:(2,1)",
      // ...
      "396:(199,0)", "397:(199,1)",
      "398:(200,0)", "399:(200,1)",
      "400:(201,0)");
  }

  /** Check that position 0 in String corresponds to FIRST_LINE:FIRST_COLUMN. */
  @Test
  void testToPosition() {
    Position firstPosition = new LineColumnConverter("foo").toPosition(0);
    assertThat(firstPosition.line()).isEqualTo(Position.FIRST_LINE);
    assertThat(firstPosition.column()).isEqualTo(Position.FIRST_COLUMN);
  }
}
