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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class TelemetryStorage implements Telemetry {

  public static final AlphaNumericComparator ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator();
  private final Map<String, Set<String>> sets = new HashMap<>();
  private final Map<String, Long> counters = new HashMap<>();

  @Override
  public void aggregateAsSortedSet(TelemetryKey key, String value) {
    sets.computeIfAbsent(key.key(), k -> new TreeSet<>(ALPHA_NUMERIC_COMPARATOR)).add(value);
  }

  @Override
  public void aggregateAsCounter(TelemetryKey key, long value) {
    counters.compute(key.key(), (k, counter) -> counter == null ? value : (counter + value));
  }

  @Override
  public Map<String, String> toMap() {
    Map<String, String> map = new TreeMap<>(ALPHA_NUMERIC_COMPARATOR);
    sets.forEach((key, value) -> map.put(key, String.join(",", value)));
    counters.forEach((key, value) -> map.put(key, String.valueOf(value)));
    return map;
  }

  public static class AlphaNumericComparator implements Comparator<String> {

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
      return !str.isEmpty() &&  str.charAt(0) >= '0' && str.charAt(0) <= '9';
    }

  }

}
