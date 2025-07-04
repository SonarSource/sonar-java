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
package org.sonar.java;

/**
 * For propagating access to telemetry to components that need it.
 */
public interface Telemetry {
  // This will forward the call to `addTelemetryProperty`.
  // We chose a different name to make textual search for the real thing easier.
  void addMetric(String key, String value);

  default void addMetric(String key, boolean value) {
    addMetric(key, String.valueOf(value));
  }

  default void addMetric(String key, int value) {
    addMetric(key, String.valueOf(value));
  }

  default void addMetric(String key, long value) {
    addMetric(key, String.valueOf(value));
  }
}
