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
package org.sonar.java.regex;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.RegexParser;
import org.sonarsource.analyzer.commons.regex.RegexSource;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassElementTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassUnionTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;

class JavaAnalyzerRegexSourceTest {

  @Test
  void invalid_regex() {
    RegexSource source = RegexParserTestUtils.makeSource("(");
    RegexParseResult result = new RegexParser(source, new FlagSet()).parse();
    if (result.getSyntaxErrors().isEmpty()) {
      fail("should have encountered syntax error");
    }
    RegexSyntaxElement offendingSyntaxElement = result.getSyntaxErrors().get(0).getOffendingSyntaxElement();
    List<TextSpan> items = ((JavaAnalyzerRegexSource) offendingSyntaxElement.getSource()).textSpansFor(offendingSyntaxElement.getRange());
    assertThat(items).hasSize(1);

    assertTextSpan(3, 2, 3, items.get(0));
  }

  @Test
  void test_string_literal() {
    RegexTree regex = assertSuccessfulParse("a\\nb");
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    List<RegexTree> items = ((SequenceTree) regex).getItems();
    assertThat(items).hasSize(3);

    assertCharacter('a', items.get(0));
    assertCharacter('\n', items.get(1));
    assertCharacter('b', items.get(2));

    assertTextSpan(3, 1, 2, items.get(0));
    assertTextSpan(3, 2, 4, items.get(1));
    assertTextSpan(3, 4, 5, items.get(2));
  }

  @Test
  void test_empty_lines() {
    RegexTree regex = assertSuccessfulParse(
      "\"\"\n"                    // line 3: start of textblock
        + "    [\n"               // line 4: start character-class
        + "\n"                    // line 5: empty line
        + "                \r\n"  // line 6: only spaces
        + "    ]\"\"");           // line 7: end character-class and end of textblock
    assertKind(RegexTree.Kind.CHARACTER_CLASS, regex);
    CharacterClassTree characterClass = ((CharacterClassTree) regex);
    List<CharacterClassElementTree> characterClasses = ((CharacterClassUnionTree) characterClass.getContents()).getCharacterClasses();
    assertThat(characterClasses).hasSize(3);
    // only contains endlines
    assertCharacter('\n', (RegexTree) characterClasses.get(0));
    assertCharacter('\n', (RegexTree) characterClasses.get(1));
    assertCharacter('\n', (RegexTree) characterClasses.get(2));

    assertLineTextSpan(4, (RegexTree) characterClasses.get(0));
    assertLineTextSpan(5, (RegexTree) characterClasses.get(1));
    assertLineTextSpan(6, (RegexTree) characterClasses.get(2));
  }

  @Test
  void test_text_blocks() {
    RegexTree regex = assertSuccessfulParse(
      "\"\"\n"           // line 3
        + "    ab\n"     // line 4
        + "    cd\"\""); // line 5
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    List<RegexTree> items = ((SequenceTree) regex).getItems();
    assertEquals(5, items.size());

    assertCharacter('a', items.get(0));
    assertCharacter('b', items.get(1));
    assertCharacter('\n', items.get(2));
    assertCharacter('c', items.get(3));
    assertCharacter('d', items.get(4));

    assertTextSpan(4, 4, 5, items.get(0));
    assertTextSpan(4, 5, 6, items.get(1));
    assertLineTextSpan(4, items.get(2));
    assertTextSpan(5, 4, 5, items.get(3));
    assertTextSpan(5, 5, 6, items.get(4));

    List<AnalyzerMessage.TextSpan> spans = ((JavaAnalyzerRegexSource) regex.getSource()).textSpansFor(regex.getRange());
    assertTextSpan(4, 4, 6, spans.get(0));
    assertTextSpan(5, 4, 6, spans.get(1));
  }

  private static void assertCharacter(char expected, RegexTree tree) {
    assertKind(RegexTree.Kind.CHARACTER, tree);
    assertEquals(expected, ((CharacterTree) tree).codePointOrUnit());
  }

  private static void assertLineTextSpan(int line, RegexTree tree) {
    assertTextSpan(line, -1, -1, tree);
  }

  private static void assertTextSpan(int line, int startColumn, int endColumn, RegexTree tree) {
    List<AnalyzerMessage.TextSpan> spans = ((JavaAnalyzerRegexSource) tree.getSource()).textSpansFor(tree.getRange());
    assertEquals(1, spans.size());
    assertTextSpan(line, startColumn, endColumn, spans.get(0));
  }

  private static void assertTextSpan(int line, int startColumn, int endColumn, AnalyzerMessage.TextSpan span) {
    assertEquals(line, span.startLine, () -> String.format("Expected line to be '%d' but got '%d'", line, span.startLine));
    assertEquals(line, span.endLine, () -> String.format("Expected line to be '%d' but got '%d'", line, span.endLine));
    assertEquals(startColumn, span.startCharacter, () -> String.format("Expected start character to be '%d' but got '%d'", startColumn, span.startCharacter));
    assertEquals(endColumn, span.endCharacter, () -> String.format("Expected end character to be '%d' but got '%d'", endColumn, span.endCharacter));
  }

}
