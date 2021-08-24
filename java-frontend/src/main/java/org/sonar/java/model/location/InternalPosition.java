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
package org.sonar.java.model.location;

import javax.annotation.Nullable;
import org.sonar.plugins.java.api.location.Position;

public class InternalPosition implements Position {

  /**
   * The line number in a file. First line is 1.
   */
  private final int line;

  /**
   * The column number at the specified line. First column is 1.
   */
  private final int column;

  public InternalPosition(int line, int column) {
    this.line = line;
    this.column = column;
  }

  @Override
  public int line() {
    return line;
  }

  @Override
  public int lineOffset() {
    return line - 1;
  }

  @Override
  public int column() {
    return column;
  }

  @Override
  public int columnOffset() {
    return column - 1;
  }

  @Override
  public int compareTo(Position o) {
    return (line == o.line()) ? Integer.compare(column, o.column()) : Integer.compare(line, o.line());
  }

  @Override
  public boolean isBefore(Position other) {
    return compareTo(other) < 0;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InternalPosition that = (InternalPosition) o;
    return line == that.line && column == that.column;
  }

  @Override
  public int hashCode() {
    return 31 * line + column;
  }

  @Override
  public String toString() {
    return line + ":" + column;
  }

}
