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
package org.sonar.plugins.surefire.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnitTestClassReportTest {

  @Test
  void shouldIncrementCounters() {
    UnitTestClassReport report = new UnitTestClassReport();
    report.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR).setDurationMilliseconds(500L));
    report.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_OK).setDurationMilliseconds(200L));
    //Some negative duration can occur due to bug in surefire.
    report.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_OK).setDurationMilliseconds(-200L));
    report.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_SKIPPED));

    assertThat(report.getResults()).hasSize(4);
    assertThat(report.getSkipped()).isEqualTo(1);
    assertThat(report.getTests()).isEqualTo(4);
    assertThat(report.getDurationMilliseconds()).isEqualTo(500L + 200L);
    assertThat(report.getErrors()).isEqualTo(1);
    assertThat(report.getFailures()).isZero();
    assertThat(report.getNegativeTimeTestNumber()).isEqualTo(1L);
  }

  @Test
  void shouldHaveEmptyReport() {
    UnitTestClassReport report = new UnitTestClassReport();
    assertThat(report.getResults()).isEmpty();
    assertThat(report.getSkipped()).isZero();
    assertThat(report.getTests()).isZero();
    assertThat(report.getDurationMilliseconds()).isZero();
    assertThat(report.getErrors()).isZero();
    assertThat(report.getFailures()).isZero();
  }
}
