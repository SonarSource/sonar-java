/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.utils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

  /**
   * Build String[] by concatenating arguments of types:
   * <ol>
   *   <li>java.lang.String</li>
   *   <li>java.lang.String[]</li>
   *   <li>java.util.Collection<java.lang.strings></li>
   * </ol> 
   * Nested collections and arrays are not supported, and will throw an ArrayStoreException if encountered.
   * @throws IllegalArgumentException If one of the argument is not of the supported types.
   * @throws ArrayStoreException If a collection passed as argument contains an element that is not a String.
   */
  public static String[] flatten(Object ... args) {
    List<String> result = new ArrayList<>();
    for (Object arg : args) {
      if (arg instanceof String s) {
        result.add(s);
      } else if (arg instanceof String[] arr) {
        Collections.addAll(result, arr);
      } else if (arg instanceof Collection<?> col) {
        result.addAll((Collection<String>) col);
      } else {
        throw new IllegalArgumentException("Unsupported argument type: " + arg.getClass());
      }
    }
    return result.toArray(new String[0]);
  }
}
