/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.regex.ast;

import org.junit.jupiter.api.Test;

import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;

class PlainCharacterTreeTest {

  @Test
  void testSimpleCharacter() {
    assertPlainCharacter('x', "x");
    assertPlainCharacter(' ', " ");
  }

  @Test
  void testSimpleEscapeSequences() {
    assertPlainCharacter('\b', "\\b");
    assertPlainCharacter('\t', "\\t");
    assertPlainCharacter('\n', "\\n");
    assertPlainCharacter('\f', "\\f");
    assertPlainCharacter('\r', "\\r");
    assertPlainCharacter('"', "\\\"");
  }

  @Test
  void octalEscapeSequences() {
    assertPlainCharacter('\n', "\\012");
    assertPlainCharacter('\n', "\\12");
    assertPlainCharacter('D', "\\104");
  }

  @Test
  void unicodeEscapeSequences() {
    assertPlainCharacter('\t', "\\u0009");
    assertPlainCharacter('D', "\\u0044");
    assertPlainCharacter('ö', "\\u00F6");
  }

  @Test
  void escapedMetaCharacters() {
    assertPlainCharacter('\\', "\\\\\\\\");
    assertPlainCharacter('.', "\\\\.");
    assertPlainCharacter('(', "\\\\(");
    assertPlainCharacter(')', "\\\\)");
    assertPlainCharacter('[', "\\\\[");
    assertPlainCharacter(']', "\\\\]");
    assertPlainCharacter('{', "\\\\{");
    assertPlainCharacter('}', "\\\\}");
  }

  @Test
  void unicodeRidiculousness() {
    assertPlainCharacter('\t', "\\u005ct");
    assertPlainCharacter('\\', "\\u005c\\u005c\\u005c\\u005c");
  }

}
