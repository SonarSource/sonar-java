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
package org.sonar.java.checks.helpers;

import javax.annotation.Nullable;

public class StringUtils {
  private StringUtils() {}

  /** Check if the string is null or empty. */
  public static boolean isEmpty(@Nullable String s) {
    return s == null || s.isEmpty();
  }

  /** Count non-overlapping occurrences of <code>pattern</code> in the <code>string</code>. */
  public static int countMatches(@Nullable String string, @Nullable String pattern) {
    if (isEmpty(string) || isEmpty(pattern)) {
      return 0;
    }

    int count = 0;
    int idx = 0;
    while ((idx = string.indexOf(pattern, idx)) != -1) {
      count++;
      idx += pattern.length();
    }

    return count;
  }
}
