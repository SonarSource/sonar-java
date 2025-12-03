/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.plugins.java.api.location.Position;

/**
 * There is a different convention in the JDT for line and column numbers.
 * The difference is only when there are some line continuation characters in text blocks.
 * Eclipse does not count the line continuation as a new line.
 * So we cannot use the following methods to get the line and column number:
 * {@link org.eclipse.jdt.core.dom.CompilationUnit#getColumnNumber(int position)}
 * {@link org.eclipse.jdt.core.dom.CompilationUnit#getLineNumber(int position)}
 */
public class LineColumnConverter {

  private static final Pattern LINE_SEPARATOR_PATTERN = Pattern.compile("\r\n?|\n");

  private int[] lineStartIndexes = new int[64];
  private int lineStartIndexesLength = 0;

  public LineColumnConverter(String source) {
    Matcher matcher = LINE_SEPARATOR_PATTERN.matcher(source);
    addLineStartIndex(0);
    while (matcher.find()) {
      addLineStartIndex(matcher.end());
    }
    addLineStartIndex(Integer.MAX_VALUE);
  }

  private void addLineStartIndex(int index) {
    if (lineStartIndexesLength == lineStartIndexes.length) {
      lineStartIndexes = Arrays.copyOf(lineStartIndexes, lineStartIndexes.length * 2);
    }
    lineStartIndexes[lineStartIndexesLength] = index;
    lineStartIndexesLength++;
  }

  public Pos toPos(int absolutSourcePosition) {
    int searchResult = Arrays.binarySearch(lineStartIndexes, 0, lineStartIndexesLength, absolutSourcePosition);
    if (searchResult < 0) {
      return new Pos(-searchResult - 1, absolutSourcePosition - lineStartIndexes[-searchResult - 2]);
    } else {
      return new Pos(searchResult + 1, 0);
    }
  }

  public Position toPosition(int absolutSourcePosition) {
    return toPos(absolutSourcePosition).toPosition();
  }

  /**
   * Represent the position in a source String. The first character is at {@code line} 1, and {@code columnOffset} 0.
   */
  public record Pos(int line, int columnOffset) {
    /**
     * Convert this object to an equivalent {@link Position}. The {@link Position} for the first character, has line 1 and column 1.
     */
    public Position toPosition() {
      return Position.at(line, columnOffset + 1);
    }
  }

}
