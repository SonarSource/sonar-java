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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisComparisonReportTest {

  @Test
  void compares_modes_only_inside_each_dataset_and_reports_resolution_cost() {
    var normal = result("normal", "normal / no libraries", 6, 6, 100, 120, Map.of());
    var separated = result("separated", "file-by-file / dependencies", 8, 1, 200, 260, Map.of());

    String report = report(List.of(normal, separated), List.of());

    assertThat(report).contains("| normal / no libraries | 6 (30.000%; Δ +0) | 6 (30.000%; Δ +0)",
      "| file-by-file / dependencies | 8 (40.000%; Δ +0) | 1 (5.000%; Δ -7)",
      "| normal / no libraries | Source paths | SUCCESS | VALID | +0 | 0 | 120.0 / 120.0 / 120.0 | +20.0 | DISABLED |",
      "| file-by-file / dependencies | Source paths | SUCCESS | VALID | -7 | 0 | 260.0 / 260.0 / 260.0 | +60.0 | DISABLED |",
      "no reduction in unknown identifiers; JavaSensor median +20.0 ms", "7 fewer unknown identifiers; JavaSensor median +60.0 ms",
      "Feature effects are comparisons **within a row**", "File-by-file analysis is a sensitivity experiment",
      "<summary>normal / no libraries</summary>", "<summary>file-by-file / dependencies</summary>")
      .doesNotContain("universal winner is", "Δ -5");
    assertThat(report.indexOf("## Results at a glance")).isLessThan(report.indexOf("## Dataset details"));
  }

  @Test
  void puts_reviewed_product_findings_before_details_and_excludes_probe_instrumentation() {
    var runs = new EnumMap<AnalysisMode, SourceOnlyComparison.Run>(AnalysisMode.class);
    String path = "src/main/java/bindings/BindingFixture.java";
    for (AnalysisMode mode : AnalysisMode.values()) {
      var probe = new SourceOnlyComparison.Finding("java-extension:semanticbindings", path, 1, "Semantic binding probes: 4 checked, 0 failed.");
      var syntax = new SourceOnlyComparison.Finding("java:S1116", path, 11, "empty statement");
      var deprecated = new SourceOnlyComparison.Finding("java:S1874", path, 8, "deprecated call");
      var base = run(mode, 0, 100);
      runs.put(mode, new SourceOnlyComparison.Run(mode.id(), true, base.scanMillis(), List.of(path),
        mode == AnalysisMode.BASELINE ? List.of(probe, syntax) : List.of(probe, syntax, deprecated), base.telemetry(),
        new SemanticReport(new SemanticReport.Counts(20, 0), Map.of(path, new SemanticReport.Counts(20, 0))), null));
    }
    var fixture = new AnalysisMatrix.Scenario("Clean compilation", "Reviewed calls and exact bindings", runs);

    String report = report(List.of(result("normal", "normal", 1, 0, 100, 120, Map.of())), List.of(fixture));

    assertThat(report).contains("| S1874: deprecated call, line 8 / 1 | 0 (missed) | 1 (correct) | 1 (correct) | 1 (correct) |",
      "| S1874: nondeprecated controls, lines 9–10 / 0 | 0 (correct) | 0 (correct) | 0 (correct) | 0 (correct) |",
      "| S1116: empty statement, line 11 / 1 | 1 (correct) | 1 (correct) | 1 (correct) | 1 (correct) |",
      "| S1874 reviewed fixture: TP / FP / FN | 0 / 0 / 1 | 1 / 0 / 0 | 1 / 0 / 0 | 1 / 0 / 0 |",
      "| Product findings in fixture | 1 | 2 | 2 | 2 |", "Exact binding probe failures", "Probe findings are instrumentation");
    assertThat(report.indexOf("## Reviewed finding evidence")).isLessThan(report.indexOf("## Dataset details"));
  }

  @Test
  void compilation_failure_does_not_invalidate_successful_analysis_and_missing_measurements_stay_missing() {
    var result = result("normal", "normal", 3, 3, 100, 120, Map.of());
    var measured = new EnumMap<>(result.samples());
    var bytecode = measured.get(AnalysisMode.BYTECODE).getFirst();
    measured.put(AnalysisMode.BYTECODE, List.of(new SourceOnlyComparison.Run(bytecode.label(), true, bytecode.scanMillis(), bytecode.files(),
      bytecode.findings(), Map.of("comparison.compilation.status", "FAILED", "comparison.compilation.classes", "0"), bytecode.semantics(), null)));
    var changed = new AnalysisComparisonReport.DatasetResult(result.dataset(), measured, Map.of());

    assertThat(report(List.of(changed), List.of())).contains("| normal | Bytecode | SUCCESS | VALID | +0 | 0 | N/A | N/A | FAILED | N/A | 0 | N/A |",
      "compilation FAILED", "Compilation SUCCESS or FAILED is measured separately from scan success");
  }

  @Test
  void failed_later_sample_and_unstable_repetitions_suppress_deltas() {
    var result = result("normal", "normal", 3, 0, 100, 120, Map.of());
    var measured = new EnumMap<>(result.samples());
    measured.put(AnalysisMode.BYTECODE, List.of(measured.get(AnalysisMode.BYTECODE).getFirst(),
      new SourceOnlyComparison.Run("later", false, 100, List.of(), List.of(), Map.of(), null, "later scan failed")));
    var failed = new AnalysisComparisonReport.DatasetResult(result.dataset(), measured, Map.of());

    assertThat(report(List.of(failed), List.of())).contains("| normal | Bytecode | SUCCESS, FAILED | INVALID | N/A | N/A |",
      "comparison unavailable: At least one scan failed", "later scan failed");
    var unstable = new AnalysisComparisonReport.DatasetResult(result.dataset(), result.samples(), Map.of("Repeated result stability", "unstable"));
    String unstableReport = report(List.of(unstable), List.of());
    assertThat(unstableReport).contains("0 (0.000%; Δ N/A)", "Measured repetitions are unstable").doesNotContain("| VALID |", "fewer unknown identifiers;");
  }

  @Test
  void invalid_identifier_coverage_does_not_create_an_apparent_improvement() {
    var result = result("normal", "normal", 3, 0, 100, 120, Map.of());
    var measured = new EnumMap<>(result.samples());
    var sourcepaths = measured.get(AnalysisMode.SOURCE_PATHS).getFirst();
    measured.put(AnalysisMode.SOURCE_PATHS, List.of(new SourceOnlyComparison.Run(sourcepaths.label(), true, sourcepaths.scanMillis(), sourcepaths.files(),
      sourcepaths.findings(), sourcepaths.telemetry(), new SemanticReport(new SemanticReport.Counts(21, 0), Map.of("Example.java", new SemanticReport.Counts(21, 0))), null)));

    assertThat(report(List.of(new AnalysisComparisonReport.DatasetResult(result.dataset(), measured, Map.of())), List.of()))
      .contains("0 (0.000%; Δ N/A)", "| normal | Source paths | SUCCESS | INVALID | N/A |", "Identifier coverage differs for Example.java");
  }

  @Test
  void shows_timing_dispersion_from_measured_samples_without_implying_statistical_significance() {
    var result = result("normal", "normal", 3, 0, 100, 120, Map.of());
    var measured = new EnumMap<>(result.samples());
    measured.put(AnalysisMode.BASELINE, List.of(run(AnalysisMode.BASELINE, 3, 100), run(AnalysisMode.BASELINE, 3, 10), run(AnalysisMode.BASELINE, 3, 20)));
    measured.put(AnalysisMode.SOURCE_PATHS, List.of(run(AnalysisMode.SOURCE_PATHS, 0, 100), run(AnalysisMode.SOURCE_PATHS, 0, 15), run(AnalysisMode.SOURCE_PATHS, 0, 25)));

    assertThat(report(List.of(new AnalysisComparisonReport.DatasetResult(result.dataset(), measured, Map.of())), List.of()))
      .contains("| normal | Baseline | SUCCESS | VALID | +0 | 0 | 10.0 / 20.0 / 100.0 | +0.0 |",
        "| normal | Source paths | SUCCESS | VALID | -3 | 0 | 15.0 / 25.0 / 100.0 | +5.0 |",
        "| normal | Baseline | 0.0 / 0.0 / 0.0 | 10.0 / 20.0 / 100.0 |",
        "| normal | Bytecode | 40.0 / 40.0 / 40.0 | 80.0 / 80.0 / 80.0 |",
        "No universal winner or statistical significance is inferred", "not raw production performance");
  }

  @Test
  void keeps_graph_diagnostics_separate_from_identifier_percentages() {
    var result = result("normal", "normal", 3, 0, 100, 120, Map.of());
    var measured = new EnumMap<>(result.samples());
    measured.replaceAll((mode, samples) -> {
      var run = samples.getFirst();
      var graph = new SemanticReport.GraphCounts(10, 1, 7, 1);
      return List.of(new SourceOnlyComparison.Run(run.label(), true, run.scanMillis(), run.files(), run.findings(), run.telemetry(),
        new SemanticReport(run.semantics().totals(), run.semantics().files(), Map.of(), graph, Map.of("module", graph),
          List.of(mode == AnalysisMode.BASELINE ? "module:UnknownSymbol@4:2" : "module:OtherUnknownSymbol@5:2"), List.of("module:UnknownType@4:4"),
          new SemanticReport.GraphTraversal(true, 1000, 17)), null));
    });

    assertThat(report(List.of(new AnalysisComparisonReport.DatasetResult(result.dataset(), measured, Map.of())), List.of()))
      .contains("| normal | Baseline | COMPLETE | 1000 | 17 | 10 | 1 | 7 | 1 |", "counts, not resolution-coverage percentages",
        "bounded traversal", "module:UnknownSymbol@4:2").doesNotContain("Resolved symbols (%)", "Resolved types (%)");
  }

  @Test
  void variable_graph_observations_show_ranges_without_invalidating_primary_results() {
    var result = result("normal", "normal", 3, 0, 100, 120, Map.of("Graph diagnostic stability", "variable"));
    var measured = new EnumMap<>(result.samples());
    measured.replaceAll((mode, samples) -> List.of(withGraph(samples.getFirst(), 10), withGraph(samples.getFirst(), 12)));

    String report = report(List.of(new AnalysisComparisonReport.DatasetResult(result.dataset(), measured, result.metadata())), List.of());

    assertThat(report).contains("| normal | Baseline | COMPLETE | 1000 | 17–19 | 10–12 | 1 | 7 | 1 |", "0 (0.000%; Δ -3)",
      "graph diagnostics vary between measured repetitions", "primary identifier/finding comparisons remain independent")
      .doesNotContain("Unknown symbol locations absent from candidate observation", "| Source paths versus Baseline | INVALID |");
  }

  private static SourceOnlyComparison.Run withGraph(SourceOnlyComparison.Run run, int resolvedSymbols) {
    var graph = new SemanticReport.GraphCounts(resolvedSymbols, 1, 7, 1);
    return new SourceOnlyComparison.Run(run.label(), true, run.scanMillis(), run.files(), run.findings(), run.telemetry(),
      new SemanticReport(run.semantics().totals(), run.semantics().files(), Map.of(), graph, Map.of("module", graph),
        List.of("module:UnknownSymbol@4:2"), List.of("module:UnknownType@4:4"), new SemanticReport.GraphTraversal(true, 1000, resolvedSymbols + 7)), null);
  }

  @Test
  void partial_graph_observation_suppresses_graph_location_inference_but_preserves_identifier_comparison() {
    var result = result("normal", "normal", 3, 0, 100, 120, Map.of("Graph diagnostic stability", "stable"));
    var measured = new EnumMap<>(result.samples());
    measured.replaceAll((mode, samples) -> {
      var observed = withGraph(samples.getFirst(), 10);
      var semantic = observed.semantics();
      return List.of(new SourceOnlyComparison.Run(observed.label(), true, observed.scanMillis(), observed.files(), observed.findings(), observed.telemetry(),
        new SemanticReport(semantic.totals(), semantic.files(), semantic.unknownIdentifiers(), semantic.graphTotals(), semantic.moduleGraphCounts(),
          semantic.unknownSymbols(), semantic.unknownTypes(), new SemanticReport.GraphTraversal(false, 1000, 1000)), null));
    });

    String report = report(List.of(new AnalysisComparisonReport.DatasetResult(result.dataset(), measured, result.metadata())), List.of());

    assertThat(report).contains("| normal | Baseline | PARTIAL | 1000 | 1000 | 10 | 1 | 7 | 1 |",
      "0 (0.000%; Δ -3)", "| Source paths versus Baseline | VALID | -3 |", "traversal is DISABLED, PARTIAL, or UNKNOWN",
      "still cover the full analyzed AST", "1,000 expansions per module", "that aborted scan is excluded from results")
      .doesNotContain("Unknown symbol locations absent from candidate observation", "Unknown type locations absent from baseline observation");
  }

  @Test
  void absent_completeness_metadata_is_unknown_and_does_not_assume_a_complete_graph() {
    var result = result("normal", "normal", 3, 0, 100, 120, Map.of());
    var measured = new EnumMap<>(result.samples());
    measured.replaceAll((mode, samples) -> {
      var observed = withGraph(samples.getFirst(), 10);
      var semantic = observed.semantics();
      return List.of(new SourceOnlyComparison.Run(observed.label(), true, observed.scanMillis(), observed.files(), observed.findings(), observed.telemetry(),
        new SemanticReport(semantic.totals(), semantic.files(), semantic.unknownIdentifiers(), semantic.graphTotals(), semantic.moduleGraphCounts(),
          semantic.unknownSymbols(), semantic.unknownTypes()), null));
    });

    assertThat(report(List.of(new AnalysisComparisonReport.DatasetResult(result.dataset(), measured, Map.of())), List.of()))
      .contains("| normal | Baseline | UNKNOWN | N/A | N/A | 10 | 1 | 7 | 1 |", "traversal is DISABLED, PARTIAL, or UNKNOWN",
        "| Source paths versus Baseline | VALID | -3 |")
      .doesNotContain("Unknown symbol locations absent from candidate observation");
  }

  @Test
  void disabled_graph_observation_reports_unavailable_counts_without_invalidating_primary_metrics() {
    var result = result("normal", "normal", 3, 0, 100, 120, Map.of("Graph diagnostic stability", "stable"));
    var measured = new EnumMap<>(result.samples());
    measured.replaceAll((mode, samples) -> {
      var run = samples.getFirst();
      var graph = new SemanticReport.GraphCounts(0, 0, 0, 0);
      return List.of(new SourceOnlyComparison.Run(run.label(), true, run.scanMillis(), run.files(), run.findings(), run.telemetry(),
        new SemanticReport(run.semantics().totals(), run.semantics().files(), Map.of(), graph, Map.of("module", graph),
          List.of(), List.of(), new SemanticReport.GraphTraversal(false, 0, 0)), null));
    });

    String report = report(List.of(new AnalysisComparisonReport.DatasetResult(result.dataset(), measured, result.metadata())), List.of());

    assertThat(report).contains("| normal | Baseline | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |",
      "| Semantic graph resolved symbols | N/A | N/A | N/A | N/A |",
      "| Semantic graph unknown types | N/A | N/A | N/A | N/A |",
      "0 (0.000%; Δ -3)", "| Source paths versus Baseline | VALID | -3 |",
      "recorded expansion limit 0 in every measured mode", "no recursive graph observation",
      "traversal is DISABLED, PARTIAL, or UNKNOWN")
      .doesNotContain("Recorded observation-only graph budget: 1,000", "Unknown symbol locations absent from candidate observation");
  }

  @Test
  void writes_one_report_for_all_datasets_with_global_and_dataset_provenance(@TempDir Path directory) throws IOException {
    var first = result("first", "first", 3, 0, 100, 120, Map.of("Classpath JAR SHA-256", "first-library-sha"));
    var second = result("second", "second", 3, 0, 100, 120, Map.of("Source snapshot SHA-256", "second-source-sha"));
    AnalysisComparisonReport.write(directory, List.of(first, second), List.of(), Map.of("PR #6309 head", "fresh-head"), List.of());

    try (var files = Files.list(directory)) {
      assertThat(files.map(path -> path.getFileName().toString()).toList()).containsExactly("report.md");
    }
    assertThat(Files.readString(directory.resolve("report.md"))).contains("| PR #6309 head | fresh-head |",
      "| Classpath JAR SHA-256 | first-library-sha |", "| Source snapshot SHA-256 | second-source-sha |");
  }

  @Test
  void requires_complete_unique_datasets_and_immutable_sample_maps() {
    var result = result("normal", "normal", 3, 0, 100, 120, Map.of());
    assertThatIllegalArgumentException().isThrownBy(() -> report(List.of(), List.of())).withMessageContaining("At least one dataset");
    assertThatIllegalArgumentException().isThrownBy(() -> report(List.of(result, result), List.of())).withMessageContaining("must be unique");
    assertThatThrownBy(() -> result.samples().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.samples().get(AnalysisMode.BASELINE).clear()).isInstanceOf(UnsupportedOperationException.class);
    var partial = new EnumMap<>(result.samples());
    partial.remove(AnalysisMode.COMBINED);
    assertThatIllegalArgumentException().isThrownBy(() -> new AnalysisComparisonReport.DatasetResult(result.dataset(), partial, Map.of()))
      .withMessage("All four analysis modes are required");
  }

  private static AnalysisComparisonReport.DatasetResult result(String id, String label, int baselineUnknown, int enabledUnknown,
                                                              int baselineTime, int enabledTime, Map<String, String> metadata) {
    var samples = new EnumMap<AnalysisMode, List<SourceOnlyComparison.Run>>(AnalysisMode.class);
    for (AnalysisMode mode : AnalysisMode.values()) {
      samples.put(mode, List.of(run(mode, mode == AnalysisMode.BASELINE ? baselineUnknown : enabledUnknown,
        mode == AnalysisMode.BASELINE ? baselineTime : enabledTime)));
    }
    return new AnalysisComparisonReport.DatasetResult(new AnalysisDataset(id, label, "source snapshot", "src/main/java", Map.of(), List.of(), false), samples, metadata);
  }

  private static SourceOnlyComparison.Run run(AnalysisMode mode, int unknown, int analyzerTime) {
    var counts = new SemanticReport.Counts(20, unknown);
    return new SourceOnlyComparison.Run(mode.id(), true, analyzerTime + 100, List.of("Example.java"), List.of(),
      Map.of("comparison.analyzer.time_ms", Integer.toString(analyzerTime), "comparison.compilation.time_ms", mode.compilation() ? "40" : "0",
        "comparison.compilation.status", mode.compilation() ? "SUCCESS" : "DISABLED", "comparison.compilation.classes", mode.compilation() ? "3" : "0",
        "comparison.bytecode.cleaned", "true"), new SemanticReport(counts, Map.of("Example.java", counts)), null);
  }

  private static String report(List<AnalysisComparisonReport.DatasetResult> datasets, List<AnalysisMatrix.Scenario> fixtures) {
    return AnalysisComparisonReport.markdown(datasets, List.of(), Map.of(), fixtures);
  }
}
