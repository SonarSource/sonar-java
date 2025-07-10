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

import java.util.Map;

/**
 * Placeholder for {@link Telemetry} that ignores all calls. Its main use is to satisfy a dependency in SonarQube for IDE, which does not send telemetry.
 */
public class NoOpTelemetry implements Telemetry {

  @Override
  public void aggregateAsSortedSet(TelemetryKey key, String value) {
    // no operation
  }

  @Override
  public void aggregateAsSortedSet(TelemetryKey key) {
    // no operation
  }

  @Override
  public void aggregateAsCounter(TelemetryKey key, long value) {
    // no operation
  }

  @Override
  public Map<String, String> toMap() {
    return Map.of();
  }

}
