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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.regex.RegexParserTestUtils.assertFailParsing;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainString;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;

class PlainCharacterTreeTest {

  @Test
  void testSimpleCharacter() {
    RegexTree x = assertSuccessfulParse("x");
    assertPlainCharacter('x', x);
    assertFalse(((PlainCharacterTree) x).getContents().isEscapedUnicode());
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
    assertPlainString("D\n", "\\104\\012");
    assertPlainString("\nD", "\\12D");
  }

  @Test
  void unicodeEscapeSequences() {
    RegexTree u1 = assertSuccessfulParse("\\u0009");
    assertPlainCharacter('\t', u1);
    assertTrue(((PlainCharacterTree) u1).getContents().isEscapedUnicode());
    RegexTree u2 = assertSuccessfulParse("\\u0044");
    assertPlainCharacter('D', u2);
    assertTrue(((PlainCharacterTree) u2).getContents().isEscapedUnicode());
    RegexTree u3 = assertSuccessfulParse("\\u00F6");
    assertPlainCharacter('ö', u3);
    assertTrue(((PlainCharacterTree) u3).getContents().isEscapedUnicode());
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
    assertPlainCharacter('\\', "\\u005c\\uu005c\\uuu005c\\u005c");
  }

  @Test
  void unclosedEscapeSequence() {
    assertFailParsing("\\\\", "Expected any character, but found the end of the regex");
  }

}
