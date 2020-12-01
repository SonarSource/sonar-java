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
package org.sonar.java.checks.helpers;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.RegexParser;
import org.sonar.java.regex.ast.CharacterClassElementTree;
import org.sonar.java.regex.ast.DotTree;
import org.sonar.java.regex.ast.FlagSet;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.MiscEscapeSequenceTree;
import org.sonar.java.regex.ast.PlainCharacterTree;
import org.sonar.java.regex.ast.RegexSource;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplifiedRegexCharacterClassTest {

  public static final int NO_FLAGS = 0;

  @Test
  void testIntersectionWithTrueAsDefaultAnswer() {
    RegexSource dummySource = new RegexSource(Collections.emptyList());
    IndexRange dummyRange = new IndexRange(0, 0);
    CharacterClassElementTree dummyTree = new MiscEscapeSequenceTree(dummySource, dummyRange, new FlagSet());

    SimplifiedRegexCharacterClass aToZ = new SimplifiedRegexCharacterClass();
    aToZ.addRange('a', 'z', dummyTree);
    SimplifiedRegexCharacterClass unknown = new SimplifiedRegexCharacterClass();
    unknown.add(dummyTree);
    SimplifiedRegexCharacterClass empty = new SimplifiedRegexCharacterClass();

    assertTrue(aToZ.intersects(unknown, true));
    assertFalse(aToZ.intersects(empty, true));
    assertFalse(unknown.intersects(empty, true));

    assertTrue(unknown.intersects(aToZ, true));
    assertFalse(empty.intersects(aToZ, true));
    assertFalse(empty.intersects(unknown, true));
  }

  @Test
  void intersects_dot() {
    assertIntersects(".", "a", false, NO_FLAGS).isTrue();
    assertIntersects(".", ".", false, NO_FLAGS).isTrue();
    assertIntersects(".", "\0", false, NO_FLAGS).isTrue();
    assertIntersects(".", "\uFFFF", false, NO_FLAGS).isTrue();
    assertIntersects("a", ".", true, NO_FLAGS).isTrue();
    assertIntersects(".", "[a-z]", false, NO_FLAGS).isTrue();
    assertIntersects("[a-z]", ".", false, NO_FLAGS).isTrue();

    // by default [\r\n\u0085\u2028\u2029] excluded from DotTree
    assertIntersects(".", "[\r\n\u0085\u2028\u2029]", false, NO_FLAGS).isFalse();
    assertIntersects(".", "\b", false, NO_FLAGS).isTrue();
    assertIntersects(".", "\f", false, NO_FLAGS).isTrue();
    assertIntersects(".", " ", false, NO_FLAGS).isTrue();
    assertIntersects(".", "\t", false, NO_FLAGS).isTrue();
  }
   
  @Test
  void intersects_dot_unix_lines() {
    // only \n excluded when UNIX_LINES is set
    assertIntersects(".", "\n", false, Pattern.UNIX_LINES).isFalse();
    assertIntersects(".", "[^a]", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects(".", "\b", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects(".", "\t", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects(".", "\f", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects(".", " ", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects(".", "\r", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects(".", "\u0085", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects(".", "\u2028", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects(".", "\u2029", false, Pattern.UNIX_LINES).isTrue();
    assertIntersects(".", ".", false, Pattern.UNIX_LINES).isTrue();
  }

  @Test
  void intersects_dot_all() {
    // no exclusion and UNIX_LINES is ignored when DOTALL is set
    assertIntersects(".", "\n", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", "[^a]", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", "\n", false, Pattern.UNIX_LINES | Pattern.DOTALL).isTrue();
    assertIntersects(".", "\b", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", "\t", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", "\f", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", " ", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", "\r", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", "\u0085", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", "\u2028", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", "\u2029", false, Pattern.DOTALL).isTrue();
    assertIntersects(".", ".", false, Pattern.DOTALL).isTrue();
  }

  @Test
  void intersects_with_utf16() {
    String maxCodePoint = new String(Character.toChars(Character.MAX_CODE_POINT));
    // two characters
    assertThat(maxCodePoint).hasSize(2);
    // but a single code point
    assertThat(maxCodePoint.codePoints().count()).isEqualTo(1);
    // and java Pattern support it
    assertThat(maxCodePoint).matches(".");

    RegexTree result = parseRegex(maxCodePoint, new FlagSet()).getResult();
    // FIXME result should be a single PlainCharacterTree and not a SequenceTree of two PlainCharacterTree s
    // In oder to assert: assertIntersects(".", maxCodePoint, false, NO_FLAGS).isTrue();
    // Instead of:
    assertThat(result).isInstanceOf(SequenceTree.class);
  }

  @Test
  void superset_of_characters_or_range() {
    assertSupersetOf("a", "a", false, NO_FLAGS).isTrue();
    assertSupersetOf("a", "b", true, NO_FLAGS).isFalse();
    assertSupersetOf("[b-d]", "a", true, NO_FLAGS).isFalse();
    assertSupersetOf("[b-d]", "b", false, NO_FLAGS).isTrue();
    assertSupersetOf("[b-d]", "c", false, NO_FLAGS).isTrue();
    assertSupersetOf("[b-d]", "d", false, NO_FLAGS).isTrue();
    assertSupersetOf("[b-d]", "e", true, NO_FLAGS).isFalse();
    assertSupersetOf("a", "[a]", false, NO_FLAGS).isTrue();
    assertSupersetOf("a", "[ab]", true, NO_FLAGS).isFalse();
    assertSupersetOf("[a-z]", "[b-e]", false, NO_FLAGS).isTrue();
    assertSupersetOf("[a-z]", "[b-e]", false, NO_FLAGS).isTrue();
    assertSupersetOf("[a-d]", "[a-e]", true, NO_FLAGS).isFalse();
    assertSupersetOf("[a-d]", "[b-e]", true, NO_FLAGS).isFalse();
    assertSupersetOf("[b-d]", "[a-d]", true, NO_FLAGS).isFalse();
    assertSupersetOf("[a-c]", "[a-c]", false, NO_FLAGS).isTrue();
    assertSupersetOf("[a-ce-g]", "[a-ce-g]", false, NO_FLAGS).isTrue();
    assertSupersetOf("[a-g]", "[a-de-g]", false, NO_FLAGS).isTrue();
    assertSupersetOf("[a-de-g]", "[a-g]", false, NO_FLAGS).isTrue();
    assertSupersetOf("[b-dg-i]", "[a-dg-i]", true, NO_FLAGS).isFalse();
    assertSupersetOf("[b-dg-i]", "[b-eg-i]", true, NO_FLAGS).isFalse();
    assertSupersetOf("[b-dg-i]", "[b-df-i]", true, NO_FLAGS).isFalse();
    assertSupersetOf("[b-dg-i]", "[b-dg-j]", true, NO_FLAGS).isFalse();
  }

  @Test
  void superset_of_dot_default() {
    assertSupersetOf(".", "a", false, NO_FLAGS).isTrue();
    assertSupersetOf(".", "\0", false, NO_FLAGS).isTrue();
    assertSupersetOf(".", "\uFFFF", false, NO_FLAGS).isTrue();
    assertSupersetOf("a", ".", true, NO_FLAGS).isFalse();
    assertSupersetOf(".", "[a-z]", false, NO_FLAGS).isTrue();
    assertSupersetOf("[a-z]", ".", false, NO_FLAGS).isFalse();

    // by default [\r\n\u0085\u2028\u2029] excluded from DotTree
    assertSupersetOf(".", "[^\r\n\u0085\u2028\u2029]", false, NO_FLAGS).isTrue();
    assertSupersetOf(".", "\r", false, NO_FLAGS).isFalse();
    assertSupersetOf(".", "\n", false, NO_FLAGS).isFalse();
    assertSupersetOf(".", "\u0085", false, NO_FLAGS).isFalse();
    assertSupersetOf(".", "\u2028", false, NO_FLAGS).isFalse();
    assertSupersetOf(".", "\u2029", false, NO_FLAGS).isFalse();
    assertSupersetOf(".", "\b", false, NO_FLAGS).isTrue();
    assertSupersetOf(".", "\f", false, NO_FLAGS).isTrue();
    assertSupersetOf(".", " ", false, NO_FLAGS).isTrue();
    assertSupersetOf(".", "\t", false, NO_FLAGS).isTrue();
  }

  @Test
  void superset_of_dot_unix_lines() {
    // only \n excluded when UNIX_LINES is set
    assertSupersetOf(".", "[^\n]", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", "\r", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", "\n", false, Pattern.UNIX_LINES).isFalse();
    assertSupersetOf(".", "\u0085", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", "\u2028", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", "\u2029", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", "\b", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", "\f", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", " ", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", "\t", false, Pattern.UNIX_LINES).isTrue();
    assertSupersetOf(".", ".", false, Pattern.UNIX_LINES).isTrue();
  }

  @Test
  void superset_of_dot_all() {
    // no exclusion
    assertSupersetOf(".", "[^a]", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\r", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\n", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\u0085", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\u2028", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\u2029", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\b", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\f", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", " ", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", "\t", false, Pattern.DOTALL).isTrue();
    assertSupersetOf(".", ".", false, Pattern.DOTALL).isTrue();
    // UNIX_LINES is ignored when DOTALL is set
    assertSupersetOf(".", "\n", false, Pattern.UNIX_LINES|Pattern.DOTALL).isTrue();
  }

  @Test
  void superset_of_predefined_character_classes() {
    assertSupersetOf("\\d", "[0-9]", false, NO_FLAGS).isTrue();
    assertSupersetOf("[0-9]", "\\d", false, NO_FLAGS).isTrue();
    assertSupersetOf("\\d", "[2-5]", false, NO_FLAGS).isTrue();
    assertSupersetOf("[2-5]", "\\d", false, NO_FLAGS).isFalse();
    assertSupersetOf("[^a-z]", "\\d", false, NO_FLAGS).isTrue();
    assertSupersetOf("\\d", "[^a-z]", true, NO_FLAGS).isFalse();
  }

  @Test
  void superset_of_case_insensitive() {
    int flags = Pattern.CASE_INSENSITIVE;
    assertSupersetOf("A", "a", false, flags).isTrue();
    assertSupersetOf("a", "A", false, flags).isTrue();
    assertSupersetOf("[a-z]", "[B-F]", false, flags).isTrue();
    assertSupersetOf("[a-f]", "[B-Z]", true, flags).isFalse();
  }

  @Test
  void superset_of_default_answer() {
    int flags = Pattern.UNICODE_CHARACTER_CLASS;
    assertSupersetOf("[0-9]", "\\d", false, flags).isFalse();
    assertSupersetOf("[0-9]", "\\d", true, flags).isTrue();
  }

  @Test
  void superset_of_empty_set() {
    SimplifiedRegexCharacterClass empty = new SimplifiedRegexCharacterClass();
    assertThat(empty.supersetOf(empty, true)).isTrue();
    assertThat(empty.supersetOf(empty, false)).isTrue();
  }

  @Test
  void conversion() {
    PlainCharacterTree plainCharacterTree = (PlainCharacterTree) parseRegex("a", new FlagSet()).getResult();
    assertThat(SimplifiedRegexCharacterClass.of(plainCharacterTree)).isNotNull();

    DotTree dotTree = (DotTree) parseRegex(".", new FlagSet()).getResult();
    assertThat(SimplifiedRegexCharacterClass.of(dotTree)).isNotNull();

    SequenceTree sequenceTree = (SequenceTree) parseRegex("ab", new FlagSet()).getResult();
    assertThat(SimplifiedRegexCharacterClass.of(sequenceTree)).isNull();
  }

  private static AbstractBooleanAssert<?> assertIntersects(String regex1, String regex2, boolean defaultAnswer, int flags) {
    FlagSet flagSet = new FlagSet(flags);
    RegexTree tree1 = parseRegex(regex1, flagSet).getResult();
    RegexTree tree2 = parseRegex(regex2, flagSet).getResult();
    SimplifiedRegexCharacterClass characterClass1 = SimplifiedRegexCharacterClass.of(tree1);
    SimplifiedRegexCharacterClass characterClass2 = SimplifiedRegexCharacterClass.of(tree2);
    return assertThat(characterClass1.intersects(characterClass2, defaultAnswer));
  }

  private static AbstractBooleanAssert<?> assertSupersetOf(String superset, String subset, boolean defaultAnswer, int flags) {
    FlagSet flagSet = new FlagSet(flags);
    RegexTree supersetResult = parseRegex(superset, flagSet).getResult();
    RegexTree subsetResult = parseRegex(subset, flagSet).getResult();
    SimplifiedRegexCharacterClass supersetCharacterClass = SimplifiedRegexCharacterClass.of(supersetResult);
    SimplifiedRegexCharacterClass subsetCharacterClass = SimplifiedRegexCharacterClass.of(subsetResult);
    return assertThat(supersetCharacterClass.supersetOf(subsetCharacterClass, defaultAnswer));
  }

  static RegexParseResult parseRegex(String stringLiteral, FlagSet flagSet) {
    String literalSourceCode = "\"" + StringEscapeUtils.escapeJava(stringLiteral) + "\"";
    InternalSyntaxToken literalToken = new InternalSyntaxToken(1, 1, literalSourceCode, Collections.emptyList(), false);
    List<LiteralTree> literals = Collections.singletonList(new LiteralTreeImpl(Tree.Kind.STRING_LITERAL, literalToken));
    RegexParseResult result = new RegexParser(new RegexSource(literals), flagSet).parse();
    assertThat(result.getSyntaxErrors()).isEmpty();
    return result;
  }
}
