/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class DefaultTelemetry implements Telemetry {

  private static final AlphaNumericComparator ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator();

  private final Map<TelemetryKey, Set<String>> sets = new EnumMap<>(TelemetryKey.class);
  private final Map<TelemetryKey, Long> counters = new EnumMap<>(TelemetryKey.class);
  private final Map<TelemetryKey, Flag> flags = new EnumMap<>(TelemetryKey.class);

  /**
   * Stores a set of booleans, which can have three possible states:
   * all true, all false, or mixed.
   */
  enum Flag {
    ALL_TRUE,
    ALL_FALSE,
    MIXED;

    static Flag of(boolean value) {
      return value ? ALL_TRUE : ALL_FALSE;
    }

    Flag aggregate(Flag that) {
      return this == that ? this : MIXED;
    }

    @Override
    public String toString() {
      return switch (this) {
        case ALL_TRUE -> "true";
        case ALL_FALSE -> "false";
        case MIXED -> "mixed";
      };
    }
  }

  @Override
  public void aggregateAsSortedSet(TelemetryKey key, String value) {
    sets.computeIfAbsent(key, k -> makeSet()).add(value);
  }

  @Override
  public void aggregateAsSortedSet(TelemetryKey key) {
    sets.computeIfAbsent(key, k -> makeSet());
  }

  private static Set<String> makeSet() {
    return new TreeSet<>(ALPHA_NUMERIC_COMPARATOR);
  }

  @Override
  public void aggregateAsCounter(TelemetryKey key, long value) {
    counters.merge(key, value, Long::sum);
  }

  @Override
  public void aggregateAsFlag(TelemetryKey key, boolean value) {
    Flag f = Flag.of(value);
    flags.merge(key, f, Flag::aggregate);
  }

  @Override
  public Map<String, String> toMap() {
    Map<String, String> map = new TreeMap<>(ALPHA_NUMERIC_COMPARATOR);
    sets.forEach((key, value) -> map.put(key.key(), value.isEmpty() ? "absent" : String.join(",", value)));
    counters.forEach((key, value) -> map.put(key.key(), String.valueOf(value)));
    flags.forEach((key, value) -> map.put(key.key(), String.valueOf(value)));
    return map;
  }
}
