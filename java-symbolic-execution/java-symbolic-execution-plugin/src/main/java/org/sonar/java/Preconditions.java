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
package org.sonar.java;

public class Preconditions {
  
  private Preconditions() {
  }
  
  public static void checkArgument(boolean expr) {
    if (!expr) {
      throw new IllegalArgumentException();
    }
  }

  public static void checkArgument(boolean expr, String message) {
    if (!expr) {
      throw new IllegalArgumentException(message);
    }
  }
  
  public static void checkState(boolean expr) {
    if (!expr) {
      throw new IllegalStateException();
    }
  }

  public static void checkState(boolean expr, String message) {
    if (!expr) {
      throw new IllegalStateException(message);
    }
  }
  
  public static void checkState(boolean expr, String message, Object argument) {
    if (!expr) {
      throw new IllegalStateException(String.format(message, argument));
    }
  }
  
  public static void checkState(boolean expr, String message, Object arg1, Object arg2) {
    if (!expr) {
      throw new IllegalStateException(String.format(message, arg1, arg2));
    }
  }
}
