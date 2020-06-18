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
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.SyntaxError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertLocation;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;
import static org.sonar.java.regex.RegexParserTestUtils.parseRegex;

class CurlyBraceQuantifierTest {

  @Test
  void testCurlyBracedQuantifier() {
    RegexTree regex = assertSuccessfulParse("x{23,42}");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertKind(RegexTree.Kind.REPETITION, repetition);
    assertPlainCharacter('x', repetition.getElement());
    CurlyBraceQuantifier quantifier = assertType(CurlyBraceQuantifier.class, repetition.getQuantifier());
    assertLocation(2, 4, quantifier.getMinimumRepetitionsToken());
    assertLocation(4, 5, quantifier.getCommaToken());
    assertLocation(5, 7, quantifier.getMaximumRepetitionsToken());
    assertEquals(23, quantifier.getMinimumRepetitions(), "Lower bound should be 23.");
    assertEquals(42, quantifier.getMaximumRepetitions(), "Upper bound should be 42.");
    assertFalse(quantifier.isOpenEnded(), "Quantifier should not be open ended.");
    assertFalse(quantifier.isSingleNumber(), "Quantifier should not be marked as only having a single number.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
  }

  @Test
  void testCurlyBracedQuantifierWithNoUpperBound() {
    RegexTree regex = assertSuccessfulParse("x{42,}");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertKind(RegexTree.Kind.REPETITION, repetition);
    assertPlainCharacter('x', repetition.getElement());
    CurlyBraceQuantifier quantifier = assertType(CurlyBraceQuantifier.class, repetition.getQuantifier());
    assertEquals(42, quantifier.getMinimumRepetitions(), "Lower bound should be 42.");
    assertNull(quantifier.getMaximumRepetitions(), "Quantifier should be open ended.");
    assertTrue(quantifier.isOpenEnded(), "Quantifier should be open ended.");
    assertFalse(quantifier.isSingleNumber(), "Quantifier should not be marked as only having a single number.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
  }

  @Test
  void testFixedCurlyBracedQuantifier() {
    RegexTree regex = assertSuccessfulParse("x{42}");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    CurlyBraceQuantifier quantifier = assertType(CurlyBraceQuantifier.class, repetition.getQuantifier());
    assertEquals(42, quantifier.getMinimumRepetitions(), "Lower bound should be 42.");
    assertEquals(42, quantifier.getMaximumRepetitions(), "Upper bound should be the same as lower bound.");
    assertFalse(quantifier.isOpenEnded(), "Quantifier should not be open ended.");
    assertTrue(quantifier.isSingleNumber(), "Quantifier should be marked as only having a single number.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
  }

  @Test
  void testLazyCurlyBracedQuantifier() {
    RegexTree regex = assertSuccessfulParse("x{23,42}?");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    CurlyBraceQuantifier quantifier = assertType(CurlyBraceQuantifier.class, repetition.getQuantifier());
    assertEquals(23, quantifier.getMinimumRepetitions(), "Lower bound should be 23.");
    assertEquals(42, quantifier.getMaximumRepetitions(), "Upper bound should be 42.");
    assertEquals(Quantifier.Modifier.LAZY, quantifier.getModifier(), "Quantifier should be lazy.");
  }

  @Test
  void testPossessiveCurlyBracedQuantifier() {
    RegexTree regex = assertSuccessfulParse("x{23,42}+");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    CurlyBraceQuantifier quantifier = assertType(CurlyBraceQuantifier.class, repetition.getQuantifier());
    assertEquals(23, quantifier.getMinimumRepetitions(), "Lower bound should be 23.");
    assertEquals(42, quantifier.getMaximumRepetitions(), "Upper bound should be 42.");
    assertEquals(Quantifier.Modifier.POSSESSIVE, quantifier.getModifier(), "Quantifier should be possessive.");
  }

  @Test
  void testCurlyBracedQuantifierWithNonNumber() {
    RegexParseResult result = parseRegex("x{a}");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Expected integer, but found 'a'", error.getMessage(), "Error should have the right message.");
    assertEquals("a", error.getOffendingSyntaxElement().getText(), "Error should complain about the correct part of the regex.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(2, 3), locations.get(0).getIndexRange(), "Error should have the right location.");
  }

  @Test
  void testCurlyBracedQuantifierWithJunkAfterNumber() {
    RegexParseResult result = parseRegex("x{1a}");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Expected ',' or '}', but found 'a'", error.getMessage(), "Error should have the right message.");
    assertEquals("a", error.getOffendingSyntaxElement().getText(), "Error should complain about the correct part of the regex.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(3, 4), locations.get(0).getIndexRange(), "Error should have the right location.");
  }

  @Test
  void testCurlyBracedQuantifierWithJunkAfterComma() {
    RegexParseResult result = parseRegex("x{1,a}");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Expected integer or '}', but found 'a'", error.getMessage(), "Error should have the right message.");
    assertEquals("a", error.getOffendingSyntaxElement().getText(), "Error should complain about the correct part of the regex.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(4, 5), locations.get(0).getIndexRange(), "Error should have the right location.");
  }

  @Test
  void testCurlyBracedQuantifierWithJunkAfterSecondNumber() {
    RegexParseResult result = parseRegex("x{1,2a}");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Expected '}', but found 'a'", error.getMessage(), "Error should have the right message.");
    assertEquals("a", error.getOffendingSyntaxElement().getText(), "Error should complain about the correct part of the regex.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(5, 6), locations.get(0).getIndexRange(), "Error should have the right location.");
  }

  @Test
  void testCurlyBracedQuantifierWithMissingClosingBrace() {
    RegexParseResult result = parseRegex("x{1,2");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Expected '}', but found the end of the regex", error.getMessage(), "Error should have the right message.");
    assertEquals("", error.getOffendingSyntaxElement().getText(), "Error should complain about the correct part of the regex.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(5, 5), locations.get(0).getIndexRange(), "Error should have the right location.");
    assertTrue(locations.get(0).getIndexRange().isEmpty(), "Error location should be empty range at end of regex.");
  }

  @Test
  void testCurlyBracedQuantifierWithoutOperand() {
    RegexParseResult result = parseRegex("{1,2}");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Unexpected quantifier '{1,2}'", error.getMessage(), "Error should have the right message.");
    assertEquals("{1,2}", error.getOffendingSyntaxElement().getText(), "Error should complain about the correct part of the regex.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(0, 5), locations.get(0).getIndexRange(), "Error should have the right location.");
    assertFalse(locations.get(0).getIndexRange().isEmpty(), "Error location should not be empty range.");
  }

  @Test
  void testCurlyBracedQuantifierWithoutOperandInGroup() {
    RegexParseResult result = parseRegex("({1,2})");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Unexpected quantifier '{1,2}'", error.getMessage(), "Error should have the right message.");
    assertEquals(error.getMessage(), error.toString(), "SyntaxError.toString() should equal the error message.");
    assertEquals("{1,2}", error.getOffendingSyntaxElement().getText(), "Error should complain about the correct part of the regex.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(1, 6), locations.get(0).getIndexRange(), "Error should have the right location.");
  }
}
