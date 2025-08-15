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
package org.sonar.java.common;


import java.text.Normalizer;
import java.util.regex.Pattern;

import static java.io.File.separator;

public class StringUtils {
  public static boolean isEmpty(String string) {
    return string == null || string.isEmpty();
  }

  public static boolean isNotEmpty(String string) {
    return !isEmpty(string);
  }

  public static String[] split(String string, char separator) {
    if (string == null || string.isEmpty()) {
      return new String[0];
    }
    return string.split("" + separator);
//    return org.apache.commons.lang3.StringUtils.split(string, separator);
  }

  public static String[] split(String string, String separator) {
    if (string == null || string.isEmpty()) {
      return new String[0];
    }
    return string.split(separator);
//    return org.apache.commons.lang3.StringUtils.split(string, separator);
  }

  public static int countMatches(String string, char ch) {
    if (string == null || string.isEmpty()) {
      return 0;
    }
    int count = 0;
    for (int i = 0; i < string.length(); i++) {
      if (ch == string.charAt(i)) {
        count++;
      }
    }
    return count;
//    return org.apache.commons.lang3.StringUtils.countMatches(string, ch);
  }

  public static int countMatches(String s1, String s2) {
    if (s1 == null || s1.isEmpty() || s2 == null || s2.isEmpty()) {
      return 0;
    }
    int count = 0;
    int idx = 0;
    while ((idx = s1.indexOf(s2, idx)) != -1) {
      count++;
      idx += s2.length();
    }
    return count;
//    return org.apache.commons.lang3.StringUtils.countMatches(s1, s2);
  }

//  public static boolean contains(String string, int ch) {
//    return org.apache.commons.lang3.StringUtils.contains(string, ch);
//  }

  public static String stripAccents(String string) {
//    return org.apache.commons.lang3.StringUtils.stripAccents(string);

    if (string == null) {
      return null;
    }

    // Normalize the string to a decomposed form (e.g., 'é' becomes 'e' followed by an accent mark).
    String normalizedString = Normalizer.normalize(string, Normalizer.Form.NFD);

    // Define a regular expression pattern to match Unicode characters that are combining diacritical marks.
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    // Use the pattern to replace all diacritical marks with an empty string.
    return pattern.matcher(normalizedString).replaceAll("");
  }

  public static String capitalize(String string) {
    if (string == null || string.isEmpty()) {
      return string;
    }
    return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
//    return org.apache.commons.lang3.StringUtils.capitalize(string);
  }

  public static String substringBefore(String input, String mark) {
    if (input == null || input.isEmpty() || separator == null) {
      return input;
    }
    if (mark.isEmpty()) {
      return "";
    }
    final int pos = input.indexOf(mark);
    if (pos == -1) {
      return input;
    }
    return input.substring(0, pos);
//    return org.apache.commons.lang3.StringUtils.substringBefore(input, mark);
  }

  public static String substringAfter(String input, String mark) {
    if (isEmpty(input)) {
      return input;
    }
    if (mark == null) {
      return "";
    }
    final int pos = input.indexOf(mark);
    if (pos == -1) {
      return "";
    }
    return input.substring(pos + mark.length());
//    return org.apache.commons.lang3.StringUtils.substringAfter(input, mark);
  }

  public static String substringBetween(String input, String start, String end) {
//    return org.apache.commons.lang3.StringUtils.substringBetween(input, start, end);
    if (input == null || start == null || end == null) {
      return null;
    }
    var startIdx = input.indexOf(start);
    if (startIdx != -1) {
      var endIdx = input.indexOf(end, startIdx + start.length());
      if (endIdx != -1) {
        return input.substring(startIdx + start.length(), endIdx);
      }
    }
    return null;
  }

//  public static String defaultIfBlank(String string, String defaultValue) {
//    return org.apache.commons.lang3.StringUtils.defaultIfBlank(string, defaultValue);
//  }
}
