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

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sonar.java.regex.RegexParserTestUtils.assertJavaCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertListElements;
import static org.sonar.java.regex.RegexParserTestUtils.assertCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertSingleEdge;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;

class DisjunctionTreeTest {

  @ParameterizedTest
  @MethodSource("provideDisjunctionWithTwoAlternatives")
  void disjunctionWithTwoAlternatives(String regex, int expectedCharacterIndex) {
    DisjunctionTree disjunction = assertType(DisjunctionTree.class, assertSuccessfulParse(regex));
    assertListElements(disjunction.getAlternatives(),
      first -> assertCharacter('a', first),
      second -> assertCharacter('b', second)
    );
    assertListElements(disjunction.getOrOperators(),
      first -> assertJavaCharacter(expectedCharacterIndex, '|', first)
    );

    FinalState finalState = assertType(FinalState.class, disjunction.continuation());
    assertEquals(AutomatonState.TransitionType.EPSILON, disjunction.incomingTransitionType());
    assertEquals(disjunction.getAlternatives(), disjunction.successors());
    assertListElements(disjunction.successors(),
      first -> testAlternative(first, finalState),
      second -> testAlternative(second, finalState)
    );
  }

  private static Stream<Arguments> provideDisjunctionWithTwoAlternatives() {
    return Stream.of(
      Arguments.of("a|b", 1),
      Arguments.of("\\\\Qa\\\\E|b", 7),
      Arguments.of("a\\\\Q\\\\E|b\\\\Q\\\\E", 7)
    );
  }

  @Test
  void disjunctionWithThreeAlternatives() {
    DisjunctionTree disjunction = assertType(DisjunctionTree.class, assertSuccessfulParse("a|b|c"));
    assertListElements(disjunction.getAlternatives(),
      first -> assertCharacter('a', first),
      second -> assertCharacter('b', second),
      third -> assertCharacter('c', third)
    );
    assertListElements(disjunction.getOrOperators(),
      first -> assertJavaCharacter(1, '|', first),
      second -> assertJavaCharacter(3, '|', second)
    );

    FinalState finalState = assertType(FinalState.class, disjunction.continuation());
    assertEquals(AutomatonState.TransitionType.EPSILON, disjunction.incomingTransitionType());
    assertEquals(disjunction.getAlternatives(), disjunction.successors());
    assertListElements(disjunction.successors(),
      first -> testAlternative(first, finalState),
      second -> testAlternative(second, finalState),
      third -> testAlternative(third, finalState)
    );
  }

  private void testAlternative(AutomatonState alternative, FinalState finalState) {
    assertEquals(AutomatonState.TransitionType.CHARACTER, alternative.incomingTransitionType());
    assertSingleEdge(alternative, finalState, AutomatonState.TransitionType.EPSILON);
  }

}
