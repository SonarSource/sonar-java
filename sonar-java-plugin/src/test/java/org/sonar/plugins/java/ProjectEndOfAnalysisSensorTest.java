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
package org.sonar.plugins.java;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.telemetry.TelemetryKey;
import org.sonar.java.telemetry.TelemetryStorage;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectEndOfAnalysisSensorTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void test_describe() {
    var sensor = new ProjectEndOfAnalysisSensor(new NoOpTelemetry());
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("JavaProjectSensor");
    assertThat(descriptor.languages()).containsExactly("java", "jsp");
  }

  @Test
  void test_telemetry(@TempDir Path tempDir) {
    logTester.setLevel(Level.DEBUG);
    var telemetry = new TelemetryStorage();
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "21");
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_MODULE_COUNT, "3");
    var sensor = new ProjectEndOfAnalysisSensor(telemetry);
    SensorContextTester context = SensorContextTester.create(tempDir);
    sensor.execute(context);
    assertThat(logTester.logs(Level.DEBUG)).containsExactly(
      "Telemetry java.language.version: 21",
      "Telemetry java.module_count: 3");
  }

}
