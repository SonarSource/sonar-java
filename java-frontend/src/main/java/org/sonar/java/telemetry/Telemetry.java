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
import org.sonar.api.ExtensionPoint;
import org.sonar.api.scanner.ScannerSide;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Provides access to the APIs for reporting telemetry.
 */
@ScannerSide
@SonarLintSide
@ExtensionPoint
public interface Telemetry {

  /**
   * Aggregates all the given values as a sorted set for the given key.
   * The final map will contain the key and a comma-separated list of sorted values.
   */
  void aggregateAsSortedSet(TelemetryKey key, String value);

  /**
   * Same as {@link #aggregateAsSortedSet(TelemetryKey, String)}, but no value is added.
   * If this is the only call for the key, then the final map will be empty for the key.
   */
  void aggregateAsSortedSet(TelemetryKey key);

  /**
   * Aggregates all the given values as a sum for the given key.
   */
  void aggregateAsCounter(TelemetryKey key, long value);

  /**
   * @return convert all the different kind of key / value pairs type into a string / string map.
   */
  Map<String, String> toMap();

}
