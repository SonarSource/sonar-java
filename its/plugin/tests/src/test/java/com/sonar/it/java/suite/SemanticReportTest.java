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

  @Test
  void reads_module_identifiers_and_semantic_graph_counts_without_treating_unknown_graph_locations_as_coverage() throws IOException {
    var firstGraph = new SemanticReport.GraphCounts(1, 3, 2, 4);
    var secondGraph = new SemanticReport.GraphCounts(2, 0, 6, 0);
    var totalGraph = new SemanticReport.GraphCounts(3, 3, 8, 4);
    String first = module("z", 2, 1, firstGraph, fileWithDetails("src/A.java", 3, 1,
      unknownIdentifier("Missing", "1:0-1:7", "VARIABLE")));
    String second = module("a", 3, 0, secondGraph, fileWithDetails("src/B.java", 3, 0));
    var report = SemanticReport.read(write(canonicalReport(5, 1, totalGraph, first, second)), List.of("z/src/A.java", "a/src/B.java"));

    assertThat(report.totals()).isEqualTo(new SemanticReport.Counts(6, 1));
    assertThat(report.graphTotals()).isEqualTo(totalGraph);
    assertThat(report.moduleGraphCounts()).containsEntry("a", secondGraph).containsEntry("z", firstGraph);
    assertThat(report.unknownSymbols()).containsExactly("z:(Owner).symbols[0]", "z:(Owner).symbols[1]", "z:(Owner).symbols[2]");
    assertThat(report.unknownTypes()).hasSize(4).startsWith("z:(Owner).types[0]");
    assertThat(report.unknownIdentifiers()).containsOnlyKeys("a/src/B.java", "z/src/A.java");
    assertThat(report.hasUnknownDetails()).isTrue();
    assertThatThrownBy(() -> report.moduleGraphCounts().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> report.unknownSymbols().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> report.unknownTypes().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void normalizes_module_paths_and_qualifies_identical_graph_locations_from_different_modules() throws IOException {
    var graph = new SemanticReport.GraphCounts(1, 1, 1, 1);
    var totals = new SemanticReport.GraphCounts(2, 2, 2, 2);
    String first = module("./one/", 1, 0, graph, file("src/../src/A.java", 1, 0));
    String second = module("two", 1, 0, graph, file("src/B.java", 1, 0));
    var report = SemanticReport.read(write(canonicalReport(2, 0, totals, first, second)), List.of("one/src/A.java", "two/src/B.java"));

    assertThat(report.files()).containsOnlyKeys("one/src/A.java", "two/src/B.java");
    assertThat(report.unknownSymbols()).containsExactly("one:(Owner).symbols[0]", "two:(Owner).symbols[0]");
    assertThat(report.moduleGraphCounts()).containsOnlyKeys("one", "two");
    assertThat(report.hasUnknownDetails()).isFalse();
  }

  @Test
  void rejects_duplicate_normalized_module_and_file_paths() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    assertInvalid(canonicalReport(0, 0, graph, module(".", 0, 0, graph), module("./", 0, 0, graph)), List.of(), "Duplicate module: .");
    assertInvalid(canonicalReport(2, 0, graph, module(".", 2, 0, graph, file("src/A.java", 1, 0), file("src/../src/A.java", 1, 0))),
      List.of("src/A.java"), "Duplicate file: src/A.java");
    assertInvalid(canonicalReport(2, 0, graph, module(".", 1, 0, graph, file("a/src/A.java", 1, 0)),
      module("a", 1, 0, graph, file("src/A.java", 1, 0))), List.of("a/src/A.java"), "Duplicate file: a/src/A.java");
  }

  @Test
  void rejects_canonical_identifier_counts_inconsistent_with_modules_or_per_file_observations() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    assertInvalid(canonicalReport(2, 0, graph, module(".", 1, 0, graph, file("A.java", 1, 0))), List.of("A.java"), "Aggregate module identifier counts");
    assertInvalid(canonicalReport(2, 0, graph, module(".", 2, 0, graph, file("A.java", 1, 0))), List.of("A.java"), "Module . identifier counts");
    assertInvalid(canonicalReport(-1, 0, graph), List.of(), "Invalid identifier counts");
    assertInvalid(canonicalReport(1, -1, graph), List.of(), "Invalid identifier counts");
    assertInvalid(canonicalReport(Integer.MAX_VALUE, 1, graph), List.of(), "integer overflow");
    assertInvalid(canonicalReport(1, 0, graph, module(".", 1, 0, graph, file("A.java", 1, 0)))
      .replaceFirst("\"resolvedIdentifierCount\":1", "\"resolvedIdentifierCount\":1.5"), List.of("A.java"), "Failed to read semantic report");
  }

  @Test
  void rejects_graph_totals_inconsistent_with_module_sums_and_arrays() throws IOException {
    var graph = new SemanticReport.GraphCounts(1, 1, 1, 1);
    var different = new SemanticReport.GraphCounts(2, 1, 1, 1);
    String module = module(".", 1, 0, graph, file("A.java", 1, 0));
    assertInvalid(canonicalReport(1, 0, different, module), List.of("A.java"), "Aggregate semantic graph counts");
    assertInvalid(canonicalReport(1, 0, graph, module.replace("\"unknownSymbols\":[\"(Owner).symbols[0]\"]", "\"unknownSymbols\":[]")),
      List.of("A.java"), "unknownSymbols detail count");
    assertInvalid(canonicalReport(1, 0, graph, module.replace("\"unknownTypes\":[\"(Owner).types[0]\"]", "\"unknownTypes\":[]")),
      List.of("A.java"), "unknownTypes detail count");
    assertInvalid(canonicalReport(1, 0, graph, module.replace("\"(Owner).symbols[0]\"", "null")), List.of("A.java"), "invalid semantic graph location");
    assertInvalid(canonicalReport(1, 0, graph, module.replace("\"unknownSymbols\":[", "\"unknownSymbols\":[\"(Owner).symbols[0]\",")),
      List.of("A.java"), "Duplicate semantic graph location");
    assertInvalid(canonicalReport(1, 0, graph, module).replaceFirst("\"resolvedSymbolCount\":1", "\"resolvedSymbolCount\":-1"),
      List.of("A.java"), "Invalid semantic graph counts");
  }

  @Test
  void accepts_canonical_modules_without_optional_per_file_observations() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String populated = withoutFiles(module(".", 1, 0, graph, file("A.java", 1, 0)));
    var report = SemanticReport.read(write(canonicalReport(1, 0, graph, populated)), List.of("A.java"));
    assertThat(report.totals()).isEqualTo(new SemanticReport.Counts(1, 0));
    assertThat(report.moduleIdentifierCounts()).containsEntry(".", new SemanticReport.Counts(1, 0));
    assertThat(report.files()).isEmpty();
    assertThat(report.hasFileObservations()).isFalse();
    assertThat(report.hasUnknownDetails()).isFalse();
    var empty = SemanticReport.read(write(canonicalReport(0, 0, graph, withoutFiles(module(".", 0, 0, graph)))), List.of());
    assertThat(empty.files()).isEmpty();
    assertThat(empty.graphTotals()).isEqualTo(graph);
    assertThat(empty.hasFileObservations()).isFalse();
    assertThat(empty.hasUnknownDetails()).isFalse();
  }

  @Test
  void canonical_unknown_identifier_list_must_match_module_count_and_observed_file_coverage() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String module = module(".", 0, 1, graph, fileWithDetails("A.java", 1, 1, unknownIdentifier("Missing", "1:0-1:7", "VARIABLE")));
    assertInvalid(canonicalReport(0, 1, graph, module.replace("\"unknownIdentifiers\":[\"(Owner).identifiers[0]\"]", "\"unknownIdentifiers\":[]")),
      List.of("A.java"), "unknownIdentifiers detail count");
    assertInvalid(canonicalReport(0, 1, graph, module), List.of("A.java", "B.java"), "missing=[B.java]");
    assertInvalid(canonicalReport(0, 1, graph, module), List.of(), "extra=[A.java]");
  }

  @Test
  void legacy_constructors_expose_absent_graph_metrics_without_inventing_zero_counts() throws IOException {
    var legacy = SemanticReport.read(write(report(1, 0, file("A.java", 1, 0))), List.of("A.java"));
    assertThat(legacy.graphTotals()).isNull();
    assertThat(legacy.moduleGraphCounts()).isEmpty();
    assertThat(legacy.unknownSymbols()).isEmpty();
    assertThat(legacy.unknownTypes()).isEmpty();
    assertThat(new SemanticReport(new SemanticReport.Counts(1, 0), Map.of("A.java", new SemanticReport.Counts(1, 0)), Map.of()).graphTotals()).isNull();
  }

  @Test
  void preserves_complete_primary_identifier_counts_when_recursive_graph_traversal_is_partial() throws IOException {
    var graph = new SemanticReport.GraphCounts(20, 3, 10, 4);
    String module = withTraversal(module(".", 8, 2, graph, fileWithDetails("A.java", 10, 2,
      unknownIdentifier("First", "1:0-1:5", "VARIABLE"), unknownIdentifier("Second", "2:0-2:6", "VARIABLE"))), false, 1000, 1000);
    var report = SemanticReport.read(write(withTraversal(canonicalReport(8, 2, graph, module), false, 1000, 1000)), List.of("A.java"));

    assertThat(report.totals()).isEqualTo(new SemanticReport.Counts(10, 2));
    assertThat(report.files()).containsEntry("A.java", new SemanticReport.Counts(10, 2));
    assertThat(report.unknownIdentifiers().get("A.java")).hasSize(2);
    assertThat(report.hasUnknownDetails()).isTrue();
    assertThat(report.graphTotals()).isEqualTo(graph);
    assertThat(report.graphTraversal()).isEqualTo(new SemanticReport.GraphTraversal(false, 1000, 1000));
  }

  @Test
  void validates_per_module_caps_while_global_expansions_can_exceed_a_single_module_cap() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String first = withTraversal(module("one", 1, 0, graph, file("A.java", 1, 0)), true, 10, 8);
    String second = withTraversal(module("two", 1, 0, graph, file("A.java", 1, 0)), true, 10, 9);
    var report = SemanticReport.read(write(withTraversal(canonicalReport(2, 0, graph, first, second), true, 10, 17)),
      List.of("one/A.java", "two/A.java"));

    assertThat(report.graphTraversal()).isEqualTo(new SemanticReport.GraphTraversal(true, 10, 17));
    assertThat(report.files()).hasSize(2);
  }

  @Test
  void rejects_inconsistent_graph_traversal_completeness_expansion_sums_and_module_caps() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String complete = withTraversal(module(".", 1, 0, graph, file("A.java", 1, 0)), true, 10, 8);
    String partial = withTraversal(module(".", 1, 0, graph, file("A.java", 1, 0)), false, 10, 10);
    assertInvalid(withTraversal(canonicalReport(1, 0, graph, complete), false, 10, 8), List.of("A.java"), "does not match module observations");
    assertInvalid(withTraversal(canonicalReport(1, 0, graph, partial), true, 10, 10), List.of("A.java"), "does not match module observations");
    assertInvalid(withTraversal(canonicalReport(1, 0, graph, complete), true, 10, 7), List.of("A.java"), "does not match module observations");
    assertInvalid(withTraversal(canonicalReport(1, 0, graph, complete), true, 9, 8), List.of("A.java"), "module graph expansion budget");
    String overBudget = withTraversal(module(".", 1, 0, graph, file("A.java", 1, 0)), false, 10, 11);
    assertInvalid(withTraversal(canonicalReport(1, 0, graph, overBudget), false, 10, 11), List.of("A.java"), "module graph expansion budget");
  }

  @Test
  void rejects_partial_or_malformed_graph_traversal_metadata() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String module = withTraversal(module(".", 1, 0, graph, file("A.java", 1, 0)), true, 10, 8);
    String json = withTraversal(canonicalReport(1, 0, graph, module), true, 10, 8);
    assertInvalid(json.replaceFirst("\"graphTraversalComplete\":true", "\"graphTraversalComplete\":\"true\""), List.of("A.java"), "invalid graphTraversalComplete boolean");
    assertInvalid(json.replaceFirst("\"graphTraversalComplete\":true,", ""), List.of("A.java"), "invalid graphTraversalComplete boolean");
    assertInvalid(json.replaceFirst("\"graphExpansionLimit\":10,", ""), List.of("A.java"), "graphExpansionLimit");
    assertInvalid(json.replaceFirst("\"graphExpansions\":8,", ""), List.of("A.java"), "graphExpansions");
    assertInvalid(json.replaceFirst("\"graphExpansionLimit\":10", "\"graphExpansionLimit\":-1"), List.of("A.java"), "Invalid graph traversal counts");
    assertInvalid(json.replaceFirst("\"graphExpansions\":8", "\"graphExpansions\":-1"), List.of("A.java"), "Invalid graph traversal counts");
  }

  @Test
  void graph_traversal_metadata_must_cover_both_the_report_and_every_module() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String module = module(".", 1, 0, graph, file("A.java", 1, 0));
    assertInvalid(withTraversal(canonicalReport(1, 0, graph, module), true, 10, 8), List.of("A.java"), "every module or none");
    assertInvalid(canonicalReport(1, 0, graph, withTraversal(module, true, 10, 8)), List.of("A.java"), "every module or none");
    var unbounded = SemanticReport.read(write(canonicalReport(1, 0, graph, module)), List.of("A.java"));
    assertThat(unbounded.graphTraversal()).isNull();
    assertThat(SemanticReport.read(write(report(1, 0, file("A.java", 1, 0))), List.of("A.java")).graphTraversal()).isNull();
    assertThat(new SemanticReport(new SemanticReport.Counts(1, 0), Map.of("A.java", new SemanticReport.Counts(1, 0)), Map.of(),
      graph, Map.of(".", graph), List.of(), List.of()).graphTraversal()).isNull();
  }

  @Test
  void reads_latest_canonical_format_with_empty_aggregators_and_large_real_graph_counts() throws IOException {
    String json = """
      {
        "resolvedIdentifierCount": 14,
        "unknownIdentifierCount": 0,
        "percentageOfUnknownIdentifier": 0.000,
        "resolvedSymbolCount": 949094,
        "unknownSymbolCount": 2,
        "resolvedTypeCount": 163000,
        "unknownTypeCount": 0,
        "modules": [
          {
            "path": ".",
            "resolvedIdentifierCount": 0,
            "unknownIdentifierCount": 0,
            "resolvedSymbolCount": 0,
            "unknownSymbolCount": 0,
            "resolvedTypeCount": 0,
            "unknownTypeCount": 0,
            "unknownIdentifiers": [],
            "unknownSymbols": [],
            "unknownTypes": []
          },
          {
            "path": "its",
            "resolvedIdentifierCount": 0,
            "unknownIdentifierCount": 0,
            "resolvedSymbolCount": 0,
            "unknownSymbolCount": 0,
            "resolvedTypeCount": 0,
            "unknownTypeCount": 0,
            "unknownIdentifiers": [],
            "unknownSymbols": [],
            "unknownTypes": []
          },
          {
            "path": "java-frontend",
            "resolvedIdentifierCount": 14,
            "unknownIdentifierCount": 0,
            "percentageOfUnknownIdentifier": 0.000,
            "resolvedSymbolCount": 949094,
            "unknownSymbolCount": 2,
            "resolvedTypeCount": 163000,
            "unknownTypeCount": 0,
            "unknownIdentifiers": [],
            "unknownSymbols": ["(Owner).first", "(Owner).second"],
            "unknownTypes": []
          }
        ]
      }
      """;
    var report = SemanticReport.read(write(json), List.of("java-frontend/src/main/java/Example.java"));

    assertThat(report.totals()).isEqualTo(new SemanticReport.Counts(14, 0));
    assertThat(report.moduleIdentifierCounts()).containsEntry(".", new SemanticReport.Counts(0, 0))
      .containsEntry("its", new SemanticReport.Counts(0, 0)).containsEntry("java-frontend", new SemanticReport.Counts(14, 0));
    assertThat(report.graphTotals()).isEqualTo(new SemanticReport.GraphCounts(949094, 2, 163000, 0));
    assertThat(report.graphTraversal()).isNull();
    assertThat(report.files()).isEmpty();
    assertThat(report.hasFileObservations()).isFalse();
    assertThat(report.hasUnknownDetails()).isFalse();
    assertThat(report.unknownSymbols()).containsExactly("java-frontend:(Owner).first", "java-frontend:(Owner).second");
    assertThatThrownBy(() -> report.moduleIdentifierCounts().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void preserves_unknown_identifier_totals_without_fabricating_per_file_counts() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String module = withoutFiles(module("main", 3, 2, graph));
    var report = SemanticReport.read(write(canonicalReport(3, 2, graph, module)), List.of("main/A.java", "main/B.java"));

    assertThat(report.totals()).isEqualTo(new SemanticReport.Counts(5, 2));
    assertThat(report.totals().unknownPercentage()).isEqualTo(40.0);
    assertThat(report.moduleIdentifierCounts()).containsOnlyKeys("main").containsEntry("main", new SemanticReport.Counts(5, 2));
    assertThat(report.files()).isEmpty();
    assertThat(report.unknownIdentifiers()).isEmpty();
    assertThat(report.hasFileObservations()).isFalse();
  }

  @Test
  void observations_cover_every_populated_module_while_empty_aggregators_can_omit_them() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String populated = module("main", 1, 0, graph, fileWithDetails("src/A.java", 1, 0));
    String empty = withoutFiles(module(".", 0, 0, graph));
    var report = SemanticReport.read(write(canonicalReport(1, 0, graph, empty, populated)), List.of("main/src/A.java"));
    assertThat(report.hasFileObservations()).isTrue();
    assertThat(report.hasUnknownDetails()).isTrue();
    assertThat(report.moduleIdentifierCounts()).containsOnlyKeys(".", "main");
    assertThat(report.files()).containsOnlyKeys("main/src/A.java");
    String unobserved = withoutFiles(module("other", 1, 0, graph));
    assertInvalid(canonicalReport(2, 0, graph, populated, unobserved), List.of("main/src/A.java", "other/src/B.java"), "every populated module or none");
  }

  @Test
  void canonical_module_totals_remain_strict_without_file_observations() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String module = withoutFiles(module("main", 1, 0, graph));
    assertInvalid(canonicalReport(2, 0, graph, module), List.of("main/A.java"), "Aggregate module identifier counts");
    assertInvalid(canonicalReport(2, 0, graph, module, module), List.of("main/A.java"), "Duplicate module");
    assertInvalid(canonicalReport(1, 0, new SemanticReport.GraphCounts(1, 0, 0, 0), module), List.of("main/A.java"), "Aggregate semantic graph counts");
  }

  @Test
  void missing_zero_count_graph_arrays_are_optional_but_nonzero_details_are_required() throws IOException {
    var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
    String module = withoutFiles(module("main", 1, 0, graph)).replace(",\"unknownIdentifiers\":[]", "")
      .replace(",\"unknownSymbols\":[]", "").replace(",\"unknownTypes\":[]", "");
    var report = SemanticReport.read(write(canonicalReport(1, 0, graph, module)), List.of("main/A.java"));
    assertThat(report.moduleIdentifierCounts()).containsEntry("main", new SemanticReport.Counts(1, 0));
    assertThat(report.unknownSymbols()).isEmpty();
    assertThat(report.unknownTypes()).isEmpty();
    var unknownGraph = new SemanticReport.GraphCounts(0, 1, 0, 0);
    String missing = withoutFiles(module("main", 1, 0, unknownGraph)).replace(",\"unknownSymbols\":[\"(Owner).symbols[0]\"]", "");
    assertInvalid(canonicalReport(1, 0, unknownGraph, missing), List.of("main/A.java"), "invalid semantic graph array: unknownSymbols");
  }

  @Test
  void existing_constructors_preserve_observation_availability_and_module_maps_are_defensively_copied() {
    var counts = new SemanticReport.Counts(1, 0);
    var legacy = new SemanticReport(counts, Map.of("A.java", counts));
    assertThat(legacy.hasFileObservations()).isTrue();
    assertThat(legacy.moduleIdentifierCounts()).isEmpty();
    var moduleCounts = new java.util.HashMap<String, SemanticReport.Counts>();
    moduleCounts.put("main", counts);
    var canonical = new SemanticReport(counts, Map.of(), Map.of(), null, Map.of(), List.of(), List.of(), null, moduleCounts);
    moduleCounts.clear();
    assertThat(canonical.moduleIdentifierCounts()).containsEntry("main", counts);
    assertThat(canonical.hasFileObservations()).isFalse();
    assertThat(canonical.hasUnknownDetails()).isFalse();
  }

  private static String withoutFiles(String module) {
    return module.substring(0, module.indexOf(",\"files\":")) + "}";
  }

  private static String withTraversal(String json, boolean complete, int limit, int expansions) {
    return "{\"graphTraversalComplete\":" + complete + ",\"graphExpansionLimit\":" + limit + ",\"graphExpansions\":" + expansions + "," + json.substring(1);
  }

  private static String canonicalReport(int resolved, int unknown, SemanticReport.GraphCounts graph, String... modules) {
    return "{\"resolvedIdentifierCount\":" + resolved + ",\"unknownIdentifierCount\":" + unknown + "," + graphFields(graph)
      + ",\"modules\":[" + String.join(",", modules) + "]}";
  }

  private static String module(String path, int resolved, int unknown, SemanticReport.GraphCounts graph, String... files) {
    return "{\"path\":\"" + path + "\",\"resolvedIdentifierCount\":" + resolved + ",\"unknownIdentifierCount\":" + unknown + "," + graphFields(graph)
      + ",\"unknownIdentifiers\":[" + locations("identifiers", unknown) + "],\"unknownSymbols\":[" + locations("symbols", graph.unknownSymbols())
      + "],\"unknownTypes\":[" + locations("types", graph.unknownTypes()) + "],\"files\":[" + String.join(",", files) + "]}";
  }

  private static String graphFields(SemanticReport.GraphCounts graph) {
    return "\"resolvedSymbolCount\":" + graph.resolvedSymbols() + ",\"unknownSymbolCount\":" + graph.unknownSymbols()
      + ",\"resolvedTypeCount\":" + graph.resolvedTypes() + ",\"unknownTypeCount\":" + graph.unknownTypes();
  }

  private static String locations(String kind, int count) {
    return String.join(",", java.util.stream.IntStream.range(0, count).mapToObj(index -> "\"(Owner)." + kind + "[" + index + "]\"").toList());
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
