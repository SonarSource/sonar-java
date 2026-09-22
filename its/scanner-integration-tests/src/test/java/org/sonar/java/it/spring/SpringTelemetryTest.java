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
package org.sonar.java.it.spring;

import com.sonarsource.scanner.integrationtester.dsl.ScannerOutputReader;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.sonar.java.it.ScannerIntegrationAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

class SpringTelemetryTest extends ScannerIntegrationAbstractTest {

  private static final int EXPECTED_BEAN_COUNT = 20;
  private static final int EXPECTED_INJECTION_POINT_COUNT = 6;
  private static final int EXPECTED_COMPONENT_SCAN_PACKAGE_COUNT = 1;
  private static final int EXPECTED_CONTEXT_COMPONENT_COUNT = 5;
  private static final int EXPECTED_SINGLE_ELEMENT_LIST_COUNT = 20;
  private static final int EXPECTED_SINGLE_ELEMENT_SET_COUNT = 25;
  private static final int EXPECTED_TWO_ELEMENT_SET_COUNT = 6;

  private static final long SPRING_CONTEXT_MODEL_SHALLOW_SIZE_BYTES = 32L;
  private static final long MODEL_COMPONENT_SHALLOW_SIZE_BYTES = 16L;
  private static final long EMPTY_HASH_MAP_SIZE_BYTES = 48L;
  private static final long SINGLE_ELEMENT_HASH_MAP_SIZE_BYTES = 160L;
  private static final long SIX_ELEMENT_HASH_MAP_SIZE_BYTES = 320L;
  private static final long TWENTY_ELEMENT_HASH_MAP_SIZE_BYTES = 832L;
  private static final long TWENTY_FOUR_ELEMENT_HASH_MAP_SIZE_BYTES = 960L;
  private static final long SINGLE_ELEMENT_HASH_SET_SIZE_BYTES = 176L;
  private static final long TWO_ELEMENT_HASH_SET_SIZE_BYTES = 208L;
  private static final long SINGLE_ELEMENT_LIST_SIZE_BYTES = 48L;
  private static final long BEAN_DEFINITION_HOLDER_SHALLOW_SIZE_BYTES = 40L;
  private static final long UPPER_BOUND_HEADROOM_MULTIPLIER = 3L;

  private static final long MINIMUM_EXPECTED_CONTEXT_MODEL_SIZE_BYTES =
    SPRING_CONTEXT_MODEL_SHALLOW_SIZE_BYTES
      + EXPECTED_CONTEXT_COMPONENT_COUNT * MODEL_COMPONENT_SHALLOW_SIZE_BYTES
      + EXPECTED_BEAN_COUNT * BEAN_DEFINITION_HOLDER_SHALLOW_SIZE_BYTES
      + EMPTY_HASH_MAP_SIZE_BYTES
      + SINGLE_ELEMENT_HASH_MAP_SIZE_BYTES
      + SIX_ELEMENT_HASH_MAP_SIZE_BYTES
      + TWENTY_ELEMENT_HASH_MAP_SIZE_BYTES
      + TWENTY_FOUR_ELEMENT_HASH_MAP_SIZE_BYTES
      + EXPECTED_SINGLE_ELEMENT_LIST_COUNT * SINGLE_ELEMENT_LIST_SIZE_BYTES
      + EXPECTED_SINGLE_ELEMENT_SET_COUNT * SINGLE_ELEMENT_HASH_SET_SIZE_BYTES
      + EXPECTED_TWO_ELEMENT_SET_COUNT * TWO_ELEMENT_HASH_SET_SIZE_BYTES;
  private static final long MAXIMUM_EXPECTED_CONTEXT_MODEL_SIZE_BYTES =
    UPPER_BOUND_HEADROOM_MULTIPLIER * MINIMUM_EXPECTED_CONTEXT_MODEL_SIZE_BYTES;

  @Test
  void spring_context_model_is_reported_as_telemetry() {
    var project = analyzeProject(Path.of("ambiguous-dependencies-should-be-resolved"), "S9352").getProject();

    assertThat(telemetryValue(project, "java.spring.bean_count"))
      .isEqualTo(Integer.toString(EXPECTED_BEAN_COUNT));
    assertThat(telemetryValue(project, "java.spring.bean_name_count"))
      .isEqualTo(Integer.toString(EXPECTED_BEAN_COUNT));
    assertThat(telemetryValue(project, "java.spring.injection_point_count"))
      .isEqualTo(Integer.toString(EXPECTED_INJECTION_POINT_COUNT));
    assertThat(telemetryValue(project, "java.spring.component_scan_package_count"))
      .isEqualTo(Integer.toString(EXPECTED_COMPONENT_SCAN_PACKAGE_COUNT));
    assertThat(telemetryValue(project, "java.spring.context_model_gathering_time_ms")).matches("\\d+");
    assertThat(telemetryValue(project, "java.spring.context_checks_time_ms")).matches("\\d+");
    long estimatedContextModelSizeBytes = Long.parseLong(
      telemetryValue(project, "java.spring.context_model_size_bytes"));
    assertThat(estimatedContextModelSizeBytes)
      .as("estimated size with headroom for strings, locations, and nested payloads excluded from the structural lower bound")
      .isGreaterThan(MINIMUM_EXPECTED_CONTEXT_MODEL_SIZE_BYTES)
      .isLessThan(MAXIMUM_EXPECTED_CONTEXT_MODEL_SIZE_BYTES);
  }

  private static String telemetryValue(ScannerOutputReader.AnalyzedProject project, String key) {
    ScannerOutputReader.TelemetryEntry entry = project.getTelemetryEntry(key);
    assertThat(entry).as("telemetry entry %s", key).isNotNull();
    return entry.value();
  }
}
