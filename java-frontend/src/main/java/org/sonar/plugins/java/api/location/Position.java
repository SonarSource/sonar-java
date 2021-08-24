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
package org.sonar.plugins.java.api.location;

import org.sonar.java.model.location.InternalPosition;

public interface Position extends Comparable<Position> {

  int FIRST_LINE = 1;
  int FIRST_COLUMN = 1;

  /**
   * The line number in a file. First line number is 1.
   */
  int line();

  /**
   * The line offset in a file. First line offset is 0. (lineOffset() == line() - 1)
   */
  int lineOffset();

  /**
   * The column number at the specified line. First column number is 1. (column() == columnOffset() + 1)
   */
  int column();

  /**
   * The column offset at the specified line. First column offset is 0. (columnOffset() == column() - 1)
   */
  int columnOffset();

  static Position at(int line, int column) {
    return new InternalPosition(line, column);
  }

  static Position atOffset(int line, int columnOffset) {
    return at(line, columnOffset + 1);
  }

  boolean isBefore(Position other);

}
