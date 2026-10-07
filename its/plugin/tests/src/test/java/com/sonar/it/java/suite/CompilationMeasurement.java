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
package com.sonar.it.java.suite;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

final class CompilationMeasurement {

  private static final Pattern PHASE = Pattern.compile(
    "Java bytecode compilation: scope=(main|test|generated) status=(SUCCESS|FAILED|SKIPPED) sources=(\\d+) classes=(\\d+) time_ms=(\\d+)");

  private record Phase(String status, long sources, long classes, long millis) {
  }

  private CompilationMeasurement() {
  }

  static Map<String, String> read(String logs, boolean enabled) {
    return read(logs, enabled, 1);
  }

  static Map<String, String> read(String logs, boolean enabled, int moduleLimit) {
    if (moduleLimit < 1) {
      throw new IllegalArgumentException("At least one module is required for compilation measurements");
    }
    var phases = new TreeMap<String, List<Phase>>();
    var matcher = PHASE.matcher(logs);
    while (matcher.find()) {
      var phase = new Phase(matcher.group(2), Long.parseLong(matcher.group(3)), Long.parseLong(matcher.group(4)), Long.parseLong(matcher.group(5)));
      var scope = phases.computeIfAbsent(matcher.group(1), key -> new ArrayList<>());
      if (scope.size() == moduleLimit) {
        throw new IllegalArgumentException("Duplicate bytecode compilation phase: " + matcher.group(1));
      }
      scope.add(phase);
    }
    if (!enabled && !phases.isEmpty()) {
      throw new IllegalArgumentException("Compilation ran while disabled");
    }
    var telemetry = new TreeMap<String, String>();
    if (enabled && phases.isEmpty()) {
      telemetry.put("comparison.compilation.status", "UNAVAILABLE");
      return Map.copyOf(telemetry);
    }
    var all = phases.values().stream().flatMap(List::stream).toList();
    String status = !enabled ? "DISABLED" : status(all);
    telemetry.put("comparison.compilation.status", status);
    telemetry.put("comparison.compilation.time_ms", Long.toString(all.stream().mapToLong(Phase::millis).sum()));
    telemetry.put("comparison.compilation.classes", Long.toString(all.stream().mapToLong(Phase::classes).sum()));
    telemetry.put("comparison.compilation.sources", Long.toString(all.stream().mapToLong(Phase::sources).sum()));
    phases.forEach((scope, records) -> {
      telemetry.put("comparison.compilation." + scope + ".status", status(records));
      telemetry.put("comparison.compilation." + scope + ".classes", Long.toString(records.stream().mapToLong(Phase::classes).sum()));
      telemetry.put("comparison.compilation." + scope + ".modules", Integer.toString(records.size()));
      for (String outcome : List.of("SUCCESS", "FAILED", "SKIPPED")) {
        telemetry.put("comparison.compilation." + scope + "." + outcome.toLowerCase(java.util.Locale.ROOT),
          Long.toString(records.stream().filter(record -> record.status().equals(outcome)).count()));
      }
    });
    return Map.copyOf(telemetry);
  }

  private static String status(List<Phase> phases) {
    if (phases.stream().anyMatch(phase -> phase.status().equals("FAILED"))) {
      return "FAILED";
    }
    return phases.stream().anyMatch(phase -> phase.status().equals("SUCCESS")) ? "SUCCESS" : "SKIPPED";
  }
}
