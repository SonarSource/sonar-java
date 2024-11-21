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
package org.sonar.java.se;

import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.Nullable;

public class ExceptionUtils {

  private ExceptionUtils() {
    // Utility class
  }

  public static boolean isUncheckedException(@Nullable Type exceptionType) {
    if (exceptionType == null) {
      return false;
    }
    return exceptionType.isSubtypeOf("java.lang.RuntimeException")
        || exceptionType.isSubtypeOf("java.lang.Error")
        || exceptionType.is("java.lang.Exception")
        || exceptionType.is("java.lang.Throwable");
  }



}
