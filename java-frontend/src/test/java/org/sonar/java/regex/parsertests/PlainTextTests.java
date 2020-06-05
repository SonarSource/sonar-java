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
package org.sonar.java.regex.parsertests;

import org.junit.jupiter.api.Test;
import org.sonar.java.regex.ast.EscapedPropertyTree;
import org.sonar.java.regex.ast.RegexTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertFailParsing;

class PlainTextTests {

  @Test
  void testSimpleCharacter() {
    assertPlainCharacter('x', "x");
    assertPlainCharacter(' ', " ");
  }

  @Test
  void testSimpleEscapeSequences() {
    assertPlainCharacter('\b', "\\b");
    assertPlainCharacter('\t', "\\t");
    assertPlainCharacter('\n', "\\n");
    assertPlainCharacter('\f', "\\f");
    assertPlainCharacter('\r', "\\r");
    assertPlainCharacter('"', "\\\"");
  }

  @Test
  void octalEscapeSequences() {
    assertPlainCharacter('\n', "\\012");
    assertPlainCharacter('\n', "\\12");
    assertPlainCharacter('D', "\\104");
  }

  @Test
  void unicodeEscapeSequences() {
    assertPlainCharacter('\t', "\\u0009");
    assertPlainCharacter('D', "\\u0044");
    assertPlainCharacter('ö', "\\u00F6");
  }

  @Test
  void escapedMetaCharacters() {
    assertPlainCharacter('\\', "\\\\\\\\");
    assertPlainCharacter('.', "\\\\.");
    assertPlainCharacter('(', "\\\\(");
    assertPlainCharacter(')', "\\\\)");
    assertPlainCharacter('[', "\\\\[");
    assertPlainCharacter(']', "\\\\]");
    assertPlainCharacter('{', "\\\\{");
    assertPlainCharacter('}', "\\\\}");
  }

  @Test
  void unicodeRidiculousness() {
    assertPlainCharacter('\t', "\\u005ct");
    assertPlainCharacter('\\', "\\u005c\\u005c\\u005c\\u005c");
  }

  /**
   * Following the form \p{Prop} or \P{Prop} (negation)
   */
  @Test
  void escapedProperties() {
    // control
    assertThat("a".matches("\\p{Lower}")).isTrue();
    assertThat("A".matches("\\p{Lower}")).isFalse();

    // POSIX character classes
    assertEscapedProperty("\\\\p{Lower}", "Lower", false);
    assertEscapedProperty("\\\\p{Upper}", "Upper", false);
    assertEscapedProperty("\\\\p{ASCII}", "ASCII", false);
    assertEscapedProperty("\\\\p{Alpha}", "Alpha", false);
    assertEscapedProperty("\\\\p{Digit}", "Digit", false);
    assertEscapedProperty("\\\\p{Alnum}", "Alnum", false);
    assertEscapedProperty("\\\\p{Punct}", "Punct", false);
    assertEscapedProperty("\\\\p{Graph}", "Graph", false);
    assertEscapedProperty("\\\\p{Print}", "Print", false);
    assertEscapedProperty("\\\\p{Blank}", "Blank", false);
    assertEscapedProperty("\\\\p{Cntrl}", "Cntrl", false);
    assertEscapedProperty("\\\\p{XDigit}", "XDigit", false);
    assertEscapedProperty("\\\\p{Space}", "Space", false);

    // java.lang.Character classes
    assertEscapedProperty("\\\\p{javaLowerCase}", "javaLowerCase", false);
    assertEscapedProperty("\\\\p{javaUpperCase}", "javaUpperCase", false);
    assertEscapedProperty("\\\\p{javaWhitespace}", "javaWhitespace", false);
    assertEscapedProperty("\\\\p{javaMirrored}", "javaMirrored", false);

    // Classes for Unicode scripts, blocks, categories and binary properties
    assertEscapedProperty("\\\\p{IsLatin}", "IsLatin", false);
    assertEscapedProperty("\\\\p{InGreek}", "InGreek", false);
    assertEscapedProperty("\\\\p{Lu}", "Lu", false);
    assertEscapedProperty("\\\\p{IsAlphabetic}", "IsAlphabetic", false);
    assertEscapedProperty("\\\\p{Sc}", "Sc", false);

    // Negation
    assertEscapedProperty("\\\\P{InGreek}", "InGreek", true); // Any character except one in the Greek block

    // accept any property, even if it does not exists in hardcoded properties
    assertEscapedProperty("\\\\P{Cowabunga}", "Cowabunga", true); // Any character except one in the Greek block
  }

  @Test
  void failingInvalidEscapedProperties() {
    assertFailParsing("\\\\p", "Expected '{', but found the end of the regex");
    assertFailParsing("\\\\p{", "Expected a property name, but found the end of the regex");
    assertFailParsing("\\\\p{foo", "Expected '}', but found the end of the regex");
  }

  private static void assertEscapedProperty(String regex, String expectedProperty, boolean isNegation) {
    RegexTree tree = assertSuccessfulParse(regex);
    assertThat(tree).isInstanceOf(EscapedPropertyTree.class);

    EscapedPropertyTree escapedPropertyTree = (EscapedPropertyTree) tree;
    assertThat(escapedPropertyTree.property()).isEqualTo(expectedProperty);
    assertThat(escapedPropertyTree.isNegation()).isEqualTo(isNegation);
  }

}
