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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sonar.java.regex.RegexParserTestUtils.assertFailParsing;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertLocation;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;

class MiscEscapeSequenceTreeTest {

  @Test
  void testBackslashN() {
    assertMiscEscapeSequence("\\\\N{Slightly Smiling Face}");
    assertMiscEscapeSequence("\\\\N{invalid name}"); // This should actually produce an error, but is accepted for now
    assertFailParsing("\\\\N", "Expected '{', but found the end of the regex");
    assertFailParsing("\\\\N{", "Expected a Unicode character name, but found the end of the regex");
    assertFailParsing("\\\\N{}", "Expected a Unicode character name, but found '}'");
    assertFailParsing("\\\\N{x", "Expected '}', but found the end of the regex");
  }

  @Test
  void testBackslashR() {
    assertMiscEscapeSequence("\\\\R");
  }

  @Test
  void testBackslashX() {
    assertMiscEscapeSequence("\\\\X");
  }

  private void assertMiscEscapeSequence(String regex) {
    assertMiscEscapeSequence(regex, 0);
  }

  private void assertMiscEscapeSequence(String regex, int initialFlags) {
    MiscEscapeSequenceTree escapeSequence = assertType(MiscEscapeSequenceTree.class, assertSuccessfulParse(regex, initialFlags));
    assertKind(RegexTree.Kind.MISC_ESCAPE_SEQUENCE, escapeSequence);
    assertThat(escapeSequence.activeFlags().getMask()).isEqualTo(initialFlags);
    assertEquals(regex, escapeSequence.getText());
    assertLocation(0, regex.length(), escapeSequence);
    assertEquals(AutomatonState.TransitionType.CHARACTER, escapeSequence.incomingTransitionType());
    assertEquals(CharacterClassElementTree.Kind.MISC_ESCAPE_SEQUENCE, escapeSequence.characterClassElementKind());
  }

}
