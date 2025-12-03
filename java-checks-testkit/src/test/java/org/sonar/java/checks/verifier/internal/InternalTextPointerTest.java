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

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.TextPointer;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTextPointerTest {

  @Test
  void methods() {
    TextPointer pointer = new InternalTextPointer(42, 666);

    assertThat(pointer.line()).isEqualTo(42);
    assertThat(pointer.lineOffset()).isEqualTo(666);
    assertThat(pointer)
      .isLessThan(new InternalTextPointer(43, 1))
      .isGreaterThan(new InternalTextPointer(42, 665));
  }

}
