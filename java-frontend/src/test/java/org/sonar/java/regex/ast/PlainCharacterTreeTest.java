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
package org.sonar.java.regex.ast;

import org.junit.jupiter.api.Test;

import static org.sonar.java.regex.RegexParserTestUtils.assertFailParsing;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainString;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;

class PlainCharacterTreeTest {

  @Test
  void testSimpleCharacter() {
    assertPlainCharacter('x', false, "x");
    assertPlainCharacter(' ', false, " ");
  }

  @Test
  void testSimpleEscapeSequences() {
    assertPlainCharacter('\b', true, "\\b");
    assertPlainCharacter('\t', true, "\\t");
    assertPlainCharacter('\n', true, "\\n");
    assertPlainCharacter('\f', true, "\\f");
    assertPlainCharacter('\r', true, "\\r");
    assertPlainCharacter('"', true, "\\\"");
  }

  @Test
  void testDoubleEscapedSimpleEscapeSequences() {
    assertPlainCharacter('\t', true, "\\\\t");
    assertPlainCharacter('\n', true, "\\\\n");
    assertPlainCharacter('\f', true, "\\\\f");
    assertPlainCharacter('\r', true, "\\\\r");
    assertPlainCharacter('\u0007', true, "\\\\a");
    assertPlainCharacter('\u001B', true, "\\\\e");
  }

  @Test
  void testControlCharacters() {
    assertPlainCharacter('\u0000', true, "\\\\c@");
    assertPlainCharacter('\u0001', true, "\\\\cA");
    assertPlainCharacter('\u001A', true, "\\\\cZ");
    assertPlainCharacter('\u001B', true, "\\\\c[");
    assertPlainCharacter('\u001C', true, "\\\\c\\\\");
    assertPlainCharacter('\u001D', true, "\\\\c]");
    assertPlainCharacter('\u001E', true, "\\\\c^");
    assertPlainCharacter('\u001F', true, "\\\\c_");
    assertPlainCharacter('\u007F', true, "\\\\c?");
    assertFailParsing("\\\\c", "Expected any character, but found the end of the regex");
  }

  @Test
  void octalEscapeSequences() {
    assertPlainCharacter('\n', true, "\\012");
    assertPlainCharacter('\n', true, "\\12");
    assertPlainCharacter('D', true, "\\104");
    assertPlainString("D\n", "\\104\\012");
    assertPlainString("\nD", "\\12D");
  }

  @Test
  void octalEscapesWithDoubleBackslash() {
    assertPlainCharacter('\n', true, "\\\\0012");
    assertPlainCharacter('\n', true, "\\\\012");
    assertPlainCharacter('D', true, "\\\\0104");
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
    assertPlainCharacter('\t', true, "\\u0009");
    assertPlainCharacter('D', true, "\\u0044");
    assertPlainCharacter('ö', true, "\\u00F6");
  }

  @Test
  void unicodeEscapesWithDoubleBackslash() {
    assertPlainCharacter('\u1234', true, "\\\\u1234");
    assertPlainCharacter('\n', true, "\\\\u000A");
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
