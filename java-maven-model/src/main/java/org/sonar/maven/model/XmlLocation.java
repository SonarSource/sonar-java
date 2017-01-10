/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.maven.model;

import com.google.common.base.Preconditions;

public class XmlLocation {
  private final int line;
  private final int column;
  private final int offset;

  public XmlLocation(int line, int column, int offset) {
    Preconditions.checkArgument(line >= 0);
    Preconditions.checkArgument(column >= 0);
    Preconditions.checkArgument(offset >= 0);
    this.line = line;
    this.column = column;
    this.offset = offset;
  }

  public XmlLocation(int line, int offset) {
    Preconditions.checkArgument(line >= 0);
    Preconditions.checkArgument(offset >= 0);
    this.line = line;
    this.column = -1;
    this.offset = offset;
  }

  public int line() {
    return line;
  }

  /**
   * @return -1 if unknown
   */
  public int column() {
    return column;
  }

  public int offset() {
    return offset;
  }

  @Override
  public String toString() {
    String columnAsText = Integer.toString(column);
    if (column == -1) {
      columnAsText = "?";
    }
    return "(" + line + "," + columnAsText + ")[" + offset + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof XmlLocation) {
      XmlLocation loc = (XmlLocation) obj;
      return line == loc.line && column == loc.column;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return offset;
  }

  public static XmlLocation getLocation(javax.xml.stream.Location loc) {
    return new XmlLocation(loc.getLineNumber(), loc.getColumnNumber(), loc.getCharacterOffset());
  }

  public static XmlLocation getStartLocation(String text, javax.xml.stream.Location endLocation) {
    int length = text.length();
    int offset = endLocation.getCharacterOffset() - length;
    int numberLines = (text.split("\\r?\\n")).length - 1;
    if (numberLines > 0) {
      return new XmlLocation(endLocation.getLineNumber() - numberLines, offset);
    }
    return new XmlLocation(endLocation.getLineNumber(), endLocation.getColumnNumber() - length, offset);
  }
}
