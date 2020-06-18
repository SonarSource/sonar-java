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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.opentest4j.AssertionFailedError;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.regex.ast.CharacterClassTree;
import org.sonar.java.regex.ast.CharacterRangeTree;
import org.sonar.java.regex.ast.FlagSet;
import org.sonar.java.regex.ast.PlainCharacterTree;
import org.sonar.java.regex.ast.RegexSource;
import org.sonar.java.regex.ast.RegexSyntaxElement;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegexParserTestUtils {

  private RegexParserTestUtils() {}

  public static RegexTree assertSuccessfulParse(String regex) {
    return assertSuccessfulParse(regex, false);
  }

  public static RegexTree assertSuccessfulParse(String regex, boolean freeSpacingMode) {
    return assertSuccessfulParseResult(regex, freeSpacingMode).getResult();
  }

  public static RegexParseResult assertSuccessfulParseResult(String regex) {
    return assertSuccessfulParseResult(regex, false);
  }

  public static RegexParseResult assertSuccessfulParseResult(String regex, boolean freeSpacingMode) {
    RegexParseResult result = parseRegex(regex, freeSpacingMode);
    if (!result.getSyntaxErrors().isEmpty()) {
      throw new AssertionFailedError("Parsing should complete with no errors.", "no errors", result.getSyntaxErrors());
    }
    return result;
  }

  public static RegexParseResult parseRegex(String regex, boolean freeSpacingMode) {
    int initialFlags = freeSpacingMode ? Pattern.COMMENTS : 0;
    RegexSource source = makeSource(regex);
    RegexParseResult result = new RegexParser(source, new FlagSet(initialFlags)).parse();
    assertEquals(initialFlags, result.getInitialFlags().getMask(), "The initial flags in result should match those passed in.");
    return result;
  }

  public static RegexParseResult parseRegex(String regex) {
    return parseRegex(regex, false);
  }

  public static void assertFailParsing(String regex, String expectedError) {
    RegexParseResult result = parseRegex(regex);
    List<SyntaxError> errors = result.getSyntaxErrors();
    if (errors.isEmpty()) {
      throw new AssertionFailedError("Expected error in parsing");
    }
    assertThat(errors.stream().map(SyntaxError::getMessage)).contains(expectedError);
  }

  public static void assertPlainString(String expected, RegexTree regex) {
    SequenceTree sequence = assertType(SequenceTree.class, regex);
    int expectedSize = expected.length();
    if (sequence.getItems().size() != expectedSize) {
      throw new AssertionFailedError("Expected a string of " + expectedSize + " characters, but got " + sequence.getItems());
    }
    for (int i = 0; i < expectedSize; i++) {
      assertPlainCharacter(expected.charAt(i), sequence.getItems().get(i));
    }
  }

  public static void assertPlainCharacter(char expected, RegexTree regex) {
    assertKind(RegexTree.Kind.PLAIN_CHARACTER, regex);
    PlainCharacterTree characterTree = assertType(PlainCharacterTree.class, regex);
    assertEquals(expected, characterTree.getCharacter(), "Regex should contain the right characters.");
  }

  public static void assertPlainCharacter(char expected, String regex) {
    assertPlainCharacter(expected, assertSuccessfulParse(regex));
  }

  public static RegexTree assertCharacterClass(boolean expectNegated, RegexTree actual) {
    CharacterClassTree characterClass = assertType(CharacterClassTree.class, actual);
    if (expectNegated) {
      assertTrue(characterClass.isNegated(), "Character class should be negated.");
    } else {
      assertFalse(characterClass.isNegated(), "Character class should not be negated.");
    }
    return characterClass.getContents();
  }

  public static void assertCharacterRange(char expectedLowerBound, char expectedUpperBound, RegexTree actual) {
    CharacterRangeTree range = assertType(CharacterRangeTree.class, actual);
    assertEquals(expectedLowerBound, range.getLowerBound().getCharacter(), "Lower bound should be '" + expectedLowerBound + "'.");
    assertEquals(expectedUpperBound, range.getUpperBound().getCharacter(), "Upper bound should be '" + expectedUpperBound + "'.");
  }

  public static <T> T assertType(Class<T> klass, Object o) {
    String actual = o.getClass().getSimpleName();
    String expected = klass.getSimpleName();
    if (!klass.isInstance(o)) {
      throw new AssertionFailedError("Object should have the correct type. ", expected, actual);
    }
    return klass.cast(o);
  }

  public static <T> void assertListSize(int expected, List<T> actual) {
    if (actual.size() != expected) {
      throw new AssertionFailedError("List should have the expected size.", "list of size " + expected, actual);
    }
  }

  @SafeVarargs
  public static <T> void assertListElements(List<T> actual, Consumer<T>... assertions) {
    assertListSize(assertions.length, actual);
    for (int i = 0; i < actual.size(); i++) {
      assertions[i].accept(actual.get(i));
    }
  }

  public static void assertKind(RegexTree.Kind expected, RegexTree actual) {
    assertEquals(expected, actual.kind(), "Regex should have kind " + expected);
    assertTrue(actual.is(expected), "`is` should return true when the kinds match.");
    assertTrue(actual.is(RegexTree.Kind.PLAIN_CHARACTER, RegexTree.Kind.DISJUNCTION, expected), "`is` should return true when one of the kinds match.");
  }

  public static void assertLocation(int expectedStart, int expectedEnd, RegexSyntaxElement element) {
    assertEquals(expectedStart, element.getRange().getBeginningOffset(), "Element should start at the given index.");
    assertEquals(expectedEnd, element.getRange().getEndingOffset(), "Element should end at the given index.");
  }

  // place the String which will contain the regex on 3rd line, starting from index 0
  private static final String JAVA_CODE = "class Foo {\n  String str = \n\"%s\";\n}";

  private static RegexSource makeSource(String content) {
    CompilationUnitTree tree = JParserTestUtils.parse(String.format(JAVA_CODE, content));
    ClassTree foo = (ClassTree) tree.types().get(0);
    VariableTree str = (VariableTree) foo.members().get(0);
    LiteralCollector visitor = new LiteralCollector();
    str.initializer().accept(visitor);
    return new RegexSource(visitor.stringLiterals);
  }

  private static class LiteralCollector extends BaseTreeVisitor {

    private final List<LiteralTree> stringLiterals = new ArrayList<>();

    @Override
    public void visitLiteral(LiteralTree tree) {
      if (tree.is(Tree.Kind.STRING_LITERAL)) {
        stringLiterals.add(tree);
      }
    }
  }
}
