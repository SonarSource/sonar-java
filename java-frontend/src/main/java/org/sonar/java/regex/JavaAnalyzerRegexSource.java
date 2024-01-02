/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.sonar.java.model.LineUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.regex.ast.IndexRange;
import org.sonarsource.analyzer.commons.regex.java.JavaRegexSource;

public class JavaAnalyzerRegexSource extends JavaRegexSource {
  /**
   * Maps an index of the regular expression to the TextSpan string literal that starts at the index
   */
  private final TextSpanTracker indexToTextSpan = new TextSpanTracker();

  public JavaAnalyzerRegexSource(List<LiteralTree> stringLiterals) {
    super(literalsToString(stringLiterals));
    for (LiteralTree literal : stringLiterals) {
      String text = getString(literal);
      indexToTextSpan.addLiteral(literal, text.length());
    }
  }

  private static String literalsToString(List<LiteralTree> stringLiterals) {
    StringBuilder sb = new StringBuilder();
    for (LiteralTree literal : stringLiterals) {
      String text = getString(literal);
      sb.append(text);
    }
    return sb.toString();
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
      if (startSpan.onLine()) {
        result.add(startSpan);
      } else {
        // This assumes that startSpan.startLine == startSpan.endLine, which should always be the case
        TextSpan newTextSpan = new TextSpan(startSpan.startLine, startSpan.startCharacter + startOffset, startSpan.endLine, startSpan.startCharacter + endOffset);
        if (shouldUseLine(startSpan, newTextSpan)) {
          result.add(new TextSpan(newTextSpan.startLine));
        } else {
          result.add(newTextSpan);
        }
      }
    } else {
      result.add(new TextSpan(startSpan.startLine, startSpan.startCharacter + startOffset, startSpan.endLine, startSpan.endCharacter));
      int indexAfterStartSpan = startEntry.startIndex + startSpan.endCharacter - startSpan.startCharacter;
      result.addAll(indexToTextSpan.textSpansBetween(indexAfterStartSpan, endEntry.startIndex));
      result.add(new TextSpan(endSpan.startLine, endSpan.startCharacter, endSpan.endLine, endSpan.startCharacter + endOffset));
    }
    return result;
  }

  boolean shouldUseLine(TextSpan textSpan, TextSpan newTextSpan) {
    // is exceeding limits of existing text span
    if (newTextSpan.endCharacter <= textSpan.endCharacter) {
      return false;
    }
    LiteralTree literal = indexToTextSpan.getLiteral(textSpan);
    // is not a text block
    if (literal == null || !literal.is(Tree.Kind.TEXT_BLOCK)) {
      return false;
    }
    int lastLine = LineUtils.startLine(literal.token())
      + literal.value().split("\n").length
      - 1;
    // last line will benefit from the closing """
    return textSpan.endLine != lastLine;
  }

  private static String getString(LiteralTree literal) {
    return literal.asConstant(String.class)
      .orElseThrow(() -> new IllegalArgumentException("Only string literals allowed"));

  }

  private static class TextSpanTracker {
    final NavigableMap<Integer, TextSpan> indexToTextSpan = new TreeMap<>();
    final Map<TextSpan, LiteralTree> textSpanToLiteral = new HashMap<>();
    int index = 0;

    void addLiteral(LiteralTree literal, int length) {
      if (literal.is(Tree.Kind.TEXT_BLOCK)) {
        addTextBlock(literal);
      } else {
        addStringLiteral(literal, length);
      }
    }

    void addStringLiteral(LiteralTree literal, int length) {
      TextSpan literalSpan = AnalyzerMessage.textSpanFor(literal);
      // Create a text span for the string with the quotes stripped out
      TextSpan textSpan = new TextSpan(literalSpan.startLine, literalSpan.startCharacter + 1, literalSpan.endLine, literalSpan.endCharacter - 1);
      indexToTextSpan.put(index, textSpan);
      textSpanToLiteral.put(textSpan, literal);
      index += length;
    }

    void addTextBlock(LiteralTree literal) {
      String[] literalTreeLines = literal.value().split("\n");
      String[] stringLines = getString(literal).split("(?<=\r?\n)");

      int indent = LiteralUtils.indentationOfTextBlock(literalTreeLines);
      int textBlockLine = LineUtils.startLine(literal.token());
      for (int i = 0; i < stringLines.length; i++) {
        int line = textBlockLine + i + 1;
        String stringLine = stringLines[i];
        int lineLength = stringLine.length();
        TextSpan textSpan;
        if (stringLine.trim().isEmpty()) {
          textSpan = new TextSpan(line);
        } else {
          int endLineTrimming = stringLine.endsWith("\n") ? 1 : 0;
          textSpan = new TextSpan(line, indent, line, indent + lineLength - endLineTrimming);
        }
        indexToTextSpan.put(index, textSpan);
        textSpanToLiteral.put(textSpan, literal);
        index += lineLength;
      }
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
    LiteralTree getLiteral(TextSpan textSpan) {
      return textSpanToLiteral.get(textSpan);
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
