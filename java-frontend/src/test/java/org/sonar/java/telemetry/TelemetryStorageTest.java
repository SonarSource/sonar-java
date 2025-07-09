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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class TelemetryStorageTest {

  @Test
  void testAggregateAsSortedSet() {
    var storage = new TelemetryStorage();
    storage.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "21");
    storage.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "17");
    storage.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "21");
    assertThat(storage.toMap()).containsExactly(
      entry("java.language.version", "17,21"));
  }

  @Test
  void testAggregateCounter() {
    var storage = new TelemetryStorage();
    storage.aggregateAsCounter(TelemetryKey.JAVA_MODULE_COUNT, 1L);
    storage.aggregateAsCounter(TelemetryKey.JAVA_MODULE_COUNT, 1L);
    storage.aggregateAsCounter(TelemetryKey.JAVA_MODULE_COUNT, 1L);
    assertThat(storage.toMap()).containsExactly(
      entry("java.module_count", "3"));
  }

  @Test
  void testAggregateDifferentTypes() {
    var storage = new TelemetryStorage();
    storage.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "21");
    storage.aggregateAsCounter(TelemetryKey.JAVA_MODULE_COUNT, 1L);
    assertThat(storage.toMap()).containsExactly(
      entry("java.language.version", "21"),
      entry("java.module_count", "1"));
  }

}
