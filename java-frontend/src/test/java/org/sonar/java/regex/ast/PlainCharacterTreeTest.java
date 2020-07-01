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
    assertFalse(((PlainCharacterTree) x).getContents().isEscapeSequence());
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
  void octalEscapesWithDoubleBackslash() {
    assertPlainCharacter('\n', "\\\\0012");
    assertPlainCharacter('\n', "\\\\012");
    assertPlainCharacter('D', "\\\\0104");
    assertPlainString("D\n", "\\\\0104\\\\012");
    assertPlainString("\nD", "\\\\012D");
    assertPlainString("%6", "\\\\0456");
  }

  @Test
  void errorsInOctalEscapesWithDoubleBackslash() {
    assertFailParsing("\\\\0", "Expected octal digit, but found the end of the regex");
    assertFailParsing("\\\\0x", "Expected octal digit, but found 'x'");
  }

  @Test
  void unicodeEscapeSequences() {
    RegexTree u1 = assertSuccessfulParse("\\u0009");
    assertPlainCharacter('\t', u1);
    assertTrue(((PlainCharacterTree) u1).getContents().isEscapeSequence());
    RegexTree u2 = assertSuccessfulParse("\\u0044");
    assertPlainCharacter('D', u2);
    assertTrue(((PlainCharacterTree) u2).getContents().isEscapeSequence());
    RegexTree u3 = assertSuccessfulParse("\\u00F6");
    assertPlainCharacter('ö', u3);
    assertTrue(((PlainCharacterTree) u3).getContents().isEscapeSequence());
  }

  @Test
  void unicodeEscapesWithDoubleBackslash() {
    assertPlainCharacter('\u1234', "\\\\u1234");
    assertPlainCharacter('\n', "\\\\u000A");
  }

  @Test
  void errorsInUnicodeEscapesWithDoubleBackslash() {
    assertFailParsing("\\\\u123", "Expected hexadecimal digit, but found the end of the regex");
    assertFailParsing("\\\\u123X", "Expected hexadecimal digit, but found 'X'");
    // Note that using multiple 'u's is legal in Java Unicode escapes, but not in regex ones
    assertFailParsing("\\\\uu1234", "Expected hexadecimal digit, but found 'u'");
    assertPlainCharacter('\n', "\\\\u000A");
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
