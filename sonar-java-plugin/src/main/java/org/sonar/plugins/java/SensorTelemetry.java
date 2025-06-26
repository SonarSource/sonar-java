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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.java.Telemetry;
import org.sonar.java.TelemetryKey;

/**
 * Wraps up {@link SensorContext} to allow passing it around without exposing other APIs.
 */
public class SensorTelemetry implements Telemetry {
  private static final Logger LOG = LoggerFactory.getLogger(SensorTelemetry.class);

  private final SensorContext context;

  public SensorTelemetry(SensorContext context) {
    this.context = context;
  }

  @Override
  public void addMetric(TelemetryKey key, String value) {
    LOG.error("{}={}", key.key(), value);
    // this.context.addTelemetryProperty(key.key(), value);
  }
}
