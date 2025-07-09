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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TelemetryStorage implements Telemetry {

  public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
  private Map<String, Set<String>> sets = new HashMap<>();
  private Map<String, Long> counters = new HashMap<>();

  @Override
  public void aggregateAsSortedSet(TelemetryKey key, String value) {
    sets.computeIfAbsent(key.key(), k -> new TreeSet<>()).add(value);
  }

  @Override
  public void aggregateAsCounter(TelemetryKey key, long value) {
    counters.compute(key.key(), (k, counter) -> counter == null ? value : (counter + value));
  }

  @Override
  public Map<String, String> toMap() {
    Map<String, String> map = new TreeMap<>();
    sets.forEach((key, value) -> map.put(key, String.join(",", value)));
    counters.forEach((key, value) -> map.put(key, String.valueOf(value)));
    return map;
  }

  /**
   * Used for testing purposes
   */
  @Override
  public String toString() {
    return GSON.toJson(toMap());
  }

}
