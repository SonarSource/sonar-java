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
package org.sonar.java.regex;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.ast.DisjunctionTree;
import org.sonar.java.regex.ast.CapturingGroupTree;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.java.regex.ast.SimpleQuantifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertLocation;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainString;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;

class CombinedTests {

  @Test
  void testNonTrivialRegex() {
    RegexTree regex = assertSuccessfulParse("(ab|b)*(||)");
    assertLocation(0, 11, regex);
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    assertFalse(regex.is(RegexTree.Kind.DISJUNCTION), "`is` should return false when kinds don't match");
    SequenceTree sequence = assertType(SequenceTree.class, regex);
    List<RegexTree> items = sequence.getItems();
    assertEquals(2, items.size(), "The sequence should have two elements.");

    assertKind(RegexTree.Kind.REPETITION, items.get(0));
    RepetitionTree firstPart = assertType(RepetitionTree.class, items.get(0));
    assertLocation(0, 7, firstPart);
    assertLocation(0, 6, firstPart.getElement());
    assertLocation(6, 7, firstPart.getQuantifier());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, firstPart.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.STAR, quantifier.getKind(), "Quantifier should be a star.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
    assertKind(RegexTree.Kind.CAPTURING_GROUP, firstPart.getElement());
    CapturingGroupTree repeatedGroup = assertType(CapturingGroupTree.class, firstPart.getElement());
    assertKind(RegexTree.Kind.DISJUNCTION, repeatedGroup.getElement());
    DisjunctionTree repeatedDisjunction = assertType(DisjunctionTree.class, repeatedGroup.getElement());
    List<RegexTree> repeatedAlternatives = repeatedDisjunction.getAlternatives();
    assertEquals(2, repeatedAlternatives.size(), "First disjunction should have two alternatives.");
    assertPlainString("ab", repeatedAlternatives.get(0));
    assertPlainCharacter('b', repeatedAlternatives.get(1));

    assertKind(RegexTree.Kind.CAPTURING_GROUP, items.get(1));
    CapturingGroupTree secondPart = assertType(CapturingGroupTree.class, items.get(1));
    assertLocation(7, 11, secondPart);
    assertKind(RegexTree.Kind.DISJUNCTION, secondPart.getElement());
    DisjunctionTree disjunction = assertType(DisjunctionTree.class, secondPart.getElement());
    List<RegexTree> alternatives = disjunction.getAlternatives();
    assertEquals(3, alternatives.size(), "Second disjunction should have three alternatives");
    for (RegexTree alternative : alternatives) {
      assertKind(RegexTree.Kind.SEQUENCE, alternative);
      SequenceTree empty = assertType(SequenceTree.class, alternative);
      assertEquals(0, empty.getItems().size(), "Second disjunction should contain only empty sequences.");
    }
  }

  @Test
  void testNonTrivialRegexInFreeSpacingMode() {
    RegexTree regex = assertSuccessfulParse("(ab | b ) #this is a comment\\n*\\\\#(||)#this is another comment", true);
    assertLocation(0, 62, regex);
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    assertFalse(regex.is(RegexTree.Kind.DISJUNCTION), "`is` should return false when kinds don't match");
    SequenceTree sequence = assertType(SequenceTree.class, regex);
    List<RegexTree> items = sequence.getItems();
    assertEquals(3, items.size(), "The sequence should have three elements.");

    assertKind(RegexTree.Kind.REPETITION, items.get(0));
    RepetitionTree firstPart = assertType(RepetitionTree.class, items.get(0));
    assertLocation(0, 31, firstPart);
    assertLocation(0, 30, firstPart.getElement());
    assertLocation(30, 31, firstPart.getQuantifier());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, firstPart.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.STAR, quantifier.getKind(), "Quantifier should be a star.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
    assertKind(RegexTree.Kind.CAPTURING_GROUP, firstPart.getElement());
    CapturingGroupTree repeatedGroup = assertType(CapturingGroupTree.class, firstPart.getElement());
    assertKind(RegexTree.Kind.DISJUNCTION, repeatedGroup.getElement());
    DisjunctionTree repeatedDisjunction = assertType(DisjunctionTree.class, repeatedGroup.getElement());
    List<RegexTree> repeatedAlternatives = repeatedDisjunction.getAlternatives();
    assertEquals(2, repeatedAlternatives.size(), "First disjunction should have two alternatives.");
    assertPlainString("ab", repeatedAlternatives.get(0));
    assertPlainCharacter('b', repeatedAlternatives.get(1));

    assertKind(RegexTree.Kind.PLAIN_CHARACTER, items.get(1));
    assertLocation(31, 34, items.get(1));
    assertPlainCharacter('#', items.get(1));

    assertKind(RegexTree.Kind.CAPTURING_GROUP, items.get(2));
    CapturingGroupTree thirdPart = assertType(CapturingGroupTree.class, items.get(2));
    assertLocation(34, 62, thirdPart);
    assertKind(RegexTree.Kind.DISJUNCTION, thirdPart.getElement());
    DisjunctionTree disjunction = assertType(DisjunctionTree.class, thirdPart.getElement());
    List<RegexTree> alternatives = disjunction.getAlternatives();
    assertEquals(3, alternatives.size(), "Second disjunction should have three alternatives");
    for (RegexTree alternative : alternatives) {
      assertKind(RegexTree.Kind.SEQUENCE, alternative);
      SequenceTree empty = assertType(SequenceTree.class, alternative);
      assertEquals(0, empty.getItems().size(), "Second disjunction should contain only empty sequences.");
    }
  }

}
