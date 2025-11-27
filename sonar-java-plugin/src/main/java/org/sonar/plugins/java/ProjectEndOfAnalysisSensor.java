/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.telemetry.Telemetry;

/**
 * Sensor that runs at the end of the project's analysis to send telemetry data.
 * Telemetry data is collected by several JavaSensor executions, one for each project's module, and aggregated in a shared Telemetry object.
 */
@Phase(name = Phase.Name.POST)
public class ProjectEndOfAnalysisSensor implements ProjectSensor {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectEndOfAnalysisSensor.class);

  private final Telemetry telemetry;

  public ProjectEndOfAnalysisSensor(Telemetry telemetry) {
    this.telemetry = telemetry;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguages(Java.KEY, Jasper.JSP_LANGUAGE_KEY).name("JavaProjectSensor");
  }

  @Override
  public void execute(SensorContext context) {
    telemetry.toMap().forEach((key, value) -> {
      LOG.debug("Telemetry {}: {}", key, value);
      context.addTelemetryProperty(key, value);
    });
  }

}
