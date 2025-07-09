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

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.jsp.Jasper;

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
    Map<String, String> telemetryData = telemetry.toMap();
    if (LOG.isDebugEnabled()) {
      LOG.debug("SonarJava Telemetry: {}", telemetryData);
    }
    telemetryData.forEach(context::addTelemetryProperty);
  }

}
