/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.ast.parser.grammar.literals;

import org.junit.Test;
import org.sonar.java.ast.api.JavaTokenType;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class StringLiteralTest {

  @Test
  public void ok() {
    assertThat(JavaTokenType.STRING_LITERAL)
      .as("regular string").matches("\"string\"")
      .as("empty string").matches("\"\"")
      .as("escaped LF").matches("\"\\n\"")
      .as("escaped double quotes").matches("\"string, which contains \\\"escaped double quotes\\\"\"")
      .as("octal escape").matches("\"string \\177\"")
      .as("unicode escape").matches("\"string \\u03a9\"");
  }

  @Test
  public void nok() {
    assertThat(JavaTokenType.STRING_LITERAL)
      .notMatches("\"");
  }

}
