/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.it.java.suite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparisonSettingsTest {

  @TempDir
  Path directory;

  @Test
  void defaults_use_pinned_versions_and_three_repetitions() throws IOException {
    var settings = ComparisonSettings.load(new Properties());
    assertThat(settings.serverVersion()).isEqualTo("26.10.0.132816");
    assertThat(settings.scannerVersion()).isEqualTo("5.9.0.7291");
    assertThat(settings.repetitions()).isEqualTo(3);
    assertThat(settings.graphLimit()).isZero();
    assertThat(settings.fileByFile()).isNull();
    assertThat(settings.candidateProperties()).isEmpty();
  }

  @Test
  void allows_explicit_release_versions_and_positive_repetitions() throws IOException {
    Properties properties = new Properties();
    properties.setProperty("sonar.runtimeVersion", "26.9.0.10000");
    properties.setProperty("comparison.scannerVersion", "5.8.0.7211");
    properties.setProperty("comparison.repetitions", "1");
    var settings = ComparisonSettings.load(properties);
    assertThat(settings.serverVersion()).isEqualTo("26.9.0.10000");
    assertThat(settings.scannerVersion()).isEqualTo("5.8.0.7211");
    assertThat(settings.repetitions()).isEqualTo(1);
  }

  @Test
  void rejects_unpinned_and_blank_versions() {
    for (String key : List.of("sonar.runtimeVersion", "comparison.scannerVersion")) {
      for (String value : List.of("", " ", "LATEST", "LATEST_RELEASE", "RELEASE", "5.9-SNAPSHOT", "[5.8,6)", "5.9+")) {
        Properties properties = property(key, value);
        assertThatThrownBy(() -> ComparisonSettings.load(properties))
          .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(key).hasMessageContaining("pinned release");
      }
    }
  }

  @Test
  void rejects_invalid_repetitions() {
    for (String value : List.of("0", "-1", "three", "1.5", "", "2147483648")) {
      Properties properties = property("comparison.repetitions", value);
      assertThatThrownBy(() -> ComparisonSettings.load(properties))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("comparison.repetitions must be a positive integer");
    }
  }

  @Test
  void loads_utf8_candidate_properties_and_keeps_them_immutable() throws IOException {
    Path path = directory.resolve("candidate.properties");
    Files.writeString(path, "sonar.java.internal.newAnalyzer=true\nsonar.java.internal.example=résolution\n");
    var settings = ComparisonSettings.load(property("comparison.candidateProperties", path.toString()));
    assertThat(settings.candidateProperties()).containsOnlyKeys("sonar.java.internal.newAnalyzer", "sonar.java.internal.example")
      .containsEntry("sonar.java.internal.newAnalyzer", "true").containsEntry("sonar.java.internal.example", "résolution");
    assertThatThrownBy(() -> settings.candidateProperties().put("other", "value"))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejects_properties_that_change_inputs_reporting_or_credentials() throws IOException {
    Path path = directory.resolve("candidate.properties");
    for (String key : List.of("sonar.projectKey", "sonar.sources", "sonar.tests", "sonar.java.source", "sonar.java.jdkHome",
      "sonar.projectBaseDir", "sonar.exclusions", "sonar.scanner.skipJreProvisioning",
      "sonar.java.sourcepath", "sonar.java.test.sourcepath",
      "sonar.java.compileToByteCode", "sonar.java.fileByFile", "sonar.working.directory",
      "sonar.java.experimental.batchModeSizeInKB",
      "sonar.sourceEncoding",
      "sonar.java.internal.semantic.report.graph.maxExpansions",
      "sonar.java.binaries", "sonar.java.libraries", "sonar.java.test.binaries", "sonar.java.test.libraries",
      "sonar.java.skipUnchanged", "sonar.internal.analysis.autoscan", "sonar.internal.analysis.autoscan.filtering",
      "sonar.java.internal.semantic.report", "sonar.verbose", "sonar.log.level", "org.slf4j.simpleLogger.log.org.sonarsource",
      "sonar.scm.disabled", "style.color", "sonar.host.url", "sonar.token", "sonar.login", "sonar.password",
      "example.secret", "example.api_key", "example.credentials")) {
      Files.writeString(path, key + "=must-not-be-printed\n");
      Properties properties = property("comparison.candidateProperties", path.toString());
      assertThatThrownBy(() -> ComparisonSettings.load(properties)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(key).hasMessageNotContaining("must-not-be-printed");
    }
  }

  @Test
  void reports_missing_or_malformed_candidate_files_and_accepts_empty_files() throws IOException {
    Path path = directory.resolve("candidate.properties");
    Properties properties = property("comparison.candidateProperties", path.toString());
    assertThatThrownBy(() -> ComparisonSettings.load(properties))
      .isInstanceOf(IOException.class).hasMessageContaining(path.toString());
    Files.writeString(path, "broken=\\uXXXX\n");
    assertThatThrownBy(() -> ComparisonSettings.load(properties))
      .isInstanceOf(IOException.class).hasMessageContaining(path.toString());
    Files.writeString(path, "");
    assertThat(ComparisonSettings.load(properties).candidateProperties()).isEmpty();
    assertThatThrownBy(() -> ComparisonSettings.load(property("comparison.candidateProperties", " ")))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("properties file");
  }

  private static Properties property(String key, String value) {
    Properties properties = new Properties();
    properties.setProperty(key, value);
    return properties;
  }

  @Test
  void optional_graph_diagnostics_have_an_explicit_bounded_budget() throws IOException {
    assertThat(ComparisonSettings.load(property("comparison.graphLimit", "1000")).graphLimit()).isEqualTo(1000);
    for (String value : List.of("-1", "1001", "all", "", "1.5")) {
      assertThatThrownBy(() -> ComparisonSettings.load(property("comparison.graphLimit", value)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("comparison.graphLimit");
    }
  }

  @Test
  void exact_scanner_flag_selects_parser_scenarios_without_changing_feature_modes() throws IOException {
    Path library = Files.writeString(directory.resolve("external.jar"), "external");
    var datasets = AnalysisDataset.sonarXml(List.of(library));
    assertThat(ComparisonSettings.load(new Properties()).selectDatasets(datasets)).hasSize(4);
    for (String value : List.of("true", "false")) {
      var selected = ComparisonSettings.load(property("sonar.java.fileByFile", value)).selectDatasets(datasets);
      assertThat(selected).hasSize(2).allSatisfy(dataset -> assertThat(dataset.sharedProperties()).containsEntry("sonar.java.fileByFile", value));
      assertThat(selected.stream().map(dataset -> dataset.libraries().size())).containsExactly(0, 1);
    }
    for (String value : List.of("", "yes", "1")) {
      assertThatThrownBy(() -> ComparisonSettings.load(property("sonar.java.fileByFile", value)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sonar.java.fileByFile");
    }
  }
}
