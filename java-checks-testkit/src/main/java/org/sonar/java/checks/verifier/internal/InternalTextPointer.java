/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
