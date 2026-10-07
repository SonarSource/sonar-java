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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompilationMeasurementTest {

  @Test
  void measures_all_scopes_and_preserves_partial_output_after_failure() {
    String logs = """
      INFO Java bytecode compilation: scope=main status=SUCCESS sources=3 classes=4 time_ms=17
      INFO Java bytecode compilation: scope=test status=FAILED sources=2 classes=1 time_ms=5
      INFO Java bytecode compilation: scope=generated status=SKIPPED sources=0 classes=0 time_ms=0
      """;
    assertThat(CompilationMeasurement.read(logs, true)).containsEntry("comparison.compilation.status", "FAILED")
      .containsEntry("comparison.compilation.classes", "5").containsEntry("comparison.compilation.sources", "5")
      .containsEntry("comparison.compilation.time_ms", "22").containsEntry("comparison.compilation.main.status", "SUCCESS")
      .containsEntry("comparison.compilation.test.status", "FAILED");
  }

  @Test
  void distinguishes_missing_measurement_from_disabled_and_empty_compilation() {
    assertThat(CompilationMeasurement.read("", true)).containsOnly(Map.entry("comparison.compilation.status", "UNAVAILABLE"));
    assertThat(CompilationMeasurement.read("", false)).containsEntry("comparison.compilation.status", "DISABLED")
      .containsEntry("comparison.compilation.classes", "0").containsEntry("comparison.compilation.time_ms", "0");
    assertThat(CompilationMeasurement.read("Java bytecode compilation: scope=main status=SKIPPED sources=0 classes=0 time_ms=0", true))
      .containsEntry("comparison.compilation.status", "SKIPPED");
  }

  @Test
  void rejects_duplicate_phases_and_unexpected_compilation() {
    String phase = "Java bytecode compilation: scope=main status=SUCCESS sources=1 classes=1 time_ms=2";
    assertThatThrownBy(() -> CompilationMeasurement.read(phase + "\n" + phase, true))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate");
    assertThatThrownBy(() -> CompilationMeasurement.read(phase, false))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("disabled");
  }

  @Test
  void four_modes_control_source_roots_and_compilation_independently() {
    for (AnalysisMode mode : AnalysisMode.values()) {
      var properties = mode.properties(List.of("src/main/java", "dependency/src/main/java"), Map.of("sonar.java.internal.example", "true"));
      assertThat(properties).containsEntry("sonar.java.compileToByteCode", Boolean.toString(mode.compilation()))
        .containsEntry("sonar.java.sourcepath", mode.sourcePaths() ? "src/main/java,dependency/src/main/java" : "")
        .containsEntry("sonar.java.test.sourcepath", "").containsEntry("sonar.java.internal.example", "true");
    }
  }

  @Test
  void measured_order_reverses_and_rotates_without_omitting_modes() {
    assertThat(NoCompilationComparisonTest.measuredOrder(0)).containsExactly(AnalysisMode.values());
    assertThat(NoCompilationComparisonTest.measuredOrder(1)).containsExactly(AnalysisMode.COMBINED, AnalysisMode.BYTECODE,
      AnalysisMode.SOURCE_PATHS, AnalysisMode.BASELINE);
    assertThat(NoCompilationComparisonTest.measuredOrder(2)).containsExactly(AnalysisMode.SOURCE_PATHS, AnalysisMode.BYTECODE,
      AnalysisMode.COMBINED, AnalysisMode.BASELINE);
    for (int repetition = 0; repetition < 8; repetition++) {
      assertThat(NoCompilationComparisonTest.measuredOrder(repetition)).containsExactlyInAnyOrder(AnalysisMode.values());
    }
  }
}
