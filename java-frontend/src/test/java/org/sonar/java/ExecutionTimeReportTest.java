/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExecutionTimeReportTest {

  private static final String NL = System.lineSeparator();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);
  public UnitTestClock clock = new UnitTestClock();
  public ExecutionTimeReport report = new ExecutionTimeReport(clock);

  void simulateAnalysis(String filename, long timeMs) {
    InputFile inputFile = mockEmptyInputFile(filename);
    report.start(inputFile);
    clock.addMilliseconds(timeMs);
    report.end();
  }

  InputFile mockEmptyInputFile(String filename) {
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.toString()).thenReturn(filename);
    try {
      when(inputFile.contents()).thenReturn("ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ");
    } catch (IOException ignored) {
      // Ignore the exception
    }
    return inputFile;
  }

  @Test
  void no_log_for_fast_total_analysis() {
    simulateAnalysis("f1", 2000);
    simulateAnalysis("f2", 2000);
    report.report();
    assertThat(logTester.logs(Level.TRACE)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).containsExactly(
      "Analysis time of f1 (2000ms)",
      "Analysis time of f2 (2000ms)");
    assertThat(logTester.logs(Level.INFO)).isEmpty();
    assertThat(report).hasToString("" +
      "    f1 (2000ms, 52B)" + NL +
      "    f2 (2000ms, 52B)");
  }

  @Test
  void no_log_when_each_file_analysis_is_fast() {
    for (int i = 0; i < 500; i++) {
      simulateAnalysis("f" + i, 500);
    }
    report.report();
    assertThat(logTester.logs(Level.TRACE)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
    assertThat(logTester.logs(Level.INFO)).isEmpty();
    assertThat(report).hasToString("");
  }

  @Test
  void log_only_10_slowest_analysis() {
    simulateAnalysis("f1200", 1200);
    simulateAnalysis("f1400", 1400);
    simulateAnalysis("f100", 100);
    assertThat(report).hasToString("" +
      "    f1400 (1400ms, 52B)" + NL +
      "    f1200 (1200ms, 52B)");
    simulateAnalysis("f2800", 2800);
    simulateAnalysis("f1300", 1300);
    simulateAnalysis("f1700", 1700);
    simulateAnalysis("f200", 200);
    simulateAnalysis("f2900", 2900);
    assertThat(report).hasToString("" +
      "    f2900 (2900ms, 52B)" + NL +
      "    f2800 (2800ms, 52B)" + NL +
      "    f1700 (1700ms, 52B)" + NL +
      "    f1400 (1400ms, 52B)" + NL +
      "    f1300 (1300ms, 52B)" + NL +
      "    f1200 (1200ms, 52B)");
    simulateAnalysis("f1000", 1000);
    simulateAnalysis("f2000", 2000);
    simulateAnalysis("f1500", 1500);
    simulateAnalysis("f1600", 1600);
    simulateAnalysis("f1800", 1800);
    simulateAnalysis("f900", 900);
    simulateAnalysis("f1100", 1100);
    simulateAnalysis("f2700", 2700);
    assertThat(report).hasToString("" +
      "    f2900 (2900ms, 52B)" + NL +
      "    f2800 (2800ms, 52B)" + NL +
      "    f2700 (2700ms, 52B)" + NL +
      "    f2000 (2000ms, 52B)" + NL +
      "    f1800 (1800ms, 52B)" + NL +
      "    f1700 (1700ms, 52B)" + NL +
      "    f1600 (1600ms, 52B)" + NL +
      "    f1500 (1500ms, 52B)" + NL +
      "    f1400 (1400ms, 52B)" + NL +
      "    f1300 (1300ms, 52B)");
    simulateAnalysis("f2100", 2100);
    simulateAnalysis("f1900", 1900);
    simulateAnalysis("f2500", 2500);
    assertThat(report).hasToString("" +
      "    f2900 (2900ms, 52B)" + NL +
      "    f2800 (2800ms, 52B)" + NL +
      "    f2700 (2700ms, 52B)" + NL +
      "    f2500 (2500ms, 52B)" + NL +
      "    f2100 (2100ms, 52B)" + NL +
      "    f2000 (2000ms, 52B)" + NL +
      "    f1900 (1900ms, 52B)" + NL +
      "    f1800 (1800ms, 52B)" + NL +
      "    f1700 (1700ms, 52B)" + NL +
      "    f1600 (1600ms, 52B)");
    simulateAnalysis("f2300", 2300);
    simulateAnalysis("f2200", 2200);
    simulateAnalysis("f2400", 2400);
    simulateAnalysis("f2600", 2600);
    report.report();
    assertThat(logTester.logs(Level.TRACE)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).containsExactly(
      "Analysis time of f1200 (1200ms)",
      "Analysis time of f1400 (1400ms)",
      "Analysis time of f2800 (2800ms)",
      "Analysis time of f1300 (1300ms)",
      "Analysis time of f1700 (1700ms)",
      "Analysis time of f2900 (2900ms)",
      "Analysis time of f1000 (1000ms)",
      "Analysis time of f2000 (2000ms)",
      "Analysis time of f1500 (1500ms)",
      "Analysis time of f1600 (1600ms)",
      "Analysis time of f1800 (1800ms)",
      "Analysis time of f1100 (1100ms)",
      "Analysis time of f2700 (2700ms)",
      "Analysis time of f2100 (2100ms)",
      "Analysis time of f1900 (1900ms)",
      "Analysis time of f2500 (2500ms)",
      "Analysis time of f2300 (2300ms)",
      "Analysis time of f2200 (2200ms)",
      "Analysis time of f2400 (2400ms)",
      "Analysis time of f2600 (2600ms)"
    );
    assertThat(logTester.logs(Level.INFO)).contains("Slowest analyzed files:" + NL +
      "    f2900 (2900ms, 52B)" + NL +
      "    f2800 (2800ms, 52B)" + NL +
      "    f2700 (2700ms, 52B)" + NL +
      "    f2600 (2600ms, 52B)" + NL +
      "    f2500 (2500ms, 52B)" + NL +
      "    f2400 (2400ms, 52B)" + NL +
      "    f2300 (2300ms, 52B)" + NL +
      "    f2200 (2200ms, 52B)" + NL +
      "    f2100 (2100ms, 52B)" + NL +
      "    f2000 (2000ms, 52B)");
  }

  @Test
  void interrupt_the_report() throws IOException {
    InputFile inputFile = mockEmptyInputFile("f1");
    report.start(inputFile);
    clock.addMilliseconds(50_000);
    // do not call end()
    report.report();
    assertThat(logTester.logs(Level.TRACE)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).containsExactly("Analysis time of f1 (50000ms)");
    assertThat(logTester.logs(Level.INFO)).contains("Slowest analyzed files:" + NL +
      "    f1 (50000ms, 52B)");
  }

  @Test
  void log_as_batch() throws IOException {
    simulateAnalysis("f1", 50_000);
    report.reportAsBatch();
    assertThat(logTester.logs(Level.TRACE)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).containsExactly("Analysis time of f1 (50000ms)");
    assertThat(logTester.logs(Level.INFO)).contains("Slowest analyzed files (batch mode enabled):" + NL +
      "    f1 (50000ms, 52B)");
  }

  @Test
  void log_debug_level() {
    logTester.setLevel(Level.DEBUG);
    simulateAnalysis("f1", 50);
    simulateAnalysis("f2", 2000);
    report.report();
    assertThat(logTester.logs(Level.TRACE)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains("Analysis time of f2 (2000ms)");
    assertThat(logTester.logs(Level.INFO)).isEmpty();
    assertThat(report).hasToString("    f2 (2000ms, 52B)");
  }

  @Test
  void log_trace_level() {
    logTester.setLevel(Level.TRACE);
    simulateAnalysis("f1", 50);
    simulateAnalysis("f2", 2000);
    report.report();
    assertThat(logTester.logs(Level.TRACE)).contains(
      "Analysis time of f1 (50ms)",
      "Analysis time of f2 (2000ms)"
    );
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
    assertThat(logTester.logs(Level.INFO)).isEmpty();
    assertThat(report).hasToString("    f2 (2000ms, 52B)");
  }

  @Test
  void use_default_file_length_of_minus_1_when_contents_cannot_be_read() throws IOException {
    InputFile inputFile = mockEmptyInputFile("default_size");
    when(inputFile.contents()).thenThrow(IOException.class);
    report.start(inputFile);
    clock.addMilliseconds(50_000);
    report.end();
    report.report();
    assertThat(logTester.logs(Level.INFO)).contains("Slowest analyzed files:" + NL +
      "    default_size (50000ms, -1B)");
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
