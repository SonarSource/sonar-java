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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.sonar.java.telemetry.TelemetryKey.JavaAnalysisKeys;
import org.sonar.java.telemetry.TelemetryKey.SpeedKeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.telemetry.TelemetryKey.JAVA_ANALYSIS_GENERATED;
import static org.sonar.java.telemetry.TelemetryKey.JAVA_ANALYSIS_MAIN;
import static org.sonar.java.telemetry.TelemetryKey.JAVA_ANALYSIS_TEST;

class TelemetryKeyTest {

  @Test
  void test_TelemetryKey_names() {
    for (TelemetryKey enumEntry : TelemetryKey.values()) {
      assertThat(enumEntry.name()).isEqualTo(enumNameFromTelemetryName(enumEntry.key()));
    }
  }

  @Test
  void testCompoundKeys() {
    for (String artifact : List.of("main.", "test.", "generated.")) {
      for (String result : List.of("success.", "parse_errors.", "exceptions.")) {
        for (String metric : List.of("size_chars", "time_ms")) {
          assertTelemetryNameExists("java.analysis." + artifact + result + metric);
        }
      }
      assertTelemetryNameExists("java.analysis." + artifact + "success.type_error_count");
    }
  }

  private static String enumNameFromTelemetryName(String telemetryName) {
    return telemetryName.toUpperCase(Locale.ROOT).replaceAll("[.\\-]", "_");
  }

  private static void assertTelemetryNameExists(String telemetryName) {
    TelemetryKey enumEntry = TelemetryKey.valueOf(enumNameFromTelemetryName(telemetryName));
    assertThat(enumEntry.key()).isEqualTo(telemetryName);
  }

  @Test
  void testAnalysisKeys() {
    Map<String, JavaAnalysisKeys> artifacts = Map.of(
      "java.analysis.main.", JAVA_ANALYSIS_MAIN,
      "java.analysis.test.", JAVA_ANALYSIS_TEST,
      "java.analysis.generated.", JAVA_ANALYSIS_GENERATED
    );
    Map<String, Function<JavaAnalysisKeys, SpeedKeys>> results = Map.of(
      "success.", JavaAnalysisKeys::success,
      "parse_errors.", JavaAnalysisKeys::parseErrors,
      "exceptions.", JavaAnalysisKeys::exceptions
    );
    Map<String, Function<SpeedKeys, TelemetryKey>> metrics = Map.of(
      "size_chars", SpeedKeys::sizeCharsKey,
      "time_ms", SpeedKeys::timeMsKey
    );
    artifacts.forEach((artifactName, artifact) -> {
      String typeErrorName = artifactName + "success.type_error_count";
      assertThat(artifact.success().typeErrorCountKey().key()).isEqualTo(typeErrorName);
      results.forEach(
        (resultName, result) -> metrics.forEach((
          metricName, metric) -> {
          String telemetryName = artifactName + resultName + metricName;
          TelemetryKey tk = metric.apply(result.apply(artifact));
          assertThat(tk.key()).isEqualTo(telemetryName);
        }));
    });
  }

}
