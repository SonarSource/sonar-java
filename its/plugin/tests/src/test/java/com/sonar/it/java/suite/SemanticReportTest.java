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
import java.util.Map;
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
  void preserves_unknown_occurrences_in_normalized_immutable_deterministic_order() throws IOException {
    var first = unknownIdentifier("A", "1:0-1:1", "VARIABLE");
    var second = unknownIdentifier("B", "3:0-3:1", "MEMBER_SELECT");
    var report = SemanticReport.read(write(report(5, 2,
      fileWithDetails("src/Z.java", 3, 0), fileWithDetails("src\\\\A.java", 2, 2, second, first))),
      List.of("src/A.java", "src/Z.java"));

    assertThat(report.hasUnknownDetails()).isTrue();
    assertThat(report.unknownIdentifiers().keySet()).containsExactly("src/A.java", "src/Z.java");
    assertThat(report.unknownIdentifiers().get("src/A.java")).containsExactly(
      new SemanticReport.UnknownIdentifier("A", "1:0-1:1", "VARIABLE"),
      new SemanticReport.UnknownIdentifier("B", "3:0-3:1", "MEMBER_SELECT"));
    assertThat(report.unknownIdentifiers().get("src/Z.java")).isEmpty();
    assertThatThrownBy(() -> report.files().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> report.unknownIdentifiers().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> report.unknownIdentifiers().get("src/A.java").clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void legacy_counts_have_no_occurrence_details_and_empty_projects_have_complete_details() throws IOException {
    var legacy = SemanticReport.read(write(report(1, 0, file("A.java", 1, 0))), List.of("A.java"));
    assertThat(legacy.hasUnknownDetails()).isFalse();
    assertThat(legacy.unknownIdentifiers()).isEmpty();
    var constructed = new SemanticReport(new SemanticReport.Counts(1, 0), Map.of("A.java", new SemanticReport.Counts(1, 0)));
    assertThat(constructed.hasUnknownDetails()).isFalse();
    assertThat(SemanticReport.read(write(report(0, 0)), List.of()).hasUnknownDetails()).isTrue();
  }

  @Test
  void rejects_mismatched_and_partial_unknown_details() throws IOException {
    var identifier = unknownIdentifier("A", "1:0-1:1", "VARIABLE");
    assertInvalid(report(1, 1, fileWithDetails("A.java", 1, 1)), List.of("A.java"), "detail count");
    assertInvalid(report(1, 0, fileWithDetails("A.java", 1, 0, identifier)), List.of("A.java"), "detail count");
    assertInvalid(report(2, 1, fileWithDetails("A.java", 1, 1, identifier), file("B.java", 1, 0)),
      List.of("A.java", "B.java"), "every file or none");
    assertInvalid(report(1, 0, file("A.java", 1, 0).replace("}", ",\"unknownIdentifiers\":null}")),
      List.of("A.java"), "Invalid unknown identifier details");
  }

  @Test
  void rejects_invalid_unknown_fields_and_duplicate_occurrences() throws IOException {
    var identifier = unknownIdentifier("A", "1:0-1:1", "VARIABLE");
    for (String invalid : List.of(
      identifier.replace("\"name\":\"A\",", ""),
      identifier.replace("\"A\"", "\" \""),
      identifier.replace("\"1:0-1:1\"", "42"),
      identifier.replace("\"VARIABLE\"", "null"))) {
      assertInvalid(report(1, 1, fileWithDetails("A.java", 1, 1, invalid)), List.of("A.java"), "invalid unknown identifier string");
    }
    assertInvalid(report(2, 2, fileWithDetails("A.java", 2, 2,
      identifier, identifier.replace("VARIABLE", "MEMBER_SELECT"))), List.of("A.java"), "Duplicate unknown identifier occurrence");
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

  private static String fileWithDetails(String path, int total, int unknown, String... identifiers) {
    String file = file(path, total, unknown);
    return file.substring(0, file.length() - 1) + ",\"unknownIdentifiers\":[" + String.join(",", identifiers) + "]}";
  }

  private static String unknownIdentifier(String name, String range, String parentKind) {
    return "{\"name\":\"" + name + "\",\"range\":\"" + range + "\",\"parentKind\":\"" + parentKind + "\"}";
  }
}
