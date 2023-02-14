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

import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

final class InternalTextRange extends InternalMockedSonarAPI implements TextRange {
  private final TextPointer start;
  private final TextPointer end;

  InternalTextRange(int startLine, int startColumn, int endLine, int endColumn) {
    this(new InternalTextPointer(startLine, startColumn), new InternalTextPointer(endLine, endColumn));
  }

  InternalTextRange(TextPointer start, TextPointer end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public TextPointer end() {
    return end;
  }

  @Override
  public TextPointer start() {
    return start;
  }

  @Override
  public boolean overlap(TextRange arg0) {
    throw notSupportedException("overlap(TextRange)");
  }
}
