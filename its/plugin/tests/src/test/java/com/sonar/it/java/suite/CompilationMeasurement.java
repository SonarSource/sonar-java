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
    var phases = new TreeMap<String, Phase>();
    var matcher = PHASE.matcher(logs);
    while (matcher.find()) {
      var phase = new Phase(matcher.group(2), Long.parseLong(matcher.group(3)), Long.parseLong(matcher.group(4)), Long.parseLong(matcher.group(5)));
      if (phases.putIfAbsent(matcher.group(1), phase) != null) {
        throw new IllegalArgumentException("Duplicate bytecode compilation phase: " + matcher.group(1));
      }
    }
    if (!enabled && !phases.isEmpty()) {
      throw new IllegalArgumentException("Compilation ran while disabled");
    }
    var telemetry = new TreeMap<String, String>();
    if (enabled && phases.isEmpty()) {
      telemetry.put("comparison.compilation.status", "UNAVAILABLE");
      return Map.copyOf(telemetry);
    }
    String status = !enabled ? "DISABLED" : phases.values().stream().anyMatch(phase -> phase.status().equals("FAILED")) ? "FAILED"
      : phases.values().stream().anyMatch(phase -> phase.status().equals("SUCCESS")) ? "SUCCESS" : "SKIPPED";
    telemetry.put("comparison.compilation.status", status);
    telemetry.put("comparison.compilation.time_ms", Long.toString(phases.values().stream().mapToLong(Phase::millis).sum()));
    telemetry.put("comparison.compilation.classes", Long.toString(phases.values().stream().mapToLong(Phase::classes).sum()));
    telemetry.put("comparison.compilation.sources", Long.toString(phases.values().stream().mapToLong(Phase::sources).sum()));
    phases.forEach((scope, phase) -> {
      telemetry.put("comparison.compilation." + scope + ".status", phase.status());
      telemetry.put("comparison.compilation." + scope + ".classes", Long.toString(phase.classes()));
    });
    return Map.copyOf(telemetry);
  }
}
