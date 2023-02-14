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
