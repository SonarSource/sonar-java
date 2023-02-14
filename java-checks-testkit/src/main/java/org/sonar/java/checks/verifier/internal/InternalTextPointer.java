/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.verifier.internal;

import java.util.Comparator;
import org.sonar.api.batch.fs.TextPointer;

import static java.util.Comparator.comparingInt;

final class InternalTextPointer implements TextPointer {
  private static final Comparator<TextPointer> COMPARATOR = comparingInt(TextPointer::line).thenComparing(TextPointer::lineOffset);
  private final int line;
  private final int offset;

  InternalTextPointer(int line, int offset) {
    this.line = line;
    this.offset = offset;
  }

  @Override
  public int line() {
    return line;
  }

  @Override
  public int lineOffset() {
    return offset;
  }

  @Override
  public int compareTo(TextPointer o) {
    return COMPARATOR.compare(this, o);
  }
}
