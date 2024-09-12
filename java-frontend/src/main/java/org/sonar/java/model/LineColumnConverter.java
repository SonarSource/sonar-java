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
package org.sonar.java.model;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    } else  {
      return new Pos(searchResult + 1, 0);
    }
  }

  public record Pos(int line, int columnOffset) {
  }

}
