/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionTimeReportTest {

  private static final String NL = System.lineSeparator();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();
  public UnitTestClock clock = new UnitTestClock();
  public ExecutionTimeReport report = new ExecutionTimeReport(clock);

  void simulateAnalysis(String file, long timeMs) {
    report.start(file);
    clock.addMilliseconds(timeMs);
    report.end();
  }

  @Test
  void no_log_for_fast_total_analysis() {
    simulateAnalysis("f1", 2000);
    simulateAnalysis("f2", 2000);
    report.report();
    assertThat(logTester.logs(LoggerLevel.TRACE)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
    assertThat(report).hasToString("" +
      "    f1 (2000ms)" + NL +
      "    f2 (2000ms)");
  }

  @Test
  void no_log_when_each_file_analysis_is_fast() {
    for (int i = 0; i < 500; i++) {
      simulateAnalysis("f" + i, 500);
    }
    report.report();
    assertThat(logTester.logs(LoggerLevel.TRACE)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
    assertThat(report).hasToString("");
  }

  @Test
  void log_only_10_slowest_analysis() {
    simulateAnalysis("f1200", 1200);
    simulateAnalysis("f1400", 1400);
    simulateAnalysis("f100", 100);
    assertThat(report).hasToString("" +
      "    f1400 (1400ms)" + NL +
      "    f1200 (1200ms)");
    simulateAnalysis("f2800", 2800);
    simulateAnalysis("f1300", 1300);
    simulateAnalysis("f1700", 1700);
    simulateAnalysis("f200", 200);
    simulateAnalysis("f2900", 2900);
    assertThat(report).hasToString("" +
      "    f2900 (2900ms)" + NL +
      "    f2800 (2800ms)" + NL +
      "    f1700 (1700ms)" + NL +
      "    f1400 (1400ms)" + NL +
      "    f1300 (1300ms)" + NL +
      "    f1200 (1200ms)");
    simulateAnalysis("f1000", 1000);
    simulateAnalysis("f2000", 2000);
    simulateAnalysis("f1500", 1500);
    simulateAnalysis("f1600", 1600);
    simulateAnalysis("f1800", 1800);
    simulateAnalysis("f900", 900);
    simulateAnalysis("f1100", 1100);
    simulateAnalysis("f2700", 2700);
    assertThat(report).hasToString("" +
      "    f2900 (2900ms)" + NL +
      "    f2800 (2800ms)" + NL +
      "    f2700 (2700ms)" + NL +
      "    f2000 (2000ms)" + NL +
      "    f1800 (1800ms)" + NL +
      "    f1700 (1700ms)" + NL +
      "    f1600 (1600ms)" + NL +
      "    f1500 (1500ms)" + NL +
      "    f1400 (1400ms)" + NL +
      "    f1300 (1300ms)");
    simulateAnalysis("f2100", 2100);
    simulateAnalysis("f1900", 1900);
    simulateAnalysis("f2500", 2500);
    assertThat(report).hasToString("" +
      "    f2900 (2900ms)" + NL +
      "    f2800 (2800ms)" + NL +
      "    f2700 (2700ms)" + NL +
      "    f2500 (2500ms)" + NL +
      "    f2100 (2100ms)" + NL +
      "    f2000 (2000ms)" + NL +
      "    f1900 (1900ms)" + NL +
      "    f1800 (1800ms)" + NL +
      "    f1700 (1700ms)" + NL +
      "    f1600 (1600ms)");
    simulateAnalysis("f2300", 2300);
    simulateAnalysis("f2200", 2200);
    simulateAnalysis("f2400", 2400);
    simulateAnalysis("f2600", 2600);
    report.report();
    assertThat(logTester.logs(LoggerLevel.TRACE)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Slowest analyzed files:" + NL +
      "    f2900 (2900ms)" + NL +
      "    f2800 (2800ms)" + NL +
      "    f2700 (2700ms)" + NL +
      "    f2600 (2600ms)" + NL +
      "    f2500 (2500ms)" + NL +
      "    f2400 (2400ms)" + NL +
      "    f2300 (2300ms)" + NL +
      "    f2200 (2200ms)" + NL +
      "    f2100 (2100ms)" + NL +
      "    f2000 (2000ms)");
  }

  @Test
  void interrupt_the_report() {
    report.start("f1");
    clock.addMilliseconds(50_000);
    // do not call end()
    report.report();
    assertThat(logTester.logs(LoggerLevel.TRACE)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Slowest analyzed files:" + NL +
      "    f1 (50000ms)");
  }

  @Test
  void log_debug_level() {
    logTester.setLevel(LoggerLevel.DEBUG);
    simulateAnalysis("f1", 50);
    simulateAnalysis("f2", 2000);
    report.report();
    assertThat(logTester.logs(LoggerLevel.TRACE)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Analysis time of f2 (2000ms)");
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
    assertThat(report).hasToString("    f2 (2000ms)");
  }

  @Test
  void log_trace_level() {
    logTester.setLevel(LoggerLevel.TRACE);
    simulateAnalysis("f1", 50);
    simulateAnalysis("f2", 2000);
    report.report();
    assertThat(logTester.logs(LoggerLevel.TRACE)).contains(
      "Analysis time of f1 (50ms)",
      "Analysis time of f2 (2000ms)"
    );
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
    assertThat(report).hasToString("    f2 (2000ms)");
  }

  private static class UnitTestClock extends Clock {

    private Instant instant;

    public UnitTestClock() {
      this.instant = Instant.now();
    }

    public void addMilliseconds(long millisToAdd) {
      this.instant = instant.plusMillis(millisToAdd);
    }

    @Override
    public Instant instant() {
      return instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zoneId) {
      throw new UnsupportedOperationException();
    }
  }
}
