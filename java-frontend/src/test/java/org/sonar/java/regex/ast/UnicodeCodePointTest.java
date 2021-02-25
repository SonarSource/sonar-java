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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.regex.RegexParserTestUtils.assertFailParsing;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertListElements;
import static org.sonar.java.regex.RegexParserTestUtils.assertLocation;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;

class UnicodeCodePointTest {

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
    UnicodeCodePointTree unicodeCodePointTree = assertType(UnicodeCodePointTree.class, assertSuccessfulParse("\uD83D\uDE02"));
    assertEquals("\uD83D\uDE02", unicodeCodePointTree.characterAsString());

    SequenceTree wrongUnicodeCodePointTree = assertType(SequenceTree.class, assertSuccessfulParse("\uD83D\uD83D"));
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
    UnicodeCodePointTree unicodeCodePoint = assertType(UnicodeCodePointTree.class, regex);
    assertKind(RegexTree.Kind.UNICODE_CODE_POINT, unicodeCodePoint);
    assertKind(CharacterClassElementTree.Kind.UNICODE_CODE_POINT, unicodeCodePoint);
    assertEquals(expectedString, unicodeCodePoint.characterAsString());
    assertEquals(expectedCodePoint, unicodeCodePoint.codePointOrUnit());
    assertLocation(start, end, unicodeCodePoint);
    assertTrue(unicodeCodePoint.isEscapeSequence());
    assertEquals(AutomatonState.TransitionType.CHARACTER, unicodeCodePoint.incomingTransitionType());
  }

}
