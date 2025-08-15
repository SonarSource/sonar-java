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

public class StringUtils {
  public static boolean isEmpty(String string) {
    return string == null || string.isEmpty();
  }

  public static boolean isNotEmpty(String string) {
    return !isEmpty(string);
  }

  public static String[] split(String string, char separator) {
    return org.apache.commons.lang3.StringUtils.split(string, separator);
  }

  public static String[] split(String string, String separator) {
    return org.apache.commons.lang3.StringUtils.split(string, separator);
  }

  public static int countMatches(String string, char ch) {
    return org.apache.commons.lang3.StringUtils.countMatches(string, ch);
  }

  public static int countMatches(String s1, String s2) {
    return org.apache.commons.lang3.StringUtils.countMatches(s1, s2);
  }

  public static boolean contains(String string, int ch) {
    return org.apache.commons.lang3.StringUtils.contains(string, ch);
  }

  public static String stripAccents(String string) {
    return org.apache.commons.lang3.StringUtils.stripAccents(string);
  }

  public static String capitalize(String string) {
    return org.apache.commons.lang3.StringUtils.capitalize(string);
  }

  public static String substringBefore(String input, String mark) {
    return org.apache.commons.lang3.StringUtils.substringBefore(input, mark);
  }

  public static String substringAfter(String input, String mark) {
    return org.apache.commons.lang3.StringUtils.substringAfter(input, mark);
  }

  public static String substringBetween(String input, String start, String end) {
    return org.apache.commons.lang3.StringUtils.substringBetween(input, start, end);
  }

  public static String defaultIfBlank(String string, String defaultValue) {
    return org.apache.commons.lang3.StringUtils.defaultIfBlank(string, defaultValue);
  }
}
