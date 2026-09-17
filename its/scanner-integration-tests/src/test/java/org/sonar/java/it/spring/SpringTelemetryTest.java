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

  @Test
  void spring_context_model_is_reported_as_telemetry() {
    var project = analyzeProject(Path.of("ambiguous-dependencies-should-be-resolved"), "S9352").getProject();

    assertThat(telemetryValue(project, "java.spring.bean_count")).isEqualTo("20");
    assertThat(telemetryValue(project, "java.spring.bean_name_count")).isEqualTo("20");
    assertThat(telemetryValue(project, "java.spring.injection_point_count")).isEqualTo("6");
    assertThat(telemetryValue(project, "java.spring.component_scan_package_count")).isEqualTo("1");
    assertThat(Long.parseLong(telemetryValue(project, "java.spring.context_model_size_bytes"))).isPositive();
  }

  private static String telemetryValue(ScannerOutputReader.AnalyzedProject project, String key) {
    ScannerOutputReader.TelemetryEntry entry = project.getTelemetryEntry(key);
    assertThat(entry).as("telemetry entry %s", key).isNotNull();
    return entry.value();
  }
}
