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

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.SyntaxError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertListElements;
import static org.sonar.java.regex.RegexParserTestUtils.assertLocation;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainString;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;
import static org.sonar.java.regex.RegexParserTestUtils.parseRegex;

class GroupTreesTest {

  @Test
  void testGroup() {
    RegexTree regex = assertSuccessfulParse("(x)");
    CapturingGroupTree group = assertType(CapturingGroupTree.class, regex);
    assertKind(RegexTree.Kind.CAPTURING_GROUP, group);
    assertThat(group.getName()).as("Group should be unnamed").isEmpty();
    assertPlainCharacter('x', group.getElement());
    assertLocation(0, 3, group);
    assertLocation(1, 2, group.getElement());
  }

  @Test
  void testUnfinishedGroup() {
    RegexParseResult result = parseRegex("(x");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Expected ')', but found the end of the regex", error.getMessage(), "Error should have the right message.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(2,2), locations.get(0).getIndexRange(), "Error should have the right location.");
    assertTrue(locations.get(0).getIndexRange().isEmpty(), "Error location should be empty range.");
  }

  @Test
  void testExtraParenthesis() {
    RegexParseResult result = parseRegex("(x))");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Unexpected ')'", error.getMessage(), "Error should have the right message.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(3,4), locations.get(0).getIndexRange(), "Error should have the right location.");
    assertFalse(locations.get(0).getIndexRange().isEmpty(), "Error location should not be empty range.");
  }

  @Test
  void testNonCapturing() {
    RegexTree regex = assertSuccessfulParse("(?:x)");
    NonCapturingGroupTree group = assertType(NonCapturingGroupTree.class, regex);
    assertKind(RegexTree.Kind.NON_CAPTURING_GROUP, group);
    RegexTree element = group.getElement();
    assertNotNull(element, "Group should have a body.");
    assertPlainCharacter('x', element);
    assertLocation(0, 5, group);
    assertLocation(3, 4, group.getElement());
  }

  @Test
  void testFlags() {
    RegexTree regex = assertSuccessfulParse("a (?x:b c) d");
    SequenceTree seq = assertType(SequenceTree.class, regex);
    assertListElements(seq.getItems(),
      first -> assertPlainCharacter('a', first),
      second -> assertPlainCharacter(' ', second),
      third -> {
        NonCapturingGroupTree group = assertType(NonCapturingGroupTree.class, third);
        assertEquals(Pattern.COMMENTS, group.getEnabledFlags().getMask());
        RegexTree element = group.getElement();
        assertNotNull(element, "Group should have a body.");
        assertPlainString("bc", element);
      },
      fourth -> assertPlainCharacter(' ', fourth),
      fifth -> assertPlainCharacter('d', fifth)
    );
  }

  @Test
  void testFlags2() {
    RegexTree regex = assertSuccessfulParse("a (?x)b c(?-x) d");
    SequenceTree seq = assertType(SequenceTree.class, regex);
    assertListElements(seq.getItems(),
      first -> assertPlainCharacter('a', first),
      second -> assertPlainCharacter(' ', second),
      third -> {
        NonCapturingGroupTree group = assertType(NonCapturingGroupTree.class, third);
        assertEquals(Pattern.COMMENTS, group.getEnabledFlags().getMask());
        assertNull(group.getElement(), "Group should not have a body.");
      },
      fourth -> assertPlainCharacter('b', fourth),
      fifth -> assertPlainCharacter('c', fifth),
      sixth -> {
        NonCapturingGroupTree group = assertType(NonCapturingGroupTree.class, sixth);
        assertEquals(Pattern.COMMENTS, group.getDisabledFlags().getMask());
        assertNull(group.getElement(), "Group should not have a body.");
      },
      seventh -> assertPlainCharacter(' ', seventh),
      eighth -> assertPlainCharacter('d', eighth)
    );
  }

  @Test
  void testNamedGroup() {
    RegexTree regex = assertSuccessfulParse("(?<foo>x)");
    CapturingGroupTree group = assertType(CapturingGroupTree.class, regex);
    assertKind(RegexTree.Kind.CAPTURING_GROUP, group);
    assertThat(group.getName()).hasValue("foo");
    assertPlainCharacter('x', group.getElement());
  }

  @Test
  void testPositiveLookAhead() {
    RegexTree regex = assertSuccessfulParse("(?=x)");
    LookAroundTree lookAround = assertType(LookAroundTree.class, regex);
    assertKind(RegexTree.Kind.LOOK_AROUND, lookAround);
    assertEquals(LookAroundTree.Polarity.POSITIVE, lookAround.getPolarity());
    assertEquals(LookAroundTree.Direction.AHEAD, lookAround.getDirection());
    assertPlainCharacter('x', lookAround.getElement());
  }

  @Test
  void testPositiveLookBehind() {
    RegexTree regex = assertSuccessfulParse("(?<=x)");
    LookAroundTree lookAround = assertType(LookAroundTree.class, regex);
    assertKind(RegexTree.Kind.LOOK_AROUND, lookAround);
    assertEquals(LookAroundTree.Polarity.POSITIVE, lookAround.getPolarity());
    assertEquals(LookAroundTree.Direction.BEHIND, lookAround.getDirection());
    assertPlainCharacter('x', lookAround.getElement());
  }

  @Test
  void testNegativeLookAhead() {
    RegexTree regex = assertSuccessfulParse("(?!x)");
    LookAroundTree lookAround = assertType(LookAroundTree.class, regex);
    assertKind(RegexTree.Kind.LOOK_AROUND, lookAround);
    assertEquals(LookAroundTree.Polarity.NEGATIVE, lookAround.getPolarity());
    assertEquals(LookAroundTree.Direction.AHEAD, lookAround.getDirection());
    assertPlainCharacter('x', lookAround.getElement());
  }

  @Test
  void testNegativeLookBehind() {
    RegexTree regex = assertSuccessfulParse("(?<!x)");
    LookAroundTree lookAround = assertType(LookAroundTree.class, regex);
    assertKind(RegexTree.Kind.LOOK_AROUND, lookAround);
    assertEquals(LookAroundTree.Polarity.NEGATIVE, lookAround.getPolarity());
    assertEquals(LookAroundTree.Direction.BEHIND, lookAround.getDirection());
    assertPlainCharacter('x', lookAround.getElement());
  }

  @Test
  void testAtomicGroup() {
    RegexTree regex = assertSuccessfulParse("(?>x)");
    AtomicGroupTree group = assertType(AtomicGroupTree.class, regex);
    assertKind(RegexTree.Kind.ATOMIC_GROUP, group);
    assertPlainCharacter('x', group.getElement());
  }

}
