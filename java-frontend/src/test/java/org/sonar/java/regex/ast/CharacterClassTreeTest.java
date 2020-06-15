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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sonar.java.regex.RegexParserTestUtils.assertCharacterClass;
import static org.sonar.java.regex.RegexParserTestUtils.assertCharacterRange;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertListElements;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;

class CharacterClassTreeTest {

  @Test
  void simpleCharacterClass() {
    RegexTree regex = assertSuccessfulParse("[a-z]");
    assertCharacterRange('a', 'z', assertCharacterClass(false, regex));
  }

  @Test
  void closingBracket() {
    RegexTree regex = assertSuccessfulParse("[]]");
    PlainCharacterTree character = assertType(PlainCharacterTree.class, assertCharacterClass(false, regex));
    assertEquals(']', character.getCharacter(), "Matched character should be ']'.");
  }

  @Test
  void dashRange() {
    RegexTree regex = assertSuccessfulParse("[---]");
    assertKind(RegexTree.Kind.CHARACTER_CLASS, regex);
    assertCharacterRange('-', '-', assertCharacterClass(false, regex));
  }

  @Test
  void leadingDash() {
    RegexTree regex = assertSuccessfulParse("[-a]");
    CharacterClassUnionTree union = assertType(CharacterClassUnionTree.class, assertCharacterClass(false, regex));
    assertKind(RegexTree.Kind.CHARACTER_CLASS_UNION, union);
    assertListElements(union.getCharacterClasses(),
      firstChar -> assertPlainCharacter('-', firstChar),
      secondChar -> assertPlainCharacter('a', secondChar)
    );
  }

  @Test
  void trailingDash() {
    RegexTree regex = assertSuccessfulParse("[a-]");
    CharacterClassUnionTree union = assertType(CharacterClassUnionTree.class, assertCharacterClass(false, regex));
    assertListElements(union.getCharacterClasses(),
      firstChar -> assertPlainCharacter('a', firstChar),
      secondChar -> assertPlainCharacter('-', secondChar)
    );
  }

  @Test
  void unionOfRangesAndSingleCharacters() {
    RegexTree regex = assertSuccessfulParse("[a-z0-9_.^]");
    CharacterClassUnionTree union = assertType(CharacterClassUnionTree.class, assertCharacterClass(false, regex));
    assertListElements(union.getCharacterClasses(),
      first -> assertCharacterRange('a', 'z', first),
      second -> assertCharacterRange('0', '9', second),
      third -> assertPlainCharacter('_', third),
      fourth -> assertPlainCharacter('.', fourth),
      fifth -> assertPlainCharacter('^', fifth)
    );
  }

  @Test
  void negatedCharacterClass() {
    RegexTree regex = assertSuccessfulParse("[^a-z]");
    assertCharacterRange('a', 'z', assertCharacterClass(true, regex));
  }

  @Test
  void intersection() {
    RegexTree regex = assertSuccessfulParse("[a-z&&[^g-i]&]");
    CharacterClassIntersectionTree intersection = assertType(CharacterClassIntersectionTree.class, assertCharacterClass(false, regex));
    assertKind(RegexTree.Kind.CHARACTER_CLASS_INTERSECTION, intersection);
    assertListElements(intersection.getCharacterClasses(),
      first -> assertCharacterRange('a', 'z', first),
      second -> {
        CharacterClassUnionTree union = assertType(CharacterClassUnionTree.class, second);
        assertListElements(union.getCharacterClasses(),
          first -> assertCharacterRange('g', 'i', assertCharacterClass(true, first)),
          last -> assertPlainCharacter('&', last)
        );
      }
    );
  }

  @Test
  void rangeWithEscapes() {
    RegexTree regex = assertSuccessfulParse("[\\\\[-\\\\]]");
    assertCharacterRange('[', ']', assertCharacterClass(false, regex));
  }

  @Test
  void classWithEscapes() {
    RegexTree regex = assertSuccessfulParse("[\\\\[\\\\-\\\\]]");
    CharacterClassUnionTree union = assertType(CharacterClassUnionTree.class, assertCharacterClass(false, regex));
    assertListElements(union.getCharacterClasses(),
      first -> assertPlainCharacter('[', first),
      second -> assertPlainCharacter('-', second),
      third -> assertPlainCharacter(']', third)
    );
  }

}
