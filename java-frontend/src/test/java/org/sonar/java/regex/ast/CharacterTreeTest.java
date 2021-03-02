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
import org.sonar.java.regex.RegexParserTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.regex.RegexParserTestUtils.*;

class CharacterTreeTest {

  @Test
  void testSimpleCharacter() {
    RegexParserTestUtils.assertCharacter('x', false, "x");
    RegexParserTestUtils.assertCharacter(' ', false, " ");
  }

  @Test
  void testSimpleEscapeSequences() {
    RegexParserTestUtils.assertCharacter('\b', true, "\\b");
    RegexParserTestUtils.assertCharacter('\t', true, "\\t");
    RegexParserTestUtils.assertCharacter('\n', true, "\\n");
    RegexParserTestUtils.assertCharacter('\f', true, "\\f");
    RegexParserTestUtils.assertCharacter('\r', true, "\\r");
    RegexParserTestUtils.assertCharacter('"', true, "\\\"");
  }

  @Test
  void testDoubleEscapedSimpleEscapeSequences() {
    RegexParserTestUtils.assertCharacter('\t', true, "\\\\t");
    RegexParserTestUtils.assertCharacter('\n', true, "\\\\n");
    RegexParserTestUtils.assertCharacter('\f', true, "\\\\f");
    RegexParserTestUtils.assertCharacter('\r', true, "\\\\r");
    RegexParserTestUtils.assertCharacter('\u0007', true, "\\\\a");
    RegexParserTestUtils.assertCharacter('\u001B', true, "\\\\e");
  }

  @Test
  void testControlCharacters() {
    RegexParserTestUtils.assertCharacter('\u0000', true, "\\\\c@");
    RegexParserTestUtils.assertCharacter('\u0001', true, "\\\\cA");
    RegexParserTestUtils.assertCharacter('\u001A', true, "\\\\cZ");
    RegexParserTestUtils.assertCharacter('\u001B', true, "\\\\c[");
    RegexParserTestUtils.assertCharacter('\u001C', true, "\\\\c\\\\");
    RegexParserTestUtils.assertCharacter('\u001D', true, "\\\\c]");
    RegexParserTestUtils.assertCharacter('\u001E', true, "\\\\c^");
    RegexParserTestUtils.assertCharacter('\u001F', true, "\\\\c_");
    RegexParserTestUtils.assertCharacter('\u007F', true, "\\\\c?");
    assertFailParsing("\\\\c", "Expected any character, but found the end of the regex");
  }

  @Test
  void octalEscapeSequences() {
    RegexParserTestUtils.assertCharacter('\n', true, "\\012");
    RegexParserTestUtils.assertCharacter('\n', true, "\\12");
    RegexParserTestUtils.assertCharacter('D', true, "\\104");
    assertPlainString("D\n", "\\104\\012");
    assertPlainString("\nD", "\\12D");
  }

  @Test
  void octalEscapesWithDoubleBackslash() {
    RegexParserTestUtils.assertCharacter('\n', true, "\\\\0012");
    RegexParserTestUtils.assertCharacter('\n', true, "\\\\012");
    RegexParserTestUtils.assertCharacter('D', true, "\\\\0104");
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
    RegexParserTestUtils.assertCharacter('\t', true, "\\u0009");
    RegexParserTestUtils.assertCharacter('D', true, "\\u0044");
    RegexParserTestUtils.assertCharacter('ö', true, "\\u00F6");
  }

  @Test
  void unicodeEscapesWithDoubleBackslash() {
    RegexParserTestUtils.assertCharacter('\u1234', true, "\\\\u1234");
    RegexParserTestUtils.assertCharacter('\n', true, "\\\\u000A");
  }

  @Test
  void errorsInUnicodeEscapesWithDoubleBackslash() {
    assertFailParsing("\\\\u123", "Expected hexadecimal digit, but found the end of the regex");
    assertFailParsing("\\\\u123X", "Expected hexadecimal digit, but found 'X'");
    // Note that using multiple 'u's is legal in Java Unicode escapes, but not in regex ones
    assertFailParsing("\\\\uu1234", "Expected hexadecimal digit, but found 'u'");
    RegexParserTestUtils.assertCharacter('\n', "\\\\u000A");
  }

