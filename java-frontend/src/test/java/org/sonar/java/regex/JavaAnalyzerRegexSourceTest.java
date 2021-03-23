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
import org.sonar.java.AnalyzerMessage;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;

class JavaAnalyzerRegexSourceTest {

  @Test
  void test_text_blocks() {
    RegexTree regex = assertSuccessfulParse("\"\"\n    ab\n    cd\"\"");
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
    assertTextSpan(4, 6, 7, items.get(2));
    assertTextSpan(5, 4, 5, items.get(3));
    assertTextSpan(5, 5, 6, items.get(4));

    List<AnalyzerMessage.TextSpan> spans = ((JavaAnalyzerRegexSource) regex.getSource()).textSpansFor(regex.getRange());
    assertTextSpan(4, 4, 7, spans.get(0));
    assertTextSpan(5, 4, 6, spans.get(1));
  }

  private void assertCharacter(char expected, RegexTree tree) {
    assertKind(RegexTree.Kind.CHARACTER, tree);
    assertEquals(expected, ((CharacterTree) tree).codePointOrUnit());
  }

  private void assertTextSpan(int line, int startColumn, int endColumn, RegexTree tree) {
    List<AnalyzerMessage.TextSpan> spans = ((JavaAnalyzerRegexSource) tree.getSource()).textSpansFor(tree.getRange());
    assertEquals(1, spans.size());
    assertTextSpan(line, startColumn, endColumn, spans.get(0));
  }

  private void assertTextSpan(int line, int startColumn, int endColumn, AnalyzerMessage.TextSpan span) {
    assertEquals(line, span.startLine);
    assertEquals(line, span.endLine);
    assertEquals(startColumn, span.startCharacter);
    assertEquals(endColumn, span.endCharacter);
  }

}
