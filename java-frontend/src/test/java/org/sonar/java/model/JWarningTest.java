/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import org.sonar.plugins.java.api.tree.SyntaxToken;

import static org.assertj.core.api.Assertions.assertThat;

class JWarningTest {

  @Test
  void getters() {
    String message = "message";
    JWarning.Type type = JWarning.Type.UNUSED_IMPORT;
    int startLine = 0;
    int startColumn = 1;
    int endLine = 2;
    int endColumn = 3;
    JWarning warning = new JWarning(message, type, startLine, startColumn, endLine, endColumn);

    assertThat(warning.getStartLine()).isEqualTo(startLine);
    assertThat(warning.getStartColumn()).isEqualTo(startColumn);
    assertThat(warning.getEndLine()).isEqualTo(endLine);
    assertThat(warning.getEndColumn()).isEqualTo(endColumn);
    assertThat(warning.getMessage()).isEqualTo(message);
    assertThat(warning.getType()).isEqualTo(type);
  }

  @Test
  void multiline_warning_contains() {
    JWarning warning = new JWarning("message", JWarning.Type.UNUSED_IMPORT, 5, 20, 7, 50);

    SyntaxToken beforeDifferentLine = syntaxToken(4, 5);
    SyntaxToken beforeSameStartLine = syntaxToken(5, 5);
    SyntaxToken insideSameStartLine = syntaxToken(5, 25);
    SyntaxToken inside = syntaxToken(6, 25);
    SyntaxToken insideSameEndLine = syntaxToken(7, 40);
    SyntaxToken afterSameEndLine = syntaxToken(7, 51);
    SyntaxToken afterDifferentLine = syntaxToken(8, 0);

    assertThat(warning.contains(beforeDifferentLine)).isFalse();
    assertThat(warning.contains(beforeSameStartLine)).isFalse();
    assertThat(warning.contains(insideSameStartLine)).isTrue();
    assertThat(warning.contains(inside)).isTrue();
    assertThat(warning.contains(insideSameEndLine)).isTrue();
    assertThat(warning.contains(afterSameEndLine)).isFalse();
    assertThat(warning.contains(afterDifferentLine)).isFalse();
  }

  @Test
  void single_line_warning_contains() {
    int line = 5;
    JWarning warning = new JWarning("message", JWarning.Type.UNUSED_IMPORT, line, 20, line, 50);

    SyntaxToken beforeDifferentLine = syntaxToken(line - 1, 0);
    SyntaxToken beforeSameLine = syntaxToken(line, 0);
    SyntaxToken inside = syntaxToken(line, 25);
    SyntaxToken afterSameLine = syntaxToken(line, 51);
    SyntaxToken afterDifferentLine = syntaxToken(line + 1, 0);

    assertThat(warning.contains(beforeDifferentLine)).isFalse();
    assertThat(warning.contains(beforeSameLine)).isFalse();
    assertThat(warning.contains(inside)).isTrue();
    assertThat(warning.contains(afterSameLine)).isFalse();
    assertThat(warning.contains(afterDifferentLine)).isFalse();
  }

  private static SyntaxToken syntaxToken(int line, int column) {
    return new InternalSyntaxToken(line, column, "test", Collections.emptyList(), false);
  }
}
