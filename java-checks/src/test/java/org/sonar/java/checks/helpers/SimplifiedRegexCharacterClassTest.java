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
import org.sonar.java.regex.ast.FlagSet;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.MiscEscapeSequenceTree;
import org.sonar.java.regex.ast.RegexSource;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplifiedRegexCharacterClassTest {

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
  void superset_of_characters_or_range() {
    int flags = 0;
    assertSupersetOf("a", "a", false, flags).isTrue();
    assertSupersetOf("a", "b", true, flags).isFalse();
    assertSupersetOf("[b-d]", "a", true, flags).isFalse();
    assertSupersetOf("[b-d]", "b", false, flags).isTrue();
    assertSupersetOf("[b-d]", "c", false, flags).isTrue();
    assertSupersetOf("[b-d]", "d", false, flags).isTrue();
    assertSupersetOf("[b-d]", "e", true, flags).isFalse();
    assertSupersetOf("a", "[a]", false, flags).isTrue();
    assertSupersetOf("a", "[ab]", true, flags).isFalse();
    assertSupersetOf("[a-z]", "[b-e]", false, flags).isTrue();
    assertSupersetOf("[a-z]", "[b-e]", false, flags).isTrue();
    assertSupersetOf("[a-d]", "[a-e]", true, flags).isFalse();
    assertSupersetOf("[a-d]", "[b-e]", true, flags).isFalse();
    assertSupersetOf("[b-d]", "[a-d]", true, flags).isFalse();
    assertSupersetOf("[a-c]", "[a-c]", false, flags).isTrue();
    assertSupersetOf("[a-ce-g]", "[a-ce-g]", false, flags).isTrue();
    assertSupersetOf("[a-g]", "[a-de-g]", false, flags).isTrue();
    assertSupersetOf("[a-de-g]", "[a-g]", false, flags).isTrue();
    assertSupersetOf("[b-dg-i]", "[a-dg-i]", true, flags).isFalse();
    assertSupersetOf("[b-dg-i]", "[b-eg-i]", true, flags).isFalse();
    assertSupersetOf("[b-dg-i]", "[b-df-i]", true, flags).isFalse();
    assertSupersetOf("[b-dg-i]", "[b-dg-j]", true, flags).isFalse();
  }

  @Test
  void superset_of_predefined_character_classes() {
    int flags = 0;
    assertSupersetOf("\\d", "[0-9]", false, flags).isTrue();
    assertSupersetOf("[0-9]", "\\d", false, flags).isTrue();
    assertSupersetOf("\\d", "[2-5]", false, flags).isTrue();
    assertSupersetOf("[2-5]", "\\d", false, flags).isFalse();
    assertSupersetOf("[^a-z]", "\\d", false, flags).isTrue();
    assertSupersetOf("\\d", "[^a-z]", true, flags).isFalse();
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

  private static AbstractBooleanAssert<?> assertSupersetOf(String superset, String subset, boolean defaultAnswer, int flags) {
    FlagSet flagSet = new FlagSet(flags);
    RegexTree supersetResult = parseRegex(superset, flagSet).getResult();
    RegexTree subsetResult = parseRegex(subset, flagSet).getResult();
    SimplifiedRegexCharacterClass supersetCharacterClass = new SimplifiedRegexCharacterClass((CharacterClassElementTree) supersetResult);
    SimplifiedRegexCharacterClass subsetCharacterClass = new SimplifiedRegexCharacterClass((CharacterClassElementTree) subsetResult);
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
