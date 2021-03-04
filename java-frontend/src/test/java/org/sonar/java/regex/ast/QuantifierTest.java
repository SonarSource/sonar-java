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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.SyntaxError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.regex.RegexParserTestUtils.assertEdge;
import static org.sonar.java.regex.RegexParserTestUtils.assertFailParsing;
import static org.sonar.java.regex.RegexParserTestUtils.assertListElements;
import static org.sonar.java.regex.RegexParserTestUtils.assertCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertSingleEdge;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;
import static org.sonar.java.regex.RegexParserTestUtils.parseRegex;

class QuantifierTest {

  @Test
  void testGreedyStar() {
    assertXWithKleeneStar("x*");
  }

  @Test
  void testGreedyPlus() {
    RegexTree regex = assertSuccessfulParse("x+");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertCharacter('x', repetition.getElement());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.PLUS, quantifier.getKind(), "Quantifier should be a plus.");
    assertEquals(1, quantifier.getMinimumRepetitions(), "Lower bound should be 1.");
    assertNull(quantifier.getMaximumRepetitions(), "Plus should have no upper bound.");
    assertTrue(quantifier.isOpenEnded(), "Plus should be open ended.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");

    CurlyBraceQuantifierTest.testAutomaton(repetition, false);
  }

  @Test
  void testGreedyQuestionMark() {
    RegexTree regex = assertSuccessfulParse("x?");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    RegexTree x = repetition.getElement();
    assertCharacter('x', x);
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.QUESTION_MARK, quantifier.getKind(), "Quantifier should be a question mark.");
    assertEquals(0, quantifier.getMinimumRepetitions(), "Lower bound should be 0.");
    assertEquals(1, quantifier.getMaximumRepetitions(), "The upper bound of a question mark quantifier should be 1.");
    assertFalse(quantifier.isOpenEnded(), "Question mark should not be open ended.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");

    EndOfRepetitionState endOfRep = assertType(EndOfRepetitionState.class, repetition.continuation());
    assertListElements(repetition.successors(),
      assertEdge(x, AutomatonState.TransitionType.CHARACTER),
      assertEdge(endOfRep, AutomatonState.TransitionType.EPSILON)
    );
    assertSingleEdge(x, endOfRep, AutomatonState.TransitionType.EPSILON);
  }

  @Test
  void testReluctantStar() {
    RegexTree regex = assertSuccessfulParse("x*?");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertCharacter('x', repetition.getElement());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.STAR, quantifier.getKind(), "Quantifier should be a Kleene star.");
    assertEquals(Quantifier.Modifier.RELUCTANT, quantifier.getModifier(), "Quantifier should be reluctant.");
    assertTrue(repetition.isReluctant());
    assertFalse(repetition.isPossessive());

    testStarAutomaton(repetition, true);
  }

  @Test
  void testPossessiveStar() {
    RegexTree regex = assertSuccessfulParse("x*+");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertCharacter('x', repetition.getElement());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.STAR, quantifier.getKind(), "Quantifier should be a Kleene star.");
    assertEquals(Quantifier.Modifier.POSSESSIVE, quantifier.getModifier(), "Quantifier should be possessive.");
    assertTrue(repetition.isPossessive());
    assertFalse(repetition.isReluctant());

    testStarAutomaton(repetition, false);
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

  @Test
  void quotesInRepetition() {
    assertXWithKleeneStar("\\\\Qx\\\\E*");
    assertXWithKleeneStar("x\\\\Q\\\\E*");
  }

  @Test
  void quantifiersWithoutOperand() {
    assertFailParsing("*", "Unexpected quantifier '*'");
    assertFailParsing("+", "Unexpected quantifier '+'");
    assertFailParsing("?", "Unexpected quantifier '?'");
    assertFailParsing("{1,10}", "Unexpected quantifier '{1,10}'");
  }

  private void assertXWithKleeneStar(String regexSource) {
    RegexTree regex = assertSuccessfulParse(regexSource);
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertCharacter('x', repetition.getElement());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, repetition.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.STAR, quantifier.getKind(), "Quantifier should be a Kleene star.");
    assertEquals(0, quantifier.getMinimumRepetitions(), "Lower bound should be 0.");
    assertNull(quantifier.getMaximumRepetitions(), "Kleene star should have no upper bound.");
    assertTrue(quantifier.isOpenEnded(), "Kleene star should be open ended.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");

    testStarAutomaton(repetition, false);
  }

  private static void testStarAutomaton(RepetitionTree repetition, boolean reluctant) {
    EndOfRepetitionState endOfRep = assertType(EndOfRepetitionState.class, repetition.continuation());
    assertEquals(repetition.activeFlags(), endOfRep.activeFlags());
    FinalState finalState = assertType(FinalState.class, endOfRep.continuation());
    assertSingleEdge(endOfRep, finalState, AutomatonState.TransitionType.EPSILON);
    RegexTree x = repetition.getElement();
    assertEquals(AutomatonState.TransitionType.EPSILON, repetition.incomingTransitionType());
    if (reluctant) {
      assertListElements(repetition.successors(),
        assertEdge(endOfRep, AutomatonState.TransitionType.EPSILON),
        assertEdge(x, AutomatonState.TransitionType.CHARACTER)
      );
    } else {
      assertListElements(repetition.successors(),
        assertEdge(x, AutomatonState.TransitionType.CHARACTER),
        assertEdge(endOfRep, AutomatonState.TransitionType.EPSILON)
      );
    }
    assertSingleEdge(x, repetition, AutomatonState.TransitionType.EPSILON);
  }

}