  @Test
  void escapedMetaCharacters() {
    RegexParserTestUtils.assertCharacter('\\', "\\\\\\\\");
    RegexParserTestUtils.assertCharacter('.', "\\\\.");
    RegexParserTestUtils.assertCharacter('(', "\\\\(");
    RegexParserTestUtils.assertCharacter(')', "\\\\)");
    RegexParserTestUtils.assertCharacter('[', "\\\\[");
    RegexParserTestUtils.assertCharacter(']', "\\\\]");
    RegexParserTestUtils.assertCharacter('{', "\\\\{");
    RegexParserTestUtils.assertCharacter('}', "\\\\}");
  }

  @Test
  void unicodeRidiculousness() {
    RegexParserTestUtils.assertCharacter('\t', "\\u005ct");
    RegexParserTestUtils.assertCharacter('\\', "\\u005c\\uu005c\\uuu005c\\u005c");
  }

  @Test
  void unclosedEscapeSequence() {
    assertFailParsing("\\\\", "Expected any character, but found the end of the regex");
  }

  @Test
  void withoutBraces() {
    SequenceTree sequence = assertType(SequenceTree.class, assertSuccessfulParse("\\\\xF6\\\\x0A\\\\x0a"));
    assertListElements(sequence.getItems(),
      first -> assertCodePoint("ö", 'ö', 0, 5, first),
      second -> assertCodePoint("\n", '\n', 5, 10, second),
      third -> assertCodePoint("\n", '\n', 10, 15, third)
    );
  }

  @Test
  void parseSupplementaryMultilingualPlane() {
    CharacterTree escapedUnicodeCodePointTree = assertType(CharacterTree.class, assertSuccessfulParse("\\\\uD83D\\\\uDE02"));
    assertEquals("\uD83D\uDE02", escapedUnicodeCodePointTree.characterAsString());

    CharacterTree escapedUnicodeCodePointTree2 = assertType(CharacterTree.class, assertSuccessfulParse("\\uD83D\\uDE02"));
    assertEquals("\uD83D\uDE02", escapedUnicodeCodePointTree2.characterAsString());


    CharacterTree unicodeCodePointTree = assertType(CharacterTree.class, assertSuccessfulParse("\uD83D\uDE02"));
    assertEquals("\uD83D\uDE02", unicodeCodePointTree.characterAsString());

    assertType(SequenceTree.class, assertSuccessfulParse("\uD83D\uD83D"));
  }

  @Test
  void withBraces() {
    String pileOfPoo = "\ud83d\udca9";
    SequenceTree sequence = assertType(SequenceTree.class, assertSuccessfulParse("\\\\x{F6}\\\\x{1f4a9}\\\\x{A}"));
    assertListElements(sequence.getItems(),
      first -> assertCodePoint("ö", 'ö', 0, 7, first),
      second -> assertCodePoint(pileOfPoo, 0x1f4a9, 7, 17, second),
      third -> assertCodePoint("\n", '\n', 17, 23, third)
    );
  }

  @Test
  void errors() {
    assertFailParsing("\\\\x1", "Expected hexadecimal digit, but found the end of the regex");
    assertFailParsing("\\\\x1X", "Expected hexadecimal digit, but found 'X'");
    assertFailParsing("\\\\x{}", "Expected hexadecimal digit, but found '}'");
    assertFailParsing("\\\\x{1X}", "Expected hexadecimal digit or '}', but found 'X'");
    assertFailParsing("\\\\x{1", "Expected hexadecimal digit or '}', but found the end of the regex");
    assertFailParsing("\\\\x{110000}", "Invalid Unicode code point");
  }

  void assertCodePoint(String expectedString, int expectedCodePoint, int start, int end, RegexTree regex) {
    CharacterTree unicodeCodePoint = assertType(CharacterTree.class, regex);
    assertKind(RegexTree.Kind.CHARACTER, unicodeCodePoint);
    assertKind(CharacterClassElementTree.Kind.PLAIN_CHARACTER, unicodeCodePoint);
    assertEquals(expectedString, unicodeCodePoint.characterAsString());
    assertEquals(expectedCodePoint, unicodeCodePoint.codePointOrUnit());
    assertLocation(start, end, unicodeCodePoint);
    assertTrue(unicodeCodePoint.isEscapeSequence());
    assertEquals(AutomatonState.TransitionType.CHARACTER, unicodeCodePoint.incomingTransitionType());
  }
}
