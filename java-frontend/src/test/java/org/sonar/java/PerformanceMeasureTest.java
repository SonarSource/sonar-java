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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.PerformanceMeasure.Duration;
import org.sonar.java.PerformanceMeasure.DurationReport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.sonar.java.PerformanceMeasure.ensureParentDirectoryExists;

class PerformanceMeasureTest {

  public AtomicLong timeNanos = new AtomicLong(System.currentTimeMillis() * 1_000_000);

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  public List<String> arrayList = new ArrayList<>();

  @Test
  void not_active_system_property() {
    Configuration config = createConfig(false, null , LoggerLevel.DEBUG);
    DurationReport duration_1 = PerformanceMeasure.start(config, "root", timeNanos::get);
    Duration duration_1_1 = PerformanceMeasure.start("cat-1");
    duration_1_1.stop();
    Duration duration_1_2 = PerformanceMeasure.start(arrayList);
    duration_1_2.stop();
    duration_1.stopAndLog(null, false);
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
  }

  @Test
  void active_system_property(@TempDir File workDir) {
    Configuration config = createConfig(true, "", LoggerLevel.DEBUG);
    DurationReport duration = PerformanceMeasure.start(config, "root", timeNanos::get);

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
    Duration duration_2_1 = PerformanceMeasure.start(arrayList);
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

    duration.stopAndLog(workDir, false);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("" +
      "Performance Measures:\n" +
      "{ \"name\": \"root\", \"calls\": 1, \"durationNanos\": 1280715556, \"children\": [\n" +
      "    { \"name\": \"cat-1\", \"calls\": 1, \"durationNanos\": 1195595132, \"children\": [\n" +
      "        { \"name\": \"sub-cat-1\", \"calls\": 1, \"durationNanos\": 234453958 },\n" +
      "        { \"name\": \"sub-cat-2\", \"calls\": 1, \"durationNanos\": 700123345 }\n" +
      "      ]\n" +
      "    },\n" +
      "    { \"name\": \"cat-2\", \"calls\": 1, \"durationNanos\": 85120424, \"children\": [\n" +
      "        { \"name\": \"ArrayList\", \"calls\": 1, \"durationNanos\": 32553812 },\n" +
      "        { \"name\": \"sub-cat-2\", \"calls\": 1, \"durationNanos\": 1800000 }\n" +
      "      ]\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  void append_measurement_cost(@TempDir File workDir) throws IOException {
    Configuration config = createConfig(true, null, LoggerLevel.DEBUG);
    DurationReport duration_1 = PerformanceMeasure.start(config, "root", System::nanoTime);
    timeNanos.addAndGet(1_382_190L);
    duration_1.stopAndLog(workDir, true);

    Path jsonPath = workDir.toPath().resolve("sonar.java.performance.measure.json");
    String jsonContent = new String(Files.readAllBytes(jsonPath), UTF_8);
    // measurements may vary from one test to another, but should not be zero
    jsonContent = jsonContent
      .replaceAll("(\"name\": \"(?:nanoTime|observationCost)\", \"calls\": 1, \"durationNanos\": )\\d++(?=[, ])", "$1ZERO_OR_MORE")
      .replaceAll("\"durationNanos\": 0(?=[, ])", "\"durationNanos\": ZERO")
      .replaceAll("\"durationNanos\": \\d++(?=[, ])", "\"durationNanos\": NOT_ZERO");

    assertThat(jsonContent).isEqualTo("" +
      "{ \"name\": \"root\", \"calls\": 1, \"durationNanos\": NOT_ZERO, \"children\": [\n" +
      "    { \"name\": \"#MeasurementCost_v1\", \"calls\": 1, \"durationNanos\": NOT_ZERO, \"children\": [\n" +
      "        { \"name\": \"createChild\", \"calls\": 1, \"durationNanos\": NOT_ZERO },\n" +
      "        { \"name\": \"incrementChild\", \"calls\": 1, \"durationNanos\": NOT_ZERO },\n" +
      "        { \"name\": \"nanoTime\", \"calls\": 1, \"durationNanos\": ZERO_OR_MORE },\n" +
      "        { \"name\": \"observationCost\", \"calls\": 1, \"durationNanos\": ZERO_OR_MORE }\n" +
      "      ]\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  void merge_performance_measures(@TempDir File workDir) throws IOException {
    Configuration config = createConfig(true, null, LoggerLevel.DEBUG);
    DurationReport duration_1 = PerformanceMeasure.start(config, "root", timeNanos::get);
    timeNanos.addAndGet(1_382_190L);

    Duration duration_1_1 = PerformanceMeasure.start("child-1");
    timeNanos.addAndGet(47_442_121L);
    duration_1_1.stop();

    timeNanos.addAndGet(2_456_121L);
    duration_1.stopAndLog(workDir, false);

    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Performance Measures:\n" +
      "{ \"name\": \"root\", \"calls\": 1, \"durationNanos\": 51280432, \"children\": [\n" +
      "    { \"name\": \"child-1\", \"calls\": 1, \"durationNanos\": 47442121 }\n" +
      "  ]\n" +
      "}");

    DurationReport duration_2 = PerformanceMeasure.start(config, "root", timeNanos::get);
    timeNanos.addAndGet(2_176_361L);

    Duration duration_2_1 = PerformanceMeasure.start("child-1");
    timeNanos.addAndGet(32_478_123L);
    duration_2_1.stop();

    Duration duration_2_2 = PerformanceMeasure.start("child-2");
    timeNanos.addAndGet(13_237_123L);
    duration_2_2.stop();

    timeNanos.addAndGet(12_567_151L);
    duration_2.stopAndLog(workDir, false);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Performance Measures:\n" +
      "{ \"name\": \"root\", \"calls\": 1, \"durationNanos\": 60458758, \"children\": [\n" +
      "    { \"name\": \"child-1\", \"calls\": 1, \"durationNanos\": 32478123 },\n" +
      "    { \"name\": \"child-2\", \"calls\": 1, \"durationNanos\": 13237123 }\n" +
      "  ]\n" +
      "}");

    Path jsonPath = workDir.toPath().resolve("sonar.java.performance.measure.json");
    String jsonContent = new String(Files.readAllBytes(jsonPath), UTF_8);
    assertThat(jsonContent).isEqualTo("{ \"name\": \"root\", \"calls\": 2, \"durationNanos\": 111739190, \"children\": [\n" +
      "    { \"name\": \"child-1\", \"calls\": 2, \"durationNanos\": 79920244 },\n" +
      "    { \"name\": \"child-2\", \"calls\": 1, \"durationNanos\": 13237123 }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  void custom_performance_measure_file(@TempDir File workDir) throws IOException {
    Path customPerformanceFile = workDir.toPath().resolve("new-directory").resolve("custom-path.json");
    Configuration config = createConfig(true, customPerformanceFile.toString(), LoggerLevel.DEBUG);
    DurationReport duration_1 = PerformanceMeasure.start(config, "root", timeNanos::get);
    timeNanos.addAndGet(1_382_190L);
    duration_1.stopAndLog(workDir, false);

    String jsonContent = new String(Files.readAllBytes(customPerformanceFile), UTF_8);
    assertThat(jsonContent).isEqualTo("{ \"name\": \"root\", \"calls\": 1, \"durationNanos\": 1382190 }");
  }

  @Test
  void can_not_merge_incompatible_json(@TempDir File workDir) throws IOException {
    Configuration config = createConfig(true, null, LoggerLevel.DEBUG);
    DurationReport duration_1 = PerformanceMeasure.start(config, "root1", timeNanos::get);
    timeNanos.addAndGet(1_382_190L);
    duration_1.stopAndLog(workDir, false);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();

    DurationReport duration_2 = PerformanceMeasure.start(config, "root2", timeNanos::get);
    timeNanos.addAndGet(2_176_361L);
    duration_2.stopAndLog(workDir, false);

    assertThat(logTester.logs(LoggerLevel.ERROR))
      .contains("Can't save performance measure: Incompatible name 'root2' and 'root1'");
  }

  @Test
  void working_directory_does_not_exist(@TempDir File parentWorkDir) throws IOException {
    File workDir = new File(parentWorkDir, "new-directory");
    Configuration config = createConfig(true, null, LoggerLevel.DEBUG);
    DurationReport duration = PerformanceMeasure.start(config, "root", timeNanos::get);
    timeNanos.addAndGet(1_382_190L);
    duration.stopAndLog(workDir, false);

    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Performance Measures:\n" +
      "{ \"name\": \"root\", \"calls\": 1, \"durationNanos\": 1382190 }");

    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0))
      .startsWith("Can't save performance measure: Directory does not exist: ");

    assertThat(parentWorkDir).isEmptyDirectory();
  }

  @Test
  void log_info_and_null_working_directory() throws IOException {
    File workDir = null;
    Configuration config = createConfig(true, null, LoggerLevel.INFO);
    DurationReport duration = PerformanceMeasure.start(config, "root", timeNanos::get);
    timeNanos.addAndGet(1_382_190L);
    duration.stopAndLog(workDir, true);

    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  void ensure_parent_directory_exists(@TempDir Path workDir) throws IOException {
    assertThatNoException().isThrownBy(() -> ensureParentDirectoryExists(Paths.get("file-without-parent.json")));

    ensureParentDirectoryExists(workDir.resolve("parent-dir/file.json"));
    assertThat(workDir.resolve("parent-dir")).isDirectory();
  }

  private static Configuration createConfig(boolean measurePerformance, String performanceMeasurePath, LoggerLevel loggerLevel) {
    Loggers.get(PerformanceMeasure.class).setLevel(loggerLevel);
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.performance.measure", measurePerformance ? "true" : "false");
    settings.setProperty("sonar.java.performance.measure.path", performanceMeasurePath);
    return new ConfigurationBridge(settings);
  }

}
