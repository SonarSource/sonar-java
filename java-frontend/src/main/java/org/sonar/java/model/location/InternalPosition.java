/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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

  public static Position atOffset(int line, int columnOffset) {
    return new InternalPosition(line, columnOffset + 1);
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
  public boolean isAfter(Position other) {
    return compareTo(other) > 0;
  }

/**
 * Returns a new {@code Position} object that represents this position's coordinates
 * when counted relative to the given starting position.
 */
  @Override
  public Position relativeTo(Position startPosition) {
    if (line == 1) {
      // If we are on the first line we need to account the column startPosition. Otherwise, it doesn't matter.
      return new InternalPosition(line + startPosition.lineOffset(), column + startPosition.columnOffset());
    }
    return new InternalPosition(line + startPosition.lineOffset(), column);
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
