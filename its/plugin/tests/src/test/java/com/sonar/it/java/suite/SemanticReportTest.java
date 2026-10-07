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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemanticReportTest {

  @TempDir
  Path directory;

  @Test
  void derives_known_counts_and_weighted_percentage_from_exact_counts() throws IOException {
    var report = SemanticReport.read(write(report(10, 2, file("src/A.java", 2, 2), file("src/B.java", 8, 0))),
      List.of("src/A.java", "src/B.java"));

    assertThat(report.totals().known()).isEqualTo(8);
    assertThat(report.totals().unknownPercentage()).isEqualTo(20.0);
    assertThat(report.files().get("src/A.java").unknownPercentage()).isEqualTo(100.0);
    assertThat(report.files().get("src/B.java").known()).isEqualTo(8);
  }

  @Test
  void normalizes_file_paths() throws IOException {
    var report = SemanticReport.read(write(report(1, 0, file("src\\\\A.java", 1, 0))), List.of("src/A.java"));
    assertThat(report.files()).containsOnlyKeys("src/A.java");
  }

  @Test
  void zero_identifiers_have_no_percentage() throws IOException {
    var report = SemanticReport.read(write(report(0, 0, file("A.java", 0, 0))), List.of("A.java"));
    assertThat(report.totals().known()).isZero();
    assertThat(report.totals().unknownPercentage()).isNull();
    assertThat(report.files().get("A.java").unknownPercentage()).isNull();
  }

  @Test
  void rejects_missing_and_malformed_output() throws IOException {
    assertThatThrownBy(() -> SemanticReport.read(directory.resolve("missing.json"), List.of()))
      .isInstanceOf(IOException.class).hasMessageContaining("missing.json");
    assertInvalid("not JSON", List.of(), "Failed to read semantic report");
    assertInvalid("{}", List.of(), "totalNumberOfIdentifier");
    assertInvalid("{\"totalNumberOfIdentifier\":0,\"totalNumberOfUnknownIdentifier\":0}", List.of(), "Failed to read semantic report");
    assertInvalid("{\"totalNumberOfIdentifier\":1.5,\"totalNumberOfUnknownIdentifier\":0,\"files\":[]}", List.of(), "Failed to read semantic report");
    assertInvalid("{\"totalNumberOfIdentifier\":\"0\",\"totalNumberOfUnknownIdentifier\":0,\"files\":[]}", List.of(), "invalid integer");
  }

  @Test
  void rejects_invalid_counts_and_inconsistent_aggregates() throws IOException {
    assertInvalid(report(-1, 0), List.of(), "Invalid identifier counts");
    assertInvalid(report(1, -1), List.of(), "Invalid identifier counts");
    assertInvalid(report(1, 2), List.of(), "Invalid identifier counts");
    assertInvalid(report(2, 0, file("A.java", 1, 0)), List.of("A.java"), "per-file sums");
    assertInvalid(report(1, 0, file("A.java", 1, 2)), List.of("A.java"), "Invalid identifier counts");
    assertInvalid(report(1, 0, "{\"path\":\"A.java\",\"numberOfIdentifier\":1}"), List.of("A.java"), "numberOfUnknownIdentifier");
  }

  @Test
  void rejects_duplicate_missing_and_extra_files() throws IOException {
    assertInvalid(report(2, 0, file("A.java", 1, 0), file("A.java", 1, 0)), List.of("A.java"), "Duplicate file");
    assertInvalid(report(1, 0, file("A.java", 1, 0)), List.of("A.java", "B.java"), "missing=[B.java]");
    assertInvalid(report(1, 0, file("A.java", 1, 0)), List.of(), "extra=[A.java]");
  }

  private void assertInvalid(String json, List<String> files, String message) throws IOException {
    Path path = write(json);
    assertThatThrownBy(() -> SemanticReport.read(path, files)).isInstanceOf(IOException.class).hasMessageContaining(message);
  }

  private Path write(String json) throws IOException {
    Path path = directory.resolve("semantic.json");
    Files.writeString(path, json);
    return path;
  }

  private static String report(int total, int unknown, String... files) {
    return "{\"totalNumberOfIdentifier\":" + total + ",\"totalNumberOfUnknownIdentifier\":" + unknown
      + ",\"globalPercentageOfUnknownIdentifier\":999,\"files\":[" + String.join(",", files) + "]}";
  }

  private static String file(String path, int total, int unknown) {
    return "{\"path\":\"" + path + "\",\"numberOfIdentifier\":" + total + ",\"numberOfUnknownIdentifier\":" + unknown + "}";
  }
}
