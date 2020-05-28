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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.regex.ast.CurlyBraceQuantifier;
import org.sonar.java.regex.ast.DisjunctionTree;
import org.sonar.java.regex.ast.GroupTree;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.Location;
import org.sonar.java.regex.ast.PlainCharacterTree;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexSource;
import org.sonar.java.regex.ast.RegexSyntaxElement;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.java.regex.ast.SimpleQuantifier;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexParserTests {

  @Test
  void testCurlyBracedQuantifier() {
    RegexTree regex = assertSuccessfulParse("x{23,42}");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    CurlyBraceQuantifier quantifier = assertType(CurlyBraceQuantifier.class, repetition.getQuantifier());
    assertEquals(23, quantifier.getMinimumRepetitions(), "Lower bound should be 23.");
    assertEquals(42, quantifier.getMaximumRepetitions(), "Upper bound should be 42.");
    assertFalse(quantifier.isOpenEnded(), "Quantifier should not be open ended.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
  }

  @Test
  void testCurlyBracedQuantifierWithNoUpperBound() {
    RegexTree regex = assertSuccessfulParse("x{42,}");
    RepetitionTree repetition = assertType(RepetitionTree.class, regex);
    assertPlainCharacter('x', repetition.getElement());
    CurlyBraceQuantifier quantifier = assertType(CurlyBraceQuantifier.class, repetition.getQuantifier());
    assertEquals(42, quantifier.getMinimumRepetitions(), "Lower bound should be 42.");
    assertNull(quantifier.getMaximumRepetitions(), "Quantifier should be open ended.");
    assertTrue(quantifier.isOpenEnded(), "Quantifier should be open ended.");
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
  void testCurlyBracedQuantifierWithSyntaxError() {
    RegexParseResult result = new RegexParser(makeSource("x{a}")).parse();
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Integer expected", error.getMessage(), "Error should have the right message.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(2,3), locations.get(0).getIndexRange(), "Error should have the right location.");
  }

  @Test
  void testNonTrivialRegex() {
    RegexTree regex = assertSuccessfulParse("(ab|b)*(||)");
    assertLocation(0, 11, regex);
    SequenceTree sequence = assertType(SequenceTree.class, regex);
    List<RegexTree> items = sequence.getItems();
    assertEquals(2, items.size(), "The sequence should have two elements.");

    RepetitionTree firstPart = assertType(RepetitionTree.class, items.get(0));
    assertLocation(0, 7, firstPart);
    assertLocation(0, 6, firstPart.getElement());
    assertLocation(6, 7, firstPart.getQuantifier());
    SimpleQuantifier quantifier = assertType(SimpleQuantifier.class, firstPart.getQuantifier());
    assertEquals(SimpleQuantifier.Kind.STAR, quantifier.getKind(), "Quantifier should be a star.");
    assertEquals(Quantifier.Modifier.GREEDY, quantifier.getModifier(), "Quantifier should be greedy.");
    GroupTree repeatedGroup = assertType(GroupTree.class, firstPart.getElement());
    DisjunctionTree repeatedDisjunction = assertType(DisjunctionTree.class, repeatedGroup.getElement());
    List<RegexTree> repeatedAlternatives = repeatedDisjunction.getAlternatives();
    assertEquals(2, repeatedAlternatives.size(), "First disjunction should have two alternatives.");
    assertPlainString("ab", repeatedAlternatives.get(0));
    assertPlainCharacter('b', repeatedAlternatives.get(1));

    GroupTree secondPart = assertType(GroupTree.class, items.get(1));
    assertLocation(7, 11, secondPart);
    DisjunctionTree disjunction = assertType(DisjunctionTree.class, secondPart.getElement());
    List<RegexTree> alternatives = disjunction.getAlternatives();
    assertEquals(3, alternatives.size(), "Second disjunction should have three alternatives");
    for (RegexTree alternative : alternatives) {
      SequenceTree empty = assertType(SequenceTree.class, alternative);
      assertEquals(0, empty.getItems().size(), "Second disjunction should contain only empty sequences.");
    }
  }

  private static RegexTree assertSuccessfulParse(String regex) {
    RegexSource source = makeSource(regex);
    RegexParseResult result = new RegexParser(source).parse();
    if (!result.getSyntaxErrors().isEmpty()) {
      throw new AssertionFailedError("Parsing should complete with no errors.", "no errors", result.getSyntaxErrors());
    }
    return result.getResult();
  }

  private static void assertPlainString(String expected, RegexTree regex) {
    SequenceTree sequence = assertType(SequenceTree.class, regex);
    int expectedSize = expected.length();
    if (sequence.getItems().size() != expectedSize) {
      throw new AssertionFailedError("Expected a string of " + expectedSize + " characters, but got " + sequence.getItems());
    }
    for (int i = 0; i < expectedSize; i++) {
      assertPlainCharacter(expected.charAt(i), sequence.getItems().get(i));
    }
  }

  private static void assertPlainCharacter(char expected, RegexTree regex) {
    PlainCharacterTree characterTree = assertType(PlainCharacterTree.class, regex);
    assertEquals(expected, characterTree.getCharacter(), "Regex should contain the right characters.");
  }

  private static <T> T assertType(Class<T> klass, Object o) {
    String actual = o.getClass().getSimpleName();
    String expected = klass.getSimpleName();
    if (!klass.isInstance(o)) {
      throw new AssertionFailedError("Object should have the correct type. ", expected, actual);
    }
    return klass.cast(o);
  }

  private static void assertLocation(int expectedStart, int expectedEnd, RegexSyntaxElement element) {
    assertEquals(expectedStart, element.getRange().getBeginningOffset(), "Element should start at the given index.");
    assertEquals(expectedEnd, element.getRange().getEndingOffset(), "Element should end at the given index.");
  }

  private final static String JAVA_HEADER = "class Foo { String str = \"";

  private static RegexSource makeSource(String content) {
    CompilationUnitTree tree = JParserTestUtils.parse(JAVA_HEADER + content +"\"; }");
    LiteralTree literal = (LiteralTree) ((VariableTree)((ClassTree)tree.types().get(0)).members().get(0)).initializer();
    return new RegexSource(Collections.singletonList(literal));
  }

}
