/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.regex.ast;

import java.util.Objects;

public class IndexRange {

  private final int beginningOffset;
  private final int endingOffset;

  public IndexRange(int beginningOffset, int endingOffset) {
    this.beginningOffset = beginningOffset;
    this.endingOffset = endingOffset;
  }

  public int getBeginningOffset() {
    return beginningOffset;
  }

  public int getEndingOffset() {
    return endingOffset;
  }

  public IndexRange merge(IndexRange other) {
    return extendTo(other.endingOffset);
  }

  public IndexRange extendTo(int newEnd) {
    return new IndexRange(beginningOffset, newEnd);
  }

  public boolean isEmpty() {
    return beginningOffset == endingOffset;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof IndexRange
      && beginningOffset == ((IndexRange) other).beginningOffset
      && endingOffset == ((IndexRange) other).endingOffset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(beginningOffset, endingOffset);
  }

  @Override
  public String toString() {
    return beginningOffset + "-" + endingOffset;
  }

}
