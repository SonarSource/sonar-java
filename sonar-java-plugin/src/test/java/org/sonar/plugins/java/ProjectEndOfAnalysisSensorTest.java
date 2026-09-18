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
package org.sonar.plugins.java;

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.telemetry.DefaultTelemetry;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.telemetry.TelemetryKey;
import org.sonar.scanner.plugin.api.impl.sensor.DefaultSensorDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectEndOfAnalysisSensorTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void test_describe() {
    var sensor = new ProjectEndOfAnalysisSensor(new NoOpTelemetry(), new SpringContextModel());
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("JavaProjectSensor");
    assertThat(descriptor.languages()).containsExactly("java", "jsp");
  }

  @Test
  void test_telemetry(@TempDir Path tempDir) {
    logTester.setLevel(Level.DEBUG);
    var telemetry = new DefaultTelemetry();
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "21");
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_MODULE_COUNT, "3");
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_CONTEXT_MODEL_GATHERING_TIME_MS, 12L);
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_CONTEXT_CHECKS_TIME_MS, 34L);
    var springContextModel = new SpringContextModel();
    var sensor = new ProjectEndOfAnalysisSensor(telemetry, springContextModel);
    SensorContextTester context = SensorContextTester.create(tempDir);
    sensor.execute(context);
    String contextModelSize = context.getTelemetryProperties().get("java.spring.context_model_size_bytes");
    assertThat(Long.parseLong(contextModelSize)).isPositive();
    assertThat(logTester.logs(Level.DEBUG)).containsExactly(
      "Telemetry java.language.version: 21",
      "Telemetry java.module_count: 3",
      "Telemetry java.spring.bean_count: 0",
      "Telemetry java.spring.bean_name_count: 0",
      "Telemetry java.spring.component_scan_package_count: 0",
      "Telemetry java.spring.context_checks_time_ms: 34",
      "Telemetry java.spring.context_model_gathering_time_ms: 12",
      "Telemetry java.spring.context_model_size_bytes: " + contextModelSize,
      "Telemetry java.spring.injection_point_count: 0");
  }

  @Test
  void test_spring_telemetry(@TempDir Path tempDir) {
    var springContextModel = new SpringContextModel();
    springContextModel.getBeanDefinitionRegistry().addBeanDefinition("myBean", newHolder("com.acme.MyBean"));
    springContextModel.getBeanDefinitionRegistry().addBeanDefinition("myOtherBean", newHolder("com.acme.MyOtherBean"));
    springContextModel.getTypeToDependenciesIndex().addDependencyForType("com.acme.MyBean", "myBean", "module-a", newLocation());
    springContextModel.getProjectPackageScan().addPackage("module-a", "com.acme");

    var sensor = new ProjectEndOfAnalysisSensor(new DefaultTelemetry(), springContextModel);
    SensorContextTester context = SensorContextTester.create(tempDir);
    sensor.execute(context);

    assertThat(context.getTelemetryProperties())
      .containsEntry("java.spring.bean_count", "2")
      .containsEntry("java.spring.bean_name_count", "2")
      .containsEntry("java.spring.injection_point_count", "1")
      .containsEntry("java.spring.component_scan_package_count", "1")
      .containsEntry("java.spring.context_model_gathering_time_ms", "0")
      .containsEntry("java.spring.context_checks_time_ms", "0");
    assertThat(Long.parseLong(context.getTelemetryProperties().get("java.spring.context_model_size_bytes"))).isPositive();
  }

  private static BeanDefinitionHolder newHolder(String type) {
    return new BeanDefinitionHolder.Builder(type, "module-a", "com.acme", newLocation()).build();
  }

  private static BeanLocation newLocation() {
    return new BeanLocation(null, new AnalyzerMessage.TextSpan(1));
  }
}
