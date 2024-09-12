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

public class LineColumnConverter {

  private final int[] lineStartIndexes;

  public LineColumnConverter(String source) {
    String[] lines = source.split("(?<=\r\n|\r|\n)", -1);
    lineStartIndexes = new int[lines.length + 1];
    for (int i = 0; i < lines.length; i++) {
      lineStartIndexes[i] = i == 0 ? 0 : (lineStartIndexes[i - 1] + lines[i - 1].length());
    }
    lineStartIndexes[lines.length] = Integer.MAX_VALUE;
  }

  public Pos toPos(int absolutSourcePosition) {
    int searchResult = Arrays.binarySearch(lineStartIndexes, absolutSourcePosition);
    if (searchResult < 0) {
      return new Pos(-searchResult - 1, absolutSourcePosition - lineStartIndexes[-searchResult - 2]);
    } else  {
      return new Pos(searchResult + 1, 0);
    }
  }

  public record Pos(int line, int columnOffset) {
  }
}
