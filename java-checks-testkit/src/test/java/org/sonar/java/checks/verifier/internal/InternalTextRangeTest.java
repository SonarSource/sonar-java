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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.sonar.api.batch.fs.TextRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalTextRangeTest {

  @Test
  void methods() {
    TextRange range = new InternalTextRange(new InternalTextPointer(42, 666), new InternalTextPointer(666, 42));

    assertThat(range.start()).isNotNull();
    assertThat(range.end()).isNotNull();
    // should have overlapped - not implemented
    assertMethodNotSupported(() -> range.overlap(new InternalTextRange(46, 14, 48, 32)), "InternalTextRange::overlap(TextRange)");
  }

  private static void assertMethodNotSupported(Executable executable, String expectedMessage) {
    InternalMockedSonarAPI.NotSupportedException e = assertThrows(InternalMockedSonarAPI.NotSupportedException.class, executable);
    assertThat(e).hasMessage(String.format("Method unsupported by the rule verifier framework: '%s'", expectedMessage));
  }
}
