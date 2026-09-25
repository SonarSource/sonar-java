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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.model.springcontext.SpringContextModelMetrics;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.telemetry.TelemetryKey;

/**
 * Sensor that runs at the end of the project's analysis to send telemetry data.
 * Telemetry data is collected by several JavaSensor executions, one for each project's module, and aggregated in a shared Telemetry object.
 */
@Phase(name = Phase.Name.POST)
@DependsUpon(value = "CollectSpringContextBeforeSendingTelemetry")
public class ProjectEndOfAnalysisSensor implements ProjectSensor {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectEndOfAnalysisSensor.class);

  private final Telemetry telemetry;
  private final SpringContextModel springContextModel;

  public ProjectEndOfAnalysisSensor(Telemetry telemetry, SpringContextModel springContextModel) {
    this.telemetry = telemetry;
    this.springContextModel = springContextModel;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguages(Java.KEY, Jasper.JSP_LANGUAGE_KEY).name("JavaProjectSensor");
  }

  @Override
  public void execute(SensorContext context) {
    recordSpringTelemetry();
    telemetry.toMap().forEach((key, value) -> {
      LOG.debug("Telemetry {}: {}", key, value);
      context.addTelemetryProperty(key, value);
    });
  }

  /**
   * Records the Spring counters unconditionally, so that projects without Spring report zeros rather than nothing and
   * remain distinguishable from projects analyzed by a version of the analyzer that did not report them at all.
   */
  private void recordSpringTelemetry() {
    var metrics = SpringContextModelMetrics.of(springContextModel);
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_BEAN_COUNT, metrics.beanCount());
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_BEAN_NAME_COUNT, metrics.beanNameCount());
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_INJECTION_POINT_COUNT, metrics.injectionPointCount());
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_COMPONENT_SCAN_PACKAGE_COUNT, metrics.componentScanPackageCount());
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_CONTEXT_MODEL_GATHERING_TIME_MS, 0L);
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_CONTEXT_CHECKS_TIME_MS, 0L);
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_CONTEXT_MODEL_SIZE_BYTES, metrics.estimatedSizeInBytes());
  }

}
