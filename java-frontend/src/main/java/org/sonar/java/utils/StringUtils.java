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
import java.util.Locale;
import java.util.regex.Pattern;

public class StringUtils {

  private static final Pattern CAMEL_CASE_BOUNDARY = Pattern.compile("(?=[A-Z])");
  private static final Pattern LETTER_DIGIT_BOUNDARY = Pattern.compile("(?<=[A-Za-z])(?=\\d)|(?<=\\d)(?=[A-Za-z])");

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

  /**
   * Split an identifier into lowercase words, handling camelCase, snake_case, all-uppercase
   * acronyms, and digit/letter boundaries. Each part between underscores is kept whole (so
   * digit-bearing words like pbkdf2 or poly1305 still match verbatim) and, when it mixes
   * letters and digits, also split into its letter-only segments (so version-suffixed
   * acronyms like AES256 also yield the bare word aes).
   */
  public static List<String> tokenizeIdentifier(String identifier) {
    List<String> words = new ArrayList<>();
    for (String part : identifier.split("_")) {
      if (part.isEmpty()) {
        continue;
      }
      if (isAllUppercaseWithLetter(part)) {
        addLetterBearingSegments(part, words);
      } else {
        for (String sub : CAMEL_CASE_BOUNDARY.split(part)) {
          if (!sub.isEmpty()) {
            addLetterBearingSegments(sub, words);
          }
        }
      }
    }
    return words;
  }

  private static void addLetterBearingSegments(String part, List<String> words) {
    words.add(part.toLowerCase(Locale.ROOT));
    String[] segments = LETTER_DIGIT_BOUNDARY.split(part);
    if (segments.length > 1) {
      for (String segment : segments) {
        if (containsLetter(segment)) {
          words.add(segment.toLowerCase(Locale.ROOT));
        }
      }
    }
  }

  private static boolean containsLetter(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLetter(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAllUppercaseWithLetter(String part) {
    boolean hasLetter = false;
    for (int i = 0; i < part.length(); i++) {
      char c = part.charAt(i);
      if (Character.isLetter(c)) {
        hasLetter = true;
        if (Character.isLowerCase(c)) {
          return false;
        }
      }
    }
    return hasLetter;
  }
}
