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
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;
import static org.sonar.java.regex.RegexParserTestUtils.parseRegex;

class QuantifierTest {

  @Test
  void testGreedyStar() {
    RegexTree regex = assertSuccessfulParse("x*");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.STAR, quantifier.getKind(), "Quantifier should be a Kleene star.");
    assertEquals(0, quantifier.getMinimumRepetitions(), "Lower bound should be 0.");
    assertNull(quantifier.getMaximumRepetitions(), "Kleene star should have no upper bound.");
    assertTrue(quantifier.isOpenEnded(), "Kleene star should be open ended.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
  }

  @Test
  void testGreedyPlus() {
    RegexTree regex = assertSuccessfulParse("x+");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.PLUS, quantifier.getKind(), "Quantifier should be a plus.");
    assertEquals(1, quantifier.getMinimumRepetitions(), "Lower bound should be 1.");
    assertNull(quantifier.getMaximumRepetitions(), "Plus should have no upper bound.");
    assertTrue(quantifier.isOpenEnded(), "Plus should be open ended.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
  }

  @Test
  void testGreedyQuestionMark() {
    RegexTree regex = assertSuccessfulParse("x?");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.QUESTION_MARK, quantifier.getKind(), "Quantifier should be a question mark.");
    assertEquals(0, quantifier.getMinimumRepetitions(), "Lower bound should be 0.");
    assertEquals(1, quantifier.getMaximumRepetitions(), "The upper bound of a question mark quantifier should be 1.");
    assertFalse(quantifier.isOpenEnded(), "Question mark should not be open ended.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
  }

  @Test
  void testLazyStar() {
    RegexTree regex = assertSuccessfulParse("x*?");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.STAR, quantifier.getKind(), "Quantifier should be a Kleene star.");
    assertEquals(Quantifier.Modifier.LAZY, quantifier.getModifier(), "Quantifier should be lazy.");
  }

  @Test
  void testPossessiveStar() {
    RegexTree regex = assertSuccessfulParse("x*+");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.STAR, quantifier.getKind(), "Quantifier should be a Kleene star.");
    assertEquals(Quantifier.Modifier.POSSESSIVE, quantifier.getModifier(), "Quantifier should be possessive.");
  }

  @Test
  void testDoubleQuantifier() {
    RegexParseResult result = parseRegex("x**");
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Unexpected quantifier '*'", error.getMessage(), "Error should have the right message.");
    assertEquals("*", error.getOffendingSyntaxElement().getText(), "Error should complain about the correct part of the regex.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(2,3), locations.get(0).getIndexRange(), "Error should have the right location.");
    assertFalse(locations.get(0).getIndexRange().isEmpty(), "Error location should not be empty range.");
  }

}
