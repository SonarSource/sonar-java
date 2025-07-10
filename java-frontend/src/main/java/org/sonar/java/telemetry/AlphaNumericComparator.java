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
package org.sonar.java.telemetry;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Comparator for alphanumeric strings that compares numbers as integers and other parts as strings.
 * For example: "elem1,elem2,elem12" instead of "elem1,elem12,elem2".
 */
class AlphaNumericComparator implements Comparator<String> {

  // Splits a string into parts where each part is either a sequence of digits or a sequence of non-digits.
  private static final Pattern DIGITS_SEPARATOR = Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

  @Override
  public int compare(@Nullable String o1, @Nullable String o2) {
    if (o1 == null || o2 == null) {
      if (o1 != null) {
        return 1;
      }
      return o2 != null ? -1 : 0;
    }
    if (o1.equals(o2)) {
      return 0;
    }
    String[] arr1 = DIGITS_SEPARATOR.split(o1, -1);
    String[] arr2 = DIGITS_SEPARATOR.split(o2, -1);
    int len = Math.min(arr1.length, arr2.length);
    for (int i = 0; i < len; i++) {
      int comp = compareStringOrNumber(arr1[i], arr2[i]);
      if (comp != 0) {
        return comp;
      }
    }
    return Integer.compare(arr1.length, arr2.length);
  }

  private static int compareStringOrNumber(String str1, String str2) {
    if (startWithDigit(str1) && startWithDigit(str2)) {
      return new BigInteger(str1).compareTo(new BigInteger(str2));
    } else {
      return str1.compareTo(str2);
    }
  }

  private static boolean startWithDigit(String str) {
    return !str.isEmpty() && str.charAt(0) >= '0' && str.charAt(0) <= '9';
  }

}
