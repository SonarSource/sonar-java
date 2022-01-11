/*
 * Copyright (C) 2012-2022 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.utils;

public final class StringUtils {

  private StringUtils() {
    // Utility class
  }

  public static String spaces(int number) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < number; i++) {
      sb.append(' ');
    }
    return sb.toString();
  }

  public static boolean isNotEmpty(String string) {
    return string != null && !string.isEmpty();
  }

}
