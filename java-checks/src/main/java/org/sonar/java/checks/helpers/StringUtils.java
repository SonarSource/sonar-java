package org.sonar.java.checks.helpers;

import javax.annotation.Nullable;

public class StringUtils {
  private StringUtils() {}

  public static boolean isEmpty(@Nullable String s) {
    return s == null || s.isEmpty();
  }

  /**
   * Calls {@link org.apache.commons.lang3.StringUtils#split(String, char)}.
   *
   * <p>This method exists so that we can do not have a name clash between two <code>StringUtils</code> classes.
   */
  public static String[] split(String string, char separator) {
    return org.apache.commons.lang3.StringUtils.split(string, separator);
  }

  /**
   * Calls {@link org.apache.commons.lang3.StringUtils#split(String, String)}.
   *
   * <p>This method exists so that we can do not have a name clash between two <code>StringUtils</code> classes.
   */
  public static String[] split(String string, String separator) {
    return org.apache.commons.lang3.StringUtils.split(string, separator);
  }
}
