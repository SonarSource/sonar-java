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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.AnalyzerMessage.TextSpan;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.plugins.java.api.tree.LiteralTree;

public class JavaRegexSource implements RegexSource {

  private final String sourceText;

  /**
   * Maps an index of the regular expression to the TextSpan string literal that starts at the index
   */
  private final TextSpanTracker indexToTextSpan = new TextSpanTracker();

  public JavaRegexSource(List<LiteralTree> stringLiterals) {
    StringBuilder sb = new StringBuilder();
    for (LiteralTree literal : stringLiterals) {
      String text = getString(literal);
      sb.append(text);
      indexToTextSpan.addLiteral(literal, text.length());
    }
    sourceText = sb.toString();
  }

  @Override
  public String getSourceText() {
    return sourceText;
  }

  public List<TextSpan> textSpansFor(IndexRange range) {
    List<TextSpan> result = new ArrayList<>();
    TextSpanEntry startEntry = indexToTextSpan.entryAtIndex(range.getBeginningOffset());
    if (range.getBeginningOffset() < 0) {
      startEntry = indexToTextSpan.entryAtIndex(0);
    }
    int startOffset = range.getBeginningOffset() - startEntry.startIndex;
    TextSpanEntry endEntry = indexToTextSpan.entryBeforeIndex(range.getEndingOffset());
    if (range.getEndingOffset() <= 0) {
      endEntry = startEntry;
    }
    int endOffset = range.getEndingOffset() - endEntry.startIndex;
    TextSpan startSpan = startEntry.textSpan;
    TextSpan endSpan = endEntry.textSpan;
    if (startSpan == endSpan) {
      // This assumes that startSpan.startLine == startSpan.endLine, which should always be the case
      result.add(new TextSpan(startSpan.startLine, startSpan.startCharacter + startOffset, startSpan.endLine, startSpan.startCharacter + endOffset));
    } else {
      result.add(new TextSpan(startSpan.startLine, startSpan.startCharacter + startOffset, startSpan.endLine, startSpan.endCharacter));
      int indexAfterStartSpan = startEntry.startIndex + startSpan.endCharacter - startSpan.startCharacter;
      result.addAll(indexToTextSpan.textSpansBetween(indexAfterStartSpan, endEntry.startIndex));
      result.add(new TextSpan(endSpan.startLine, endSpan.startCharacter, endSpan.endLine, endSpan.startCharacter + endOffset));
    }
    return result;
  }

  @Override
  public CharacterParser createCharacterParser() {
    return new JavaCharacterParser(this);
  }

  @Override
  public RegexDialect dialect() {
    return RegexDialect.JAVA;
  }

  private static String getString(LiteralTree literal) {
    return literal.asConstant(String.class)
      .orElseThrow(() -> new IllegalArgumentException("Only string literals allowed"));

  }

  private static class TextSpanTracker {
    final NavigableMap<Integer, TextSpan> indexToTextSpan = new TreeMap<>();
    int index = 0;

    void addLiteral(LiteralTree literal, int length) {
      TextSpan literalSpan = AnalyzerMessage.textSpanFor(literal);
      // Create a text span for the string with the quotes stripped out
      indexToTextSpan.put(index, new TextSpan(literalSpan.startLine, literalSpan.startCharacter + 1, literalSpan.endLine, literalSpan.endCharacter - 1));
      index += length;
    }

    @Nullable
    TextSpanEntry entryAtIndex(Integer index) {
      return entry(indexToTextSpan.floorEntry(index));
    }

    @Nullable
    TextSpanEntry entryBeforeIndex(Integer index) {
      return entry(indexToTextSpan.lowerEntry(index));
    }

    @Nullable
    TextSpanEntry entry(@Nullable Map.Entry<Integer, TextSpan> e) {
      if (e == null) {
        return null;
      }
      return new TextSpanEntry(e);
    }

    Collection<TextSpan> textSpansBetween(Integer startIndex, Integer endIndex) {
      return indexToTextSpan.subMap(startIndex, endIndex).values();
    }

  }

  private static class TextSpanEntry {
    final int startIndex;
    final TextSpan textSpan;

    TextSpanEntry(Map.Entry<Integer, TextSpan> entry) {
      startIndex = entry.getKey();
      textSpan = entry.getValue();
    }
  }

}
