/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreconditionsTest {

  @Test
  void checkArgument() {
    assertThrows(IllegalArgumentException.class, () -> Preconditions.checkArgument(false));
    assertDoesNotThrow(() -> Preconditions.checkArgument(true));
  }

  @Test
  void checkArgumentWithMessage() {
    assertThrows(IllegalArgumentException.class, () -> Preconditions.checkArgument(false, "message"), "message");
    assertDoesNotThrow(() -> Preconditions.checkArgument(true));
  }

  @Test
  void checkState() {
    assertThrows(IllegalStateException.class, () -> Preconditions.checkState(false));
    assertDoesNotThrow(() -> Preconditions.checkState(true));
  }

  @Test
  void checkStateWithMessage() {
    assertThrows(IllegalStateException.class, () -> Preconditions.checkState(false, "message"), "message");
    assertDoesNotThrow(() -> Preconditions.checkArgument(true));
  }

  @Test
  void checkStateWithMessageArg() {
    String msg = "message %s";
    String arg = "arg";
    String arg2 = "arg2";
    assertThrows(IllegalStateException.class, () -> Preconditions.checkState(false, msg, arg), "message arg");
    assertThrows(IllegalStateException.class, () -> Preconditions.checkState(false, msg, arg, arg2), "message arg arg2");
    assertDoesNotThrow(() -> Preconditions.checkArgument(true));
  }

}
