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
package org.sonar.java.exceptions;

public class ThrowableUtils {
  
  private ThrowableUtils() {
  }

  /* This method uses a standard 2 pointers algorithm to detect whether there is a cycle in a chain of exception causes */
  public static Throwable getRootCause(Throwable throwable) {
    Throwable slowPointer = throwable;

    Throwable cause;
    Throwable t = throwable;
    for(boolean advanceSlowPointer = false; (cause = t.getCause()) != null; advanceSlowPointer = !advanceSlowPointer) {
      t = cause;
      if (cause == slowPointer) {
        throw new IllegalArgumentException("Loop in causal chain detected.", cause);
      }

      if (advanceSlowPointer) {
        slowPointer = slowPointer.getCause();
      }
    }
    return t;
  }
}
