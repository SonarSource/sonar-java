/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class DefaultTelemetryTest {

  private Telemetry telemetry;

  @BeforeEach
  void setUp() {
    telemetry = new DefaultTelemetry();
  }

  @Test
  void testAggregateAsSortedSet() {
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "21");
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "17");
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "8");
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "21");
    assertThat(telemetry.toMap()).containsExactly(
      entry("java.language.version", "8,17,21"));
  }

  @Test
  void testAggeregateAsSortedSetAbsent() {
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_DEPENDENCY_LOMBOK);
    assertThat(telemetry.toMap()).containsExactly(
      entry("java.dependency.lombok", "absent"));
  }

  @Test
  void testAggeregateAsSortedSetOneValueAndAbsent() {
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_DEPENDENCY_SPRING_WEB, "7.0.0-M1");
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_DEPENDENCY_SPRING_WEB);
    assertThat(telemetry.toMap()).containsExactly(
      entry("java.dependency.spring-web", "7.0.0-M1"));
  }

  @Test
  void testAggeregateAsSortedSetAbsentAndTwoValues() {
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_DEPENDENCY_LOMBOK);
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_DEPENDENCY_LOMBOK, "1.18.30");
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_DEPENDENCY_LOMBOK, "1.16.1");
    assertThat(telemetry.toMap()).containsExactly(
      entry("java.dependency.lombok", "1.16.1,1.18.30"));
  }

  @Test
  void testAggregateCounter() {
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_MODULE_COUNT, 1L);
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_MODULE_COUNT, 1L);
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_MODULE_COUNT, 1L);
    assertThat(telemetry.toMap()).containsExactly(
      entry("java.module_count", "3"));
  }

  @Test
  void testAggregateDifferentTypes() {
    telemetry.aggregateAsSortedSet(TelemetryKey.JAVA_LANGUAGE_VERSION, "21");
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_MODULE_COUNT, 1L);
    assertThat(telemetry.toMap()).containsExactly(
      entry("java.language.version", "21"),
      entry("java.module_count", "1"));
  }


  @Test
  void testAggregateFlagFalse() {
    telemetry.aggregateAsFlag(TelemetryKey.JAVA_IS_AUTOSCAN, false);
    telemetry.aggregateAsFlag(TelemetryKey.JAVA_IS_AUTOSCAN, false);
    assertThat(telemetry.toMap()).containsExactly(entry("java.is_autoscan", "false"));
  }

  @Test
  void testAggregateFlagTrue() {
    telemetry.aggregateAsFlag(TelemetryKey.JAVA_IS_AUTOSCAN, true);
    telemetry.aggregateAsFlag(TelemetryKey.JAVA_IS_AUTOSCAN, true);
    assertThat(telemetry.toMap()).containsExactly(entry("java.is_autoscan", "true"));
  }

  @Test
  void testAggregateFlagBoth() {
    telemetry.aggregateAsFlag(TelemetryKey.JAVA_IS_AUTOSCAN, true);
    telemetry.aggregateAsFlag(TelemetryKey.JAVA_IS_AUTOSCAN, false);
    telemetry.aggregateAsFlag(TelemetryKey.JAVA_IS_AUTOSCAN, true);
    assertThat(telemetry.toMap()).containsExactly(entry("java.is_autoscan", "mixed"));
  }
}
