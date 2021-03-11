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

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.PerformanceMeasure.Duration;
import org.sonar.java.PerformanceMeasure.DurationReport;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceMeasureTest {

  public AtomicLong timeNanos = new AtomicLong(System.currentTimeMillis() * 1_000_000);

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void not_active_system_property() {
    MapSettings settings = new MapSettings();
    Configuration config = new ConfigurationBridge(settings);
    DurationReport duration_1 = PerformanceMeasure.start(config, "root", timeNanos::get);
    Duration duration_1_1 = PerformanceMeasure.start("cat-1");
    duration_1_1.stop();
    Duration duration_1_2 = PerformanceMeasure.start(settings);
    duration_1_2.stop();
    duration_1.stopAndLog();
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
  }

  @Test
  void active_system_property() {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.performance.measure", "true");
    Configuration config = new ConfigurationBridge(settings);
    DurationReport duration = PerformanceMeasure.start(config, "root", timeNanos::get);

    Duration root_duration = PerformanceMeasure.start("root");

    Duration duration_1 = PerformanceMeasure.start("cat-1");
    timeNanos.addAndGet(47_442_121L);
    Duration duration_1_1 = PerformanceMeasure.start("sub-cat-1");
    timeNanos.addAndGet(234_453_958L);
    duration_1_1.stop();
    timeNanos.addAndGet(123_183_297L);
    Duration duration_1_2 = PerformanceMeasure.start("sub-cat-2");
    timeNanos.addAndGet(700_123_345L);
    duration_1_2.stop();
    timeNanos.addAndGet(90_392_411L);
    duration_1.stop();

    Duration duration_2 = PerformanceMeasure.start("cat-2");
    Duration duration_2_1 = PerformanceMeasure.start(settings);
    timeNanos.addAndGet(32_553_812L);
    duration_2_1.stop();
    timeNanos.addAndGet(36_432_090L);
    // intentionally call "start()" without "stop()" to simulate a badly handled exception
    PerformanceMeasure.start("sub-cat-2");
    timeNanos.addAndGet(1_900_000L);
    // "start()" using the same name should recover from previous mistake
    Duration duration_2_2_duplicated = PerformanceMeasure.start("sub-cat-2");
    timeNanos.addAndGet(1_800_000L);
    duration_2_2_duplicated.stop();
    // stop can be called twice by mistake without consequences
    duration_2_2_duplicated.stop();
    timeNanos.addAndGet(12_434_522L);
    duration_2.stop();

    duration.stopAndLog();
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("" +
      "Performance Measures:\n" +
      "  {perf} /root ( 1,280,715 micro_s during 1 call(s), minus children duration 0 micro_s )\n" +
      "  {perf} /root/cat-1 ( 1,195,595 micro_s during 1 call(s), minus children duration 261,017 micro_s )\n" +
      "  {perf} /root/cat-1/sub-cat-1 ( 234,453 micro_s during 1 call(s))\n" +
      "  {perf} /root/cat-1/sub-cat-2 ( 700,123 micro_s during 1 call(s))\n" +
      "  {perf} /root/cat-2 ( 85,120 micro_s during 1 call(s), minus children duration 50,766 micro_s )\n" +
      "  {perf} /root/cat-2/MapSettings ( 32,553 micro_s during 1 call(s))\n" +
      "  {perf} /root/cat-2/sub-cat-2 ( 1,800 micro_s during 1 call(s))\n");
  }

}
