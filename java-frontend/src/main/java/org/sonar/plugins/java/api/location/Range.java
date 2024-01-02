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
package org.sonar.plugins.java.api.location;

import org.sonar.java.model.location.InternalRange;

public interface Range {

  /**
   * @return the inclusive start position of the range. The character at this location is part of the range.
   */
  Position start();

  /**
   * @return the exclusive end position of the range. The character at this location is not part of the range.
   */
  Position end();

  /**
   * @param start is inclusive.
   * @param end is exclusive, the character at the "end" location is not part of the range.
   */
  static Range at(Position start, Position end) {
    return new InternalRange(start, end);
  }

  /**
   * @param startLine, starting at 1, the line number of the first character of the range.
   * @param startColumn starting at 1, the character at this location is part of the range.
   * @param endLine, starting at 1, exclusive, the line number of the last character, the last character is not part of the range.
   * @param endColumn starting at 1, exclusive, the column number of the last character, the last character is not part of the range.
   */
  static Range at(int startLine, int startColumn, int endLine, int endColumn) {
    return new InternalRange(Position.at(startLine,startColumn), Position.at(endLine, endColumn));
  }

  /**
   * @param start is inclusive.
   * @param length is used to compute the end of the range
   */
  static Range at(Position start, int length) {
    return new InternalRange(start, Position.at(start.line(), start.column() + length));
  }

  /**
   * @param start is inclusive.
   * @param text to split into lines to compute the end of the range
   */
  static Range at(Position start, String text) {
    return new InternalRange(start, text);
  }

}
