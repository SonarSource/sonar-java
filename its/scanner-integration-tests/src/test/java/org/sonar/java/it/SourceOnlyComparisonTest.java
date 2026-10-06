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
package org.sonar.java.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class SourceOnlyComparisonTest {

  @Test
  void matches_by_rule_path_and_line_while_preserving_duplicates() {
    var first = finding("java:S1116", "src\\Example.java", 3, "first");
    var second = finding("java:S1116", "src/Example.java", 3, "second");
    var added = finding("java:S1874", "src/Example.java", 8, "added");
    var comparison = SourceOnlyComparison.compare(run(List.of(first, second)), run(List.of(first, added)), false,
      List.of("java:S1116", "java:S1874", "java:S1206"));

    assertThat(comparison.valid()).isTrue();
    assertThat(comparison.currentOnly()).containsExactly(second);
    assertThat(comparison.candidateOnly()).containsExactly(added);
    assertThat(comparison.rules()).containsExactly(
      new SourceOnlyComparison.RuleResult("java:S1116", 2, 1, 1, 1, 0, 0.5),
      new SourceOnlyComparison.RuleResult("java:S1206", 0, 0, 0, 0, 0, null),
      new SourceOnlyComparison.RuleResult("java:S1874", 0, 1, 0, 0, 1, null));
  }

  @Test
  void changed_messages_match_but_changed_lines_and_rules_do_not() {
    var before = run(List.of(finding("java:S1116", "src/Example.java", 3, "before")));
    var after = run(List.of(finding("java:S1116", "src/Example.java", 3, "after")));
    var comparison = SourceOnlyComparison.compare(before, after, true, List.of());
    assertThat(comparison.currentOnly()).isEmpty();
    assertThat(comparison.candidateOnly()).isEmpty();

    var moved = run(List.of(finding("java:S1116", "src/Example.java", 4, "before")));
    assertThat(SourceOnlyComparison.compare(before, moved, false, List.of()).currentOnly()).hasSize(1);
    var otherRule = run(List.of(finding("java:S1874", "src/Example.java", 3, "before")));
    assertThat(SourceOnlyComparison.compare(before, otherRule, false, List.of()).candidateOnly()).hasSize(1);
  }

  @Test
  void matches_file_level_findings_and_reports_empty_results_without_dividing_by_zero() {
    var fileIssue = finding("java:S1206", "src/Example.java", null, "file issue");
    var matching = SourceOnlyComparison.compare(run(List.of(fileIssue)), run(List.of(fileIssue)), false, List.of());
    assertThat(matching.currentOnly()).isEmpty();
    assertThat(matching.rules().getFirst().retention()).isEqualTo(1.0);

    var empty = SourceOnlyComparison.compare(run(List.of()), run(List.of()), true, List.of("java:S1116"));
    assertThat(empty.rules().getFirst().retention()).isNull();
    assertThat(SourceOnlyComparison.markdown(empty)).contains("placeholder", "no findings", "N/A");
  }

  @Test
  void failed_scans_and_different_file_coverage_have_no_agreement_metrics() {
    var failed = new SourceOnlyComparison.Run("candidate", false, 12, List.of(), List.of(), Map.of(), null, "Scanner exited with code 1");
    var comparison = SourceOnlyComparison.compare(run(List.of()), failed, false, List.of());
    assertThat(comparison.valid()).isFalse();
    assertThat(comparison.rules()).isEmpty();
    assertThat(SourceOnlyComparison.markdown(comparison)).contains("FAILED", "Scanner exited with code 1", "Comparison unavailable");

    var differentFiles = new SourceOnlyComparison.Run("candidate", true, 12, List.of("Other.java"), List.of(), Map.of(), null, null);
    assertThat(SourceOnlyComparison.compare(run(List.of()), differentFiles, false, List.of()).valid()).isFalse();
  }

  @Test
  void allocates_numbered_runs_without_overwriting_previous_results(@TempDir Path directory) throws IOException {
    Path first = SourceOnlyComparison.createRunDirectory(directory);
    Files.writeString(first.resolve("report.md"), "previous results");
    assertThat(first.getFileName()).hasToString("run-001");
    assertThat(SourceOnlyComparison.createRunDirectory(directory).getFileName()).hasToString("run-002");

    Files.createDirectory(directory.resolve("run-005"));
    Files.writeString(directory.resolve("README.md"), "unrelated file");
    assertThat(SourceOnlyComparison.createRunDirectory(directory).getFileName()).hasToString("run-006");
    assertThat(Files.readString(first.resolve("report.md"))).isEqualTo("previous results");
  }

  @Test
  void writes_only_a_report_with_summary_before_rule_details(@TempDir Path directory) throws IOException {
    var lost = finding("java:S1116", "src/Example.java", 3, "lost | finding\nwith detail");
    var comparison = SourceOnlyComparison.compare(run(List.of(lost)), run(List.of()), false, List.of());
    SourceOnlyComparison.write(directory, comparison);

    try (var files = Files.list(directory)) {
      assertThat(files.map(path -> path.getFileName().toString()).toList()).containsExactly("report.md");
    }
    String report = Files.readString(directory.resolve("report.md"));
    assertThat(report).contains("| Findings | 1 | 0 |", "| Scan time (ms) | 12 | 12 |", "| Current-only findings | 1 |",
      "java:S1116", "0.0%", "src/Example.java", "lost \\| finding with detail", "not accuracy");
    assertThat(report.indexOf("## Summary")).isLessThan(report.indexOf("## Rules with findings"));
  }

  @Test
  void summarizes_zero_finding_rules_and_reports_missing_telemetry_as_unavailable() {
    var current = new SourceOnlyComparison.Run("current", true, 12, List.of("src/Example.java"), List.of(),
      Map.of("java.analysis.main.success.type_error_count", "390"), semantics(10, 3), null);
    var comparison = SourceOnlyComparison.compare(current, run(List.of()), false, List.of("java:S1116", "java:S1206"));
    String report = SourceOnlyComparison.markdown(comparison);
    assertThat(report)
      .contains("| Rules compared | 2 |", "2 rules had no findings", "| Undefined-type errors | 390 | N/A |")
      .doesNotContain("| java:S1116 |", "| java:S1206 |", "diff.json", "current.log");
  }

  @Test
  void compares_semantics_globally_and_per_file() {
    var current = new SourceOnlyComparison.Run("current", true, 12, List.of("src/Example.java"), List.of(), Map.of(), semantics(10, 4), null);
    var candidate = new SourceOnlyComparison.Run("candidate", true, 12, List.of("src/Example.java"), List.of(), Map.of(), semantics(10, 1), null);
    String report = SourceOnlyComparison.markdown(SourceOnlyComparison.compare(current, candidate, false, List.of()));

    assertThat(report).contains("| Identifiers (total) | 10 | 10 |", "| Known identifiers | 6 | 9 |", "| Unknown identifiers | 4 | 1 |",
      "| Unknown identifiers (%) | 40.000% | 10.000% |", "| Change in unknown identifiers (percentage points) | -30.000 |",
      "| src/Example.java | 10 / 10 | 6 / 9 | 4 / 1 | 40.000% / 10.000% | -30.000 |");
    assertThat(report.indexOf("## Semantics per file")).isLessThan(report.indexOf("## Rules with findings"));
  }

  @Test
  void missing_semantics_invalidates_comparison_and_empty_semantics_has_no_percentage() {
    var missing = new SourceOnlyComparison.Run("candidate", true, 12, List.of("src/Example.java"), List.of(), Map.of(), null, null);
    assertThat(SourceOnlyComparison.compare(run(List.of()), missing, false, List.of()).valid()).isFalse();

    var empty = new SourceOnlyComparison.Run("scan", true, 12, List.of("src/Example.java"), List.of(), Map.of(), semantics(0, 0), null);
    String report = SourceOnlyComparison.markdown(SourceOnlyComparison.compare(empty, empty, true, List.of()));
    assertThat(report).contains("| Known identifiers | 0 | 0 |", "| Unknown identifiers (%) | N/A | N/A |",
      "| Change in unknown identifiers (percentage points) | N/A |", "| Files with no unknown identifiers | 0 / 1 (0.000%) | 0 / 1 (0.000%) |",
      "| Files without comparable identifier percentages | 1 |");
  }

  @Test
  void summarizes_file_progress_and_largest_unknown_contributors() {
    var current = semanticRun(Map.of("A.java", new SemanticReport.Counts(10, 4), "B.java", new SemanticReport.Counts(10, 0),
      "C.java", new SemanticReport.Counts(5, 1), "D.java", new SemanticReport.Counts(0, 0)));
    var candidate = semanticRun(Map.of("A.java", new SemanticReport.Counts(10, 1), "B.java", new SemanticReport.Counts(10, 2),
      "C.java", new SemanticReport.Counts(5, 1), "D.java", new SemanticReport.Counts(0, 0)));
    String report = SourceOnlyComparison.markdown(SourceOnlyComparison.compare(current, candidate, false, List.of()));

    assertThat(report).contains("| Files with no unknown identifiers | 1 / 4 (25.000%) | 0 / 4 (0.000%) |",
      "| Net change in unknown identifier count | -1 |", "| Files improved (unknown %) | 1 |", "| Files unchanged (unknown %) | 1 |",
      "| Files regressed (unknown %) | 1 |", "| Files without comparable identifier percentages | 1 |",
      "| A.java | 4 / 1 | 80.000% / 25.000% |", "| B.java | 0 / 2 | 0.000% / 50.000% |");
    assertThat(report.indexOf("## Top files contributing unknown identifiers")).isLessThan(report.indexOf("## Semantics per file"));
  }

  @Test
  void limits_top_contributors_to_five_and_handles_no_unknowns() {
    var current = semanticRun(Map.of("A.java", new SemanticReport.Counts(10, 1), "B.java", new SemanticReport.Counts(10, 2),
      "C.java", new SemanticReport.Counts(10, 3), "D.java", new SemanticReport.Counts(10, 4),
      "E.java", new SemanticReport.Counts(10, 5), "F.java", new SemanticReport.Counts(10, 6)));
    var knownFiles = new TreeMap<String, SemanticReport.Counts>();
    current.files().forEach(path -> knownFiles.put(path, new SemanticReport.Counts(10, 0)));
    var candidate = semanticRun(knownFiles);
    String report = SourceOnlyComparison.markdown(SourceOnlyComparison.compare(current, candidate, false, List.of()));
    String contributors = report.substring(report.indexOf("## Top files contributing unknown identifiers"), report.indexOf("## Semantics per file"));

    assertThat(contributors).contains("| F.java | 6 / 0 | 28.571% / N/A |", "| B.java | 2 / 0 |").doesNotContain("| A.java |");
    assertThat(contributors.indexOf("| F.java |")).isLessThan(contributors.indexOf("| E.java |"));
    String allKnown = SourceOnlyComparison.markdown(SourceOnlyComparison.compare(candidate, candidate, true, List.of()));
    assertThat(allKnown).contains("| Files with no unknown identifiers | 6 / 6 (100.000%) | 6 / 6 (100.000%) |",
      "Values are **current / candidate**.\n\nNone.");
  }

  @Test
  void semantic_report_paths_are_distinct_and_default_to_temporary_project(@TempDir Path directory) {
    String key = "sonar.java.internal.semantic.report";
    String previous = System.getProperty(key);
    try {
      System.setProperty(key, directory.resolve("report.json").toString());
      assertThat(NoCompilationComparisonTest.semanticReportPath(directory, "current")).isEqualTo(directory.resolve("report-current.json"));
      assertThat(NoCompilationComparisonTest.semanticReportPath(directory, "candidate")).isEqualTo(directory.resolve("report-candidate.json"));
      assertThat(NoCompilationComparisonTest.semanticReportPath(directory, "smoke")).isEqualTo(directory.resolve("semantic-report.json"));
      System.clearProperty(key);
      assertThat(NoCompilationComparisonTest.semanticReportPath(directory, "current")).isEqualTo(directory.resolve("semantic-report.json"));
    } finally {
      if (previous == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, previous);
      }
    }
  }

  private static SourceOnlyComparison.Finding finding(String rule, String path, Integer line, String message) {
    return new SourceOnlyComparison.Finding(rule, path, line, message);
  }

  private static SourceOnlyComparison.Run run(List<SourceOnlyComparison.Finding> findings) {
    return new SourceOnlyComparison.Run("scan", true, 12, List.of("src/Example.java"), findings, Map.of(), semantics(10, 3), null);
  }

  private static SemanticReport semantics(int total, int unknown) {
    var counts = new SemanticReport.Counts(total, unknown);
    return new SemanticReport(counts, Map.of("src/Example.java", counts));
  }

  private static SourceOnlyComparison.Run semanticRun(Map<String, SemanticReport.Counts> files) {
    var totals = new SemanticReport.Counts(files.values().stream().mapToInt(SemanticReport.Counts::total).sum(),
      files.values().stream().mapToInt(SemanticReport.Counts::unknown).sum());
    return new SourceOnlyComparison.Run("scan", true, 12, files.keySet().stream().sorted().toList(), List.of(), Map.of(),
      new SemanticReport(totals, files), null);
  }
}
