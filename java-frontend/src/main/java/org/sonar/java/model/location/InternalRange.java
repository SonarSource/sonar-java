/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.model.location;

import java.util.List;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.location.Range;

public class InternalRange implements Range {

  private final Position start;

  private final Position end;

  public InternalRange(Position start, Position end) {
    this.start = start;
    this.end = end;
  }

  public InternalRange(Position start, String text) {
    List<String> lines = LineUtils.splitLines(text);
    String lastLine = lines.get(lines.size() - 1);
    int endLine = start.line() + lines.size() - 1;
    int endColumn = (lines.size() == 1 ? start.column() : Position.FIRST_COLUMN) + lastLine.length();
    this.start = start;
    this.end = Position.at(endLine, endColumn);
  }

  /**
   * @return the inclusive start position of the range. The character at this location is part of the range.
   */
  @Override
  public Position start() {
    return start;
  }

  /**
   * @return the exclusive end position of the range. The character at this location is not part of the range.
   */
  @Override
  public Position end() {
    return end;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InternalRange that = (InternalRange) o;
    return start.equals(that.start) && end.equals(that.end);
  }

  @Override
  public int hashCode() {
    return 31 * start.hashCode() + end.hashCode();
  }

  @Override
  public String toString() {
    return "(" + start + ")-(" + end + ")";
  }

}
