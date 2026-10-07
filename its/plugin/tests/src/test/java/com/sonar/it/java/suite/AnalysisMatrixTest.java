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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AnalysisMatrixTest {

  @Test
  void summarizes_all_modes_with_weighted_project_percentages_and_per_file_counts() {
    var runs = runs();
    runs.put(AnalysisMode.BASELINE, run(files(100, 1), Map.of(), List.of()));
    runs.put(AnalysisMode.SOURCE_PATHS, run(files(20, 1), Map.of(), List.of()));
    runs.put(AnalysisMode.BYTECODE, run(files(5, 0), Map.of(), List.of()));
    runs.put(AnalysisMode.COMBINED, run(files(2, 0), Map.of(), List.of()));

    String report = report(runs);

    assertThat(report).contains("| Metric | Baseline | Source paths | Bytecode | Combined |",
      "| Source paths enabled | NO | YES | NO | YES |",
      "| Internal compilation enabled | NO | NO | YES | YES |",
      "| Identifiers (total) | 1001 | 1001 | 1001 | 1001 |",
      "| Known identifiers | 900 | 980 | 996 | 999 |",
      "| Unknown identifiers (%) | 10.090% | 2.098% | 0.500% | 0.200% |",
      "| A.java | 1000 / 900 / 100 (10.000%) | 1000 / 980 / 20 (2.000%) | 1000 / 995 / 5 (0.500%) | 1000 / 998 / 2 (0.200%) |",
      "| B.java | 1 / 0 / 1 (100.000%) | 1 / 0 / 1 (100.000%) | 1 / 1 / 0 (0.000%) | 1 / 1 / 0 (0.000%) |");
    assertThat(report.indexOf("## Summary")).isLessThan(report.indexOf("## Semantics per file"));
  }

  @Test
  void reports_failed_compilation_separately_from_successful_analysis() {
    var runs = runs();
    for (AnalysisMode mode : AnalysisMode.values()) {
      runs.put(mode, run(files(1, 0), Map.of("comparison.compilation.status", mode.compilation() ? "FAILED" : "DISABLED",
        "comparison.compilation.classes", "0", "comparison.bytecode.cleaned", "true"), List.of()));
    }

    assertThat(report(runs)).contains("| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |",
      "| Compilation outcome | DISABLED | DISABLED | FAILED | FAILED |",
      "| Generated class files before cleanup | 0 | 0 | 0 | 0 |",
      "| Temporary bytecode cleaned up | YES | YES | YES | YES |",
      "Scan success and compilation success are separate outcomes");
  }

  @Test
  void invalid_file_coverage_has_no_change_or_agreement_metrics() {
    var runs = runs();
    runs.put(AnalysisMode.BYTECODE, run(Map.of("Other.java", new SemanticReport.Counts(10, 1)), Map.of(), List.of()));

    assertThat(report(runs)).contains("| Bytecode versus Baseline | INVALID | N/A | N/A | N/A | N/A | N/A | N/A | N/A |",
      "| Combined versus Bytecode | INVALID | N/A | N/A | N/A | N/A | N/A | N/A | N/A |",
      "Comparison unavailable: The scans indexed different files", "| Other.java | N/A | N/A | 10 / 9 / 1 (10.000%) | N/A |");
  }

  @Test
  void different_identifier_coverage_invalidates_pairs_even_when_indexed_files_match() {
    var runs = runs();
    runs.put(AnalysisMode.SOURCE_PATHS, run(Map.of("Example.java", new SemanticReport.Counts(11, 2)), Map.of(), List.of()));

    assertThat(report(runs)).contains("| Source paths versus Baseline | INVALID | N/A | N/A | N/A | N/A | N/A | N/A | N/A |",
      "Identifier coverage differs for Example.java: current=10, candidate=11");
  }

  @Test
  void distinguishes_changed_unknown_occurrences_when_total_unknown_counts_are_equal() {
    var runs = runs();
    var before = new SemanticReport.UnknownIdentifier("Before", "2:4-2:10", "VARIABLE");
    var after = new SemanticReport.UnknownIdentifier("After", "3:4-3:9", "METHOD_INVOCATION");
    runs.replaceAll((mode, ignored) -> detailedRun(before));
    runs.put(AnalysisMode.SOURCE_PATHS, detailedRun(after));

    assertThat(report(runs)).contains("| Source paths versus Baseline | VALID | +0 | +0.000 |",
      "| No longer reported unknown | 1 |", "| Newly reported unknown | 1 |",
      "| Example.java | 2:4-2:10 | Before | VARIABLE |",
      "| Example.java | 3:4-3:9 | After | METHOD_INVOCATION |");
  }

  @Test
  void compares_each_feature_to_baseline_and_combined_to_each_feature() {
    String report = report(runs());

    assertThat(report).contains("<summary>Source paths versus Baseline</summary>", "<summary>Bytecode versus Baseline</summary>",
      "<summary>Combined versus Baseline</summary>", "<summary>Combined versus Source paths</summary>", "<summary>Combined versus Bytecode</summary>");
    assertThat(report.split("<summary>")).hasSize(6);
  }

  @Test
  void calculates_timing_medians_without_discarding_missing_measurements() {
    var samples = new EnumMap<AnalysisMode, List<SourceOnlyComparison.Run>>(AnalysisMode.class);
    for (AnalysisMode mode : AnalysisMode.values()) {
      samples.put(mode, List.of(timedRun(100, "20", "9"), timedRun(9, "1", "1"), timedRun(1, "3", "3")));
    }
    samples.put(AnalysisMode.SOURCE_PATHS, List.of(timedRun(100, "20", "9"), timedRun(9, "", "1"), timedRun(1, "3", "3")));

    String report = AnalysisMatrix.markdown(samples, List.of(), Map.of(), List.of());

    assertThat(report).contains("| Median Maven/server time (ms) | 9.0 | 9.0 | 9.0 | 9.0 |",
      "| Median JavaSensor time, including compilation (ms) | 3.0 | N/A | 3.0 | 3.0 |",
      "| Median internal compilation time (ms) | 3.0 | 3.0 | 3.0 | 3.0 |",
      "| 2 | Source paths | 9 | N/A | 1.0 | UNAVAILABLE | N/A |");
  }

  @Test
  void rejects_incomplete_modes_and_empty_sample_lists() {
    var incomplete = runs();
    incomplete.remove(AnalysisMode.COMBINED);
    assertThatIllegalArgumentException().isThrownBy(() -> report(incomplete)).withMessage("All four analysis modes are required");
    assertThatIllegalArgumentException().isThrownBy(() -> new AnalysisMatrix.Scenario("fixture", "details", incomplete))
      .withMessage("All four analysis modes are required");

    var samples = samples(runs());
    samples.put(AnalysisMode.BYTECODE, List.of());
    assertThatIllegalArgumentException().isThrownBy(() -> AnalysisMatrix.markdown(samples, List.of(), Map.of(), List.of()))
      .withMessage("Measured samples are required for Bytecode");
  }

  @Test
  void preserves_rule_counts_and_differing_finding_messages() {
    var first = new SourceOnlyComparison.Finding("java:S1116", "Example.java", 3, "first | finding\nmessage");
    var duplicate = new SourceOnlyComparison.Finding("java:S1116", "Example.java", 3, "duplicate");
    var added = new SourceOnlyComparison.Finding("java:S1874", "Example.java", 8, "added");
    var runs = runs();
    runs.put(AnalysisMode.BASELINE, run(Map.of("Example.java", new SemanticReport.Counts(10, 2)), Map.of(), List.of(first, duplicate)));
    runs.put(AnalysisMode.SOURCE_PATHS, run(Map.of("Example.java", new SemanticReport.Counts(10, 2)), Map.of(), List.of(duplicate)));
    runs.put(AnalysisMode.COMBINED, run(Map.of("Example.java", new SemanticReport.Counts(10, 2)), Map.of(), List.of(added)));

    String report = AnalysisMatrix.markdown(samples(runs), List.of("java:S1116", "java:S1874", "java:S1206"), Map.of(), List.of());

    assertThat(report).contains("| java:S1116 | 2 | 1 | 0 | 0 |", "| java:S1874 | 0 | 0 | 0 | 1 |",
      "1 configured rules had no findings", "first \\| finding message", "| java:S1874 | Example.java | 8 | added |",
      "| Source paths versus Baseline | VALID | +0 | +0.000 | 0 / 1 / 0 | 1 | 1 | 0 | 50.000% |");
  }

  @Test
  void writes_only_report_and_places_scenario_evidence_near_summary(@TempDir Path directory) throws IOException {
    var runs = runs();
    var scenario = new AnalysisMatrix.Scenario("Dependency scope", "Only the consumer is analyzed; dependency sources resolve references.", runs);
    AnalysisMatrix.write(directory, samples(runs), List.of(), Map.of("Setting", "value | with\nnewline"), List.of(scenario));

    try (var files = Files.list(directory)) {
      assertThat(files.map(path -> path.getFileName().toString()).toList()).containsExactly("report.md");
    }
    String report = Files.readString(directory.resolve("report.md"));
    assertThat(report).contains("### Dependency scope", scenario.description(), "| Setting | value \\| with newline |",
      "| Compilation outcome | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE |");
    assertThat(report.indexOf("## Focused correctness scenarios")).isLessThan(report.indexOf("## Semantics per file"));
  }

  @Test
  void failed_scans_show_error_and_missing_semantics_without_agreement() {
    var runs = runs();
    runs.put(AnalysisMode.BYTECODE, new SourceOnlyComparison.Run("bytecode", false, 12, List.of(), List.of(),
      Map.of("comparison.compilation.status", "FAILED"), null, "Scanner exited with code 1"));

    assertThat(report(runs)).contains("| Scan status | SUCCESS | SUCCESS | FAILED | SUCCESS |",
      "| Identifiers (total) | 10 | 10 | N/A | 10 |", "### Bytecode scan failure", "Scanner exited with code 1",
      "Comparison unavailable: At least one scan failed");
  }

  @Test
  void later_failed_repetition_invalidates_comparisons_instead_of_reusing_first_success() {
    var samples = samples(runs());
    var first = samples.get(AnalysisMode.BYTECODE).getFirst();
    var failed = new SourceOnlyComparison.Run("bytecode-second", false, 14, List.of(), List.of(), Map.of(), null, "Later scan failed");
    samples.put(AnalysisMode.BYTECODE, List.of(first, failed));

    String report = AnalysisMatrix.markdown(samples, List.of(), Map.of(), List.of());

    assertThat(report).contains("| Scan status | SUCCESS | SUCCESS | SUCCESS, FAILED | SUCCESS |",
      "| Identifiers (total) | 10 | 10 | N/A | 10 |",
      "| Bytecode versus Baseline | INVALID | N/A | N/A | N/A | N/A | N/A | N/A | N/A |",
      "Later scan failed");
  }

  @Test
  void unstable_repetitions_have_no_comparison_or_agreement_metrics() {
    String report = AnalysisMatrix.markdown(samples(runs()), List.of(), Map.of("Repeated result stability", "unstable"), List.of());

    assertThat(report).contains("Measured repetitions are unstable", "| Source paths versus Baseline | INVALID | N/A | N/A | N/A | N/A | N/A | N/A | N/A |")
      .doesNotContain("| VALID |", "| Still unknown in both modes |", "| No longer reported unknown |");
  }

  @Test
  void preserves_ast_contexts_unknown_contributors_and_ranked_semantic_changes() {
    var runs = runs();
    var before = semanticRun(Map.of("A.java", new SemanticReport.Counts(10, 2), "B.java", new SemanticReport.Counts(10, 0)),
      Map.of("A.java", List.of(new SemanticReport.UnknownIdentifier("first", "1:0-1:5", "VARIABLE"),
        new SemanticReport.UnknownIdentifier("second", "2:0-2:6", "MEMBER_SELECT")), "B.java", List.of()));
    var after = semanticRun(Map.of("A.java", new SemanticReport.Counts(10, 0), "B.java", new SemanticReport.Counts(10, 1)),
      Map.of("A.java", List.of(), "B.java", List.of(new SemanticReport.UnknownIdentifier("third", "3:0-3:5", "METHOD_INVOCATION"))));
    runs.replaceAll((mode, ignored) -> before);
    runs.put(AnalysisMode.SOURCE_PATHS, after);

    String report = report(runs);

    assertThat(report).contains("| VARIABLE | 1 | 0 | 1 | 1 |", "| MEMBER_SELECT | 1 | 0 | 1 | 1 |", "| METHOD_INVOCATION | 0 | 1 | 0 | 0 |",
      "| A.java | 2 (100.000%) | 0 (0.000%) | 2 (100.000%) | 2 (100.000%) |",
      "| B.java | 0 (0.000%) | 1 (100.000%) | 0 (0.000%) | 0 (0.000%) |",
      "| Source paths versus Baseline | VALID | -1 | -5.000 | 1 / 0 / 1 |",
      "### Largest semantic improvements", "| A.java | 20.000% | 0.000% | -20.000 |",
      "### Largest semantic regressions", "| B.java | 0.000% | 10.000% | +10.000 |");
    assertThat(report.indexOf("## Top files contributing unknown identifiers")).isLessThan(report.indexOf("## Semantics per file"));
  }

  @Test
  void shows_exact_probe_completions_and_bytecode_evidence_without_fixture_finding_agreement() {
    var fixtureRuns = runs();
    for (AnalysisMode mode : AnalysisMode.values()) {
      var completion = new SourceOnlyComparison.Finding("java-extension:semanticbindings", "Example.java", 1,
        "Semantic binding probes: 5 checked, 0 failed.");
      var bytecode = new SourceOnlyComparison.Finding("java-extension:semanticbindings", "Example.java", 1,
        "Generated bytecode probe: " + (mode.compilation() ? "present" : "absent") + ".");
      fixtureRuns.put(mode, run(Map.of("Example.java", new SemanticReport.Counts(10, 2)), Map.of(), List.of(completion, bytecode)));
    }
    var scenario = new AnalysisMatrix.Scenario("Exact bindings", "Dependency files remain outside analysis.", fixtureRuns);

    String report = AnalysisMatrix.markdown(samples(runs()), List.of(), Map.of(), List.of(scenario));

    assertThat(report).contains("| Exact binding probes checked | 5 | 5 | 5 | 5 |", "| Exact binding probe failures | 0 | 0 | 0 | 0 |",
      "Semantic binding probes: 5 checked, 0 failed.",
      "| Bytecode during analysis | Generated bytecode probe: absent. | Generated bytecode probe: absent. | Generated bytecode probe: present. | Generated bytecode probe: present. |",
      "| Coverage versus baseline | VALID | VALID | VALID | VALID |");
    assertThat(report.split("### Feature comparisons")).hasSize(2);
    String scenarioSection = report.substring(report.indexOf("## Focused correctness scenarios"), report.indexOf("### Feature comparisons"));
    assertThat(scenarioSection).doesNotContain("Shared findings", "Retention");
  }

  @Test
  void excludes_instrumentation_from_product_finding_counts_and_retention() {
    var runs = runs();
    var syntax = new SourceOnlyComparison.Finding("java:S1116", "Example.java", 3, "syntax finding");
    var probe = new SourceOnlyComparison.Finding("java-extension:semanticbindings", "Example.java", 1, "Semantic binding probes: 4 checked, 0 failed.");
    runs.put(AnalysisMode.BASELINE, run(Map.of("Example.java", new SemanticReport.Counts(10, 2)), Map.of(), List.of(syntax, probe)));
    runs.put(AnalysisMode.SOURCE_PATHS, run(Map.of("Example.java", new SemanticReport.Counts(10, 2)), Map.of(), List.of(syntax)));

    assertThat(report(runs)).contains("| Findings | 1 | 1 | 0 | 0 |",
      "| Source paths versus Baseline | VALID | +0 | +0.000 | 0 / 1 / 0 | 1 | 0 | 0 | 100.000% |")
      .doesNotContain("| java-extension:semanticbindings |", "Semantic binding probes: 4 checked, 0 failed.");
  }

  @Test
  void reports_semantic_graph_counts_and_location_changes_without_coverage_claims() {
    var runs = runs();
    var before = graphRun(new SemanticReport.GraphCounts(8, 1, 5, 1), List.of("module:OldSymbol@2:0"), List.of("module:OldType@3:0"));
    var after = graphRun(new SemanticReport.GraphCounts(12, 1, 7, 1), List.of("module:NewSymbol@4:0"), List.of("module:NewType@5:0"));
    runs.replaceAll((mode, ignored) -> before);
    runs.put(AnalysisMode.SOURCE_PATHS, after);

    assertThat(report(runs)).contains("| Semantic graph resolved symbols | 8 | 12 | 8 | 8 |",
      "| Semantic graph unknown symbols | 1 | 1 | 1 | 1 |", "| Semantic graph resolved types | 5 | 7 | 5 | 5 |",
      "| Semantic graph unknown types | 1 | 1 | 1 | 1 |", "| Unknown locations absent from candidate observation | 1 |",
      "| Unknown locations absent from baseline observation | 1 |", "| module:OldSymbol@2:0 |", "| module:NewType@5:0 |",
      "Expanded traversal can add locations", "graph counts are diagnostics, not coverage percentages or correctness scores")
      .doesNotContain("Resolved symbols (%)", "Resolved types (%)");
  }

  @Test
  void renders_canonical_module_only_comparisons_with_real_module_counts_and_unavailable_file_metrics() {
    var runs = runs();
    runs.put(AnalysisMode.BASELINE, canonicalRun(100, false));
    runs.put(AnalysisMode.SOURCE_PATHS, canonicalRun(20, false));
    runs.put(AnalysisMode.BYTECODE, canonicalRun(5, false));
    runs.put(AnalysisMode.COMBINED, canonicalRun(2, false));
    String report = report(runs);

    assertThat(report).contains("| Identifiers (total) | 1000 | 1000 | 1000 | 1000 |",
      "| Per-file semantic observations | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE |",
      "| Files with no unknown identifiers | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE |",
      "## Semantics per module", "| . | 0 / 0 / 0 (N/A) | 0 / 0 / 0 (N/A) | 0 / 0 / 0 (N/A) | 0 / 0 / 0 (N/A) |",
      "| main | 1000 / 900 / 100 (10.000%) | 1000 / 980 / 20 (2.000%) | 1000 / 995 / 5 (0.500%) | 1000 / 998 / 2 (0.200%) |",
      "| Source paths versus Baseline | VALID | -80 | -8.000 | UNAVAILABLE |",
      "UNAVAILABLE: per-file observations are absent in all modes", "UNAVAILABLE: unknown occurrence details are absent in all modes")
      .doesNotContain("| Files with no unknown identifiers | 0 / 0", "| main/A.java | 1000", "| No longer reported unknown | 0 |");
    assertThat(report.indexOf("## Semantics per module")).isLessThan(report.indexOf("## Semantics per file"));
  }

  @Test
  void module_only_mode_changes_remain_invalid_when_module_coverage_differs() {
    var runs = runs();
    runs.replaceAll((mode, ignored) -> canonicalRun(10, false));
    var base = runs.get(AnalysisMode.SOURCE_PATHS);
    var counts = new SemanticReport.Counts(1000, 1);
    runs.put(AnalysisMode.SOURCE_PATHS, new SourceOnlyComparison.Run(base.label(), true, 12, base.files(), List.of(), Map.of(),
      new SemanticReport(counts, Map.of(), Map.of(), null, Map.of(), List.of(), List.of(), null,
        Map.of("other", counts), false), null));

    assertThat(report(runs)).contains("| Source paths versus Baseline | INVALID | N/A | N/A | N/A | N/A | N/A | N/A | N/A |",
      "Comparison unavailable: Semantic reports cover different modules");
  }

  @Test
  void optional_file_observations_can_be_missing_in_some_modes_without_invalidating_module_comparisons() {
    var runs = runs();
    runs.replaceAll((mode, ignored) -> canonicalRun(10, false));
    runs.put(AnalysisMode.BASELINE, canonicalRun(10, true));

    assertThat(report(runs)).contains("| Per-file semantic observations | AVAILABLE | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE |",
      "| Source paths versus Baseline | VALID | +0 | +0.000 | UNAVAILABLE |",
      "| main/A.java | 1000 / 990 / 10 (1.000%) | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE |",
      "| main/A.java | 10 (100.000%) | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE |");
  }

  private static SourceOnlyComparison.Run canonicalRun(int unknown, boolean observations) {
    var counts = new SemanticReport.Counts(1000, unknown);
    var files = observations ? Map.of("main/A.java", counts) : Map.<String, SemanticReport.Counts>of();
    return new SourceOnlyComparison.Run("canonical", true, 12, List.of("main/A.java"), List.of(), Map.of(),
      new SemanticReport(counts, files, Map.of(), null, Map.of(), List.of(), List.of(), null,
        Map.of(".", new SemanticReport.Counts(0, 0), "main", counts), observations), null);
  }

  private static SourceOnlyComparison.Run graphRun(SemanticReport.GraphCounts graph, List<String> symbols, List<String> types) {
    var base = run(Map.of("Example.java", new SemanticReport.Counts(10, 2)), Map.of(), List.of());
    return new SourceOnlyComparison.Run(base.label(), true, base.scanMillis(), base.files(), base.findings(), base.telemetry(),
      new SemanticReport(base.semantics().totals(), base.semantics().files(), Map.of(), graph, Map.of("module", graph), symbols, types,
        new SemanticReport.GraphTraversal(true, 1000, 20)), null);
  }

  private static Map<String, SemanticReport.Counts> files(int firstUnknown, int secondUnknown) {
    return Map.of("A.java", new SemanticReport.Counts(1000, firstUnknown), "B.java", new SemanticReport.Counts(1, secondUnknown));
  }

  private static EnumMap<AnalysisMode, SourceOnlyComparison.Run> runs() {
    var runs = new EnumMap<AnalysisMode, SourceOnlyComparison.Run>(AnalysisMode.class);
    for (AnalysisMode mode : AnalysisMode.values()) {
      runs.put(mode, run(Map.of("Example.java", new SemanticReport.Counts(10, 2)), Map.of(), List.of()));
    }
    return runs;
  }

  private static SourceOnlyComparison.Run run(Map<String, SemanticReport.Counts> files, Map<String, String> telemetry,
                                             List<SourceOnlyComparison.Finding> findings) {
    var totals = new SemanticReport.Counts(files.values().stream().mapToInt(SemanticReport.Counts::total).sum(),
      files.values().stream().mapToInt(SemanticReport.Counts::unknown).sum());
    return new SourceOnlyComparison.Run("mode", true, 12, new TreeMap<>(files).keySet().stream().toList(), findings, telemetry,
      new SemanticReport(totals, files), null);
  }

  private static SourceOnlyComparison.Run detailedRun(SemanticReport.UnknownIdentifier identifier) {
    var counts = new SemanticReport.Counts(10, 1);
    return new SourceOnlyComparison.Run("mode", true, 12, List.of("Example.java"), List.of(), Map.of(),
      new SemanticReport(counts, Map.of("Example.java", counts), Map.of("Example.java", List.of(identifier))), null);
  }

  private static SourceOnlyComparison.Run semanticRun(Map<String, SemanticReport.Counts> files,
                                                      Map<String, List<SemanticReport.UnknownIdentifier>> details) {
    var run = run(files, Map.of(), List.of());
    return new SourceOnlyComparison.Run(run.label(), true, run.scanMillis(), run.files(), run.findings(), run.telemetry(),
      new SemanticReport(run.semantics().totals(), files, details), null);
  }

  private static SourceOnlyComparison.Run timedRun(long millis, String analyzerMillis, String compilationMillis) {
    var run = run(Map.of("Example.java", new SemanticReport.Counts(10, 2)),
      Map.of("comparison.analyzer.time_ms", analyzerMillis, "comparison.compilation.time_ms", compilationMillis), List.of());
    return new SourceOnlyComparison.Run(run.label(), true, millis, run.files(), run.findings(), run.telemetry(), run.semantics(), null);
  }

  private static EnumMap<AnalysisMode, List<SourceOnlyComparison.Run>> samples(Map<AnalysisMode, SourceOnlyComparison.Run> runs) {
    var samples = new EnumMap<AnalysisMode, List<SourceOnlyComparison.Run>>(AnalysisMode.class);
    runs.forEach((mode, run) -> samples.put(mode, List.of(run)));
    return samples;
  }

  private static String report(Map<AnalysisMode, SourceOnlyComparison.Run> runs) {
    return AnalysisMatrix.markdown(samples(runs), List.of(), Map.of(), List.of());
  }
}
