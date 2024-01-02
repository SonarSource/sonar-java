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

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxToken;

import static org.assertj.core.api.Assertions.assertThat;

class InternalSyntaxTokenTest {

  @Test
  void line_column() {
    SyntaxToken token = token(1, 1, "");
    assertThat(token.line()).isEqualTo(1);
    assertThat(token.column()).isZero();
    assertThat(token.text()).isEmpty();

    token = token(42, 22, "foo");
    assertThat(token.line()).isEqualTo(42);
    assertThat(token.column()).isEqualTo(21);
    assertThat(token.text()).isEqualTo("foo");
  }

  @Test
  void range() {
    assertThat(token(1, 1, "").range())
      .isEqualTo(Range.at(1,1,1,1));

    assertThat(token(42, 22, "foo").range())
      .isEqualTo(Range.at(42,22,42, 25));

    assertThat(token(42, 22, "\"\"\"foo\"\"\"").range())
      .isEqualTo(Range.at(42,22,42, 31));

    assertThat(token(10, 8, "\"\"\"foo\r\n  bar\n  \r  qix\"\"\"").range())
      .isEqualTo(Range.at(10,8,13, 9));

    assertThat(token(10, 8, "\"\"\"\n\n\n\"\"\"").range())
      .isEqualTo(Range.at(10,8,13, 4));
  }

  private static InternalSyntaxToken token(int line, int column, String value) {
    int columnOffset = column - 1;
    return new InternalSyntaxToken(line, columnOffset, value, Collections.emptyList(), false);
  }

}
