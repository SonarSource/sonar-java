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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.AnalyzerMessage.TextSpan;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.plugins.java.api.tree.LiteralTree;

public class JavaRegexSource implements RegexSource {

  private final String sourceText;

  /**
   * Maps an index of the regular expression to the TextSpan string literal that starts at the index
   */
  private final NavigableMap<Integer, TextSpan> indexToTextSpan;

  public JavaRegexSource(List<LiteralTree> stringLiterals) {
    StringBuilder sb = new StringBuilder();
    indexToTextSpan = new TreeMap<>();
    int index = 0;
    for (LiteralTree literal : stringLiterals) {
      String text = getString(literal);
      sb.append(text);
      TextSpan literalSpan = AnalyzerMessage.textSpanFor(literal);
      // Create a text span for the string with the quotes stripped out
      indexToTextSpan.put(index, new TextSpan(literalSpan.startLine, literalSpan.startCharacter + 1, literalSpan.endLine, literalSpan.endCharacter - 1));
      index += text.length();
    }
    sourceText = sb.toString();
  }

  @Override
  public String getSourceText() {
    return sourceText;
  }

  public List<TextSpan> textSpansFor(IndexRange range) {
    List<TextSpan> result = new ArrayList<>();
    Map.Entry<Integer, TextSpan> startEntry = indexToTextSpan.floorEntry(range.getBeginningOffset());
    if (range.getBeginningOffset() < 0) {
      startEntry = indexToTextSpan.firstEntry();
    }
    int startOffset = range.getBeginningOffset() - startEntry.getKey();
    Map.Entry<Integer, TextSpan> endEntry = indexToTextSpan.lowerEntry(range.getEndingOffset());
    if (range.getEndingOffset() <= 0) {
      endEntry = startEntry;
    }
    int endOffset = range.getEndingOffset() - endEntry.getKey();
    TextSpan startSpan = startEntry.getValue();
    TextSpan endSpan = endEntry.getValue();
    if (startSpan == endSpan) {
      // This assumes that startSpan.startLine == startSpan.endLine, which should always be the case
      result.add(new TextSpan(startSpan.startLine, startSpan.startCharacter + startOffset, startSpan.endLine, startSpan.startCharacter + endOffset));
    } else {
      result.add(new TextSpan(startSpan.startLine, startSpan.startCharacter + startOffset, startSpan.endLine, startSpan.endCharacter));
      int indexAfterStartSpan = startEntry.getKey() + startSpan.endCharacter - startSpan.startCharacter;
      Set<Map.Entry<Integer, TextSpan>> entries = indexToTextSpan.subMap(indexAfterStartSpan, endEntry.getKey())
        .entrySet();
      for (Map.Entry<Integer, TextSpan> entry : entries) {
        result.add(entry.getValue());
      }
      result.add(new TextSpan(endSpan.startLine, endSpan.startCharacter, endSpan.endLine, endSpan.startCharacter + endOffset));
    }
    return result;
  }

  @Override
  public CharacterParser createCharacterParser() {
    return new JavaCharacterParser(this);
  }

  private static String getString(LiteralTree literal) {
    return literal.asConstant(String.class)
      .orElseThrow(() -> new IllegalArgumentException("Only string literals allowed"));

  }

}
