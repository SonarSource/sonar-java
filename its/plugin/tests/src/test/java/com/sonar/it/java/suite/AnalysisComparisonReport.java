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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class AnalysisComparisonReport {

  record DatasetResult(AnalysisDataset dataset, Map<AnalysisMode, List<SourceOnlyComparison.Run>> samples, Map<String, String> metadata) {
    DatasetResult {
      AnalysisMatrix.requireSamples(samples);
      var immutable = new EnumMap<AnalysisMode, List<SourceOnlyComparison.Run>>(AnalysisMode.class);
      samples.forEach((mode, runs) -> immutable.put(mode, List.copyOf(runs)));
      samples = Map.copyOf(immutable);
      metadata = Map.copyOf(metadata);
    }

    Map<AnalysisMode, SourceOnlyComparison.Run> runs() {
      return AnalysisMatrix.representatives(samples);
    }

    boolean stable() {
      return !"unstable".equalsIgnoreCase(metadata.get("Repeated result stability"));
    }
  }

  private AnalysisComparisonReport() {
  }

  static void write(Path directory, List<DatasetResult> datasets, List<String> activeRules,
                    Map<String, String> globalMetadata, List<AnalysisMatrix.Scenario> fixtures) throws IOException {
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("report.md"), markdown(datasets, activeRules, globalMetadata, fixtures));
  }

  static String markdown(List<DatasetResult> datasets, List<String> activeRules,
                         Map<String, String> globalMetadata, List<AnalysisMatrix.Scenario> fixtures) {
    if (datasets.isEmpty() || datasets.stream().map(result -> result.dataset().id()).distinct().count() != datasets.size()) {
      throw new IllegalArgumentException("At least one dataset is required and dataset identifiers must be unique");
    }
    var report = new StringBuilder("# Java source-path and internal-compilation comparison\n\n")
      .append("One analyzer artifact, four modes: **Baseline**, **Source paths (#6308)**, **Bytecode (#6309)**, and **Combined**. Maven runs only `sonar:sonar`; no project bytecode is supplied.\n\n")
      .append("## Results at a glance\n\n")
      .append("Cells show **unknown identifiers (unknown %; change from the same dataset's baseline)**. A negative change means fewer unknown identifiers. Invalid or unstable comparisons have no change metric.\n\n");
    header(report, "Dataset / parser batching / external JARs");
    for (DatasetResult result : datasets) {
      row(report, result.dataset().label(), mode -> identifierCell(result, mode, activeRules));
    }
    report.append("\nNormal batches are the ordinary-analysis control. File-by-file analysis is a sensitivity experiment that removes cross-file parser-batch resolution. Library-enabled datasets supply identical external compile-scope JARs to every mode. Feature effects are comparisons **within a row**; adding libraries changes the input and is a separate factor.\n\n");
    appendReviewedFindings(report, fixtures);
    appendFixtureSummary(report, fixtures);
    appendCosts(report, datasets, activeRules);
    appendObservedTradeoffs(report, datasets, activeRules);
    appendGraphs(report, datasets);
    if (!fixtures.isEmpty()) {
      report.append("\n<details>\n<summary>Focused fixture measurements and exact binding evidence</summary>\n\n");
      AnalysisMatrix.appendScenarios(report, fixtures);
      report.append("\n</details>\n");
    }
    report.append("\n## Dataset details\n\n");
    for (DatasetResult result : datasets) {
      report.append("<details>\n<summary>").append(html(result.dataset().label())).append("</summary>\n\n")
        .append(escape(result.dataset().description())).append("\n\n")
        .append(AnalysisMatrix.detailsMarkdown(result.samples(), activeRules, result.metadata()))
        .append("\n</details>\n\n");
    }
    appendMetadata(report, globalMetadata);
    report.append("\n## Reading the comparison\n\n")
      .append("- Identifier percentages use aggregate counts. Same-dataset comparisons require successful scans, identical indexed files, matching canonical modules and global/module identifier totals. Per-file coverage and totals are checked when observations exist; unavailable file observations are labelled explicitly. Unstable primary repetitions have no comparison metrics.\n")
      .append("- Compilation SUCCESS or FAILED is measured separately from scan success. Missing measurements are UNAVAILABLE or N/A. Failed compilation can leave partial classes that are available during analysis; generated classes must be removed afterward.\n")
      .append("- JavaSensor time includes internal compilation; Maven/server time also includes scanner startup and server processing. All timing medians exclude warm-ups. A few repetitions describe this run and do not establish a general performance ranking.\n")
      .append("- Product findings include only `java:` rules. Probe findings are instrumentation. Findings on real projects measure agreement, while the explicitly reviewed fixture checks binding correctness and expected findings.\n")
      .append("- Unknown identifier occurrences match by file, name, and token range. Fewer unknowns alone do not prove correct bindings. AST context is diagnostic rather than a semantic role.\n")
      .append("- Semantic graph counts describe unique references reached by the target branch's bounded traversal. Resolving a name can expose additional graph nodes, so these counts and location differences are diagnostics rather than coverage percentages or correctness scores.\n")
      .append("- Source paths do not expand analyzed scope. Internal compilation disables annotation processing. External libraries, parser batching, source files, JDK, profile, and extra properties are held fixed across each dataset's four modes.\n");
    return report.toString();
  }

  private static String identifierCell(DatasetResult result, AnalysisMode mode, List<String> activeRules) {
    var run = result.runs().get(mode);
    if (run.semantics() == null) {
      return "N/A";
    }
    var counts = run.semantics().totals();
    var comparison = compare(result, mode, activeRules);
    String change = comparison.valid() ? signed(counts.unknown() - comparison.current().semantics().totals().unknown()) : "N/A";
    return counts.unknown() + " (" + percentage(counts.unknownPercentage()) + "; Δ " + change + ")";
  }

  private static void appendReviewedFindings(StringBuilder report, List<AnalysisMatrix.Scenario> fixtures) {
    fixtures.stream().filter(fixture -> fixture.name().equals("Clean compilation")).findFirst().ifPresent(fixture -> {
      report.append("## Reviewed finding evidence\n\n")
        .append("The clean fixture has one genuinely deprecated call, two nondeprecated control calls, and one empty statement. These are reviewed expectations, not inferred accuracy from analyzer agreement. Cells show actual findings and whether they match the reviewed code.\n\n");
      header(report, "Reviewed location / expected findings");
      String path = "src/main/java/bindings/BindingFixture.java";
      row(report, "S1874: deprecated call, line 8 / 1", mode -> oracle(fixture.runs().get(mode), "java:S1874", path, Set.of(8), 1));
      row(report, "S1874: nondeprecated controls, lines 9–10 / 0", mode -> oracle(fixture.runs().get(mode), "java:S1874", path, Set.of(9, 10), 0));
      row(report, "S1116: empty statement, line 11 / 1", mode -> oracle(fixture.runs().get(mode), "java:S1116", path, Set.of(11), 1));
      row(report, "S1874 reviewed fixture: TP / FP / FN", mode -> findingAccuracy(fixture.runs().get(mode), "java:S1874", path, 8));
      row(report, "S1116 reviewed fixture: TP / FP / FN", mode -> findingAccuracy(fixture.runs().get(mode), "java:S1116", path, 11));
      row(report, "Product findings in fixture", mode -> fixture.runs().get(mode).findings().stream().filter(finding -> finding.rule().startsWith("java:")).count());
      report.append("\nA missed deprecated call is a demonstrated limitation of that mode in this fixture. Exact overload and inherited-field binding checks are reported separately below.\n\n");
    });
  }

  private static String findingAccuracy(SourceOnlyComparison.Run run, String rule, String path, int line) {
    if (!run.success()) {
      return "N/A (scan failed)";
    }
    var findings = run.findings().stream().filter(finding -> finding.rule().equals(rule)).toList();
    long matching = findings.stream().filter(finding -> finding.path().equals(path) && finding.line() != null && finding.line() == line).count();
    long truePositives = Math.min(1, matching);
    return truePositives + " / " + (findings.size() - truePositives) + " / " + (1 - truePositives);
  }

  private static String oracle(SourceOnlyComparison.Run run, String rule, String path, Set<Integer> lines, int expected) {
    if (!run.success()) {
      return "N/A (scan failed)";
    }
    long count = run.findings().stream().filter(finding -> finding.rule().equals(rule) && finding.path().equals(path)
      && finding.line() != null && lines.contains(finding.line())).count();
    return count + (count == expected ? " (correct)" : count < expected ? " (missed)" : " (unexpected)");
  }

  private static void appendFixtureSummary(StringBuilder report, List<AnalysisMatrix.Scenario> fixtures) {
    if (fixtures.isEmpty()) {
      return;
    }
    report.append("## Focused scenario results\n\nUnknown identifiers; exact binding probes establish whether selected resolutions are correct.\n\n");
    header(report, "Scenario");
    for (AnalysisMatrix.Scenario fixture : fixtures) {
      row(report, fixture.name(), mode -> {
        var run = fixture.runs().get(mode);
        return run.success() && run.semantics() != null ? run.semantics().totals().unknown() : "N/A";
      });
    }
  }

  private static void appendCosts(StringBuilder report, List<DatasetResult> datasets, List<String> activeRules) {
    report.append("\n## Benefit and cost by configuration\n\nChanges are relative to the same dataset's baseline. Timings summarize measured scans only; the min/median/max shows observed dispersion. ")
      .append(graphObservationNote(datasets))
      .append(" Full AST identifier/per-file observation and evaluation measurement hooks are included in these timings, so they are not raw production performance.\n\n")
      .append("Internal compilation runs and cleans up per Maven module. Its compiler does not receive other modules' source roots or temporary outputs; source-path resolution receives all configured roots. FAILED with external libraries can therefore expose cross-module dependencies or missing generated sources, while partial classes remain usable for that module's analysis.\n\n")
      .append("| Dataset | Mode | Scan | Comparison | Unknown Δ | Product findings | JavaSensor min / median / max (ms) | JavaSensor median Δ (ms) | Compilation | Compilation median (ms) | Generated classes | Cleanup |\n")
      .append("|---|---|---|---|---:|---:|---:|---:|---|---:|---:|---|\n");
    for (DatasetResult result : datasets) {
      for (AnalysisMode mode : AnalysisMode.values()) {
        var run = result.runs().get(mode);
        var samples = result.samples().get(mode);
        var comparison = compare(result, mode, activeRules);
        Double time = median(samples, "comparison.analyzer.time_ms");
        Double before = median(result.samples().get(AnalysisMode.BASELINE), "comparison.analyzer.time_ms");
        report.append("| ").append(escape(result.dataset().label())).append(" | ").append(mode.label())
          .append(" | ").append(samples.stream().map(sample -> sample.success() ? "SUCCESS" : "FAILED").distinct().collect(Collectors.joining(", ")))
          .append(" | ").append(comparison.valid() ? "VALID" : "INVALID")
          .append(" | ").append(comparison.valid() ? signed(comparison.candidate().semantics().totals().unknown() - comparison.current().semantics().totals().unknown()) : "N/A")
          .append(" | ").append(run.success() ? AnalysisMatrix.productRun(run).findings().size() : "N/A")
          .append(" | ").append(dispersion(samples, "comparison.analyzer.time_ms")).append(" | ").append(comparison.valid() && time != null && before != null ? signedTiming(time - before) : "N/A")
          .append(" | ").append(distinct(samples, "comparison.compilation.status", "UNAVAILABLE"))
          .append(" | ").append(timing(median(samples, "comparison.compilation.time_ms")))
          .append(" | ").append(distinct(samples, "comparison.compilation.classes", "N/A"))
          .append(" | ").append(distinct(samples, "comparison.bytecode.cleaned", "N/A")).append(" |\n");
      }
    }
    report.append("\n### JavaSensor phases\n\nThe remaining analysis phase is each sample's JavaSensor time minus compilation time. It includes parsing, rule execution, and enabled semantic observations. Invalid or missing timers give N/A; phase medians need not sum to the overall median.\n\n")
      .append("| Dataset | Mode | Compilation min / median / max (ms) | Analysis excluding compilation min / median / max (ms) |\n")
      .append("|---|---|---:|---:|\n");
    for (DatasetResult result : datasets) {
      for (AnalysisMode mode : AnalysisMode.values()) {
        var samples = result.samples().get(mode);
        report.append("| ").append(escape(result.dataset().label())).append(" | ").append(mode.label()).append(" | ")
          .append(dispersion(samples, "comparison.compilation.time_ms")).append(" | ").append(nonCompilationDispersion(samples)).append(" |\n");
      }
    }
  }

  private static String graphObservationNote(List<DatasetResult> datasets) {
    var samples = datasets.stream().flatMap(result -> result.samples().values().stream()).flatMap(List::stream).toList();
    if (samples.stream().anyMatch(run -> run.semantics() == null || run.semantics().graphTraversal() == null)) {
      return "Graph expansion limits were not recorded for every scan; recursive observation overhead cannot be established.";
    }
    var limits = samples.stream().map(run -> run.semantics().graphTraversal().limit()).distinct().sorted().toList();
    if (limits.equals(List.of(0))) {
      return "Recursive graph diagnostics are DISABLED (recorded expansion limit 0 in every measured mode), so these scans perform no recursive graph observation.";
    }
    String budgets = limits.stream().map(value -> String.format(Locale.ROOT, "%,d", value)).collect(Collectors.joining(", "));
    return "Recorded observation-only graph budget: " + budgets + " expansions per module. Enabled recursive graph reporting contributes to these timings."
      + (limits.size() > 1 ? " Graph budgets vary between scans; timing differences cannot isolate feature cost." : "");
  }

  private static void appendObservedTradeoffs(StringBuilder report, List<DatasetResult> datasets, List<String> activeRules) {
    report.append("\n## Observed tradeoffs\n\n");
    for (DatasetResult result : datasets) {
      for (AnalysisMode mode : List.of(AnalysisMode.SOURCE_PATHS, AnalysisMode.BYTECODE, AnalysisMode.COMBINED)) {
        var comparison = compare(result, mode, activeRules);
        report.append("- **").append(escape(result.dataset().label())).append(" / ").append(mode.label()).append("**: ");
        if (!comparison.valid()) {
          report.append("comparison unavailable: ").append(escape(comparison.error())).append("\n");
          continue;
        }
        int reduction = comparison.current().semantics().totals().unknown() - comparison.candidate().semantics().totals().unknown();
        report.append(reduction == 0 ? "no reduction in unknown identifiers" : reduction > 0 ? reduction + " fewer unknown identifiers" : -reduction + " more unknown identifiers");
        Double before = median(result.samples().get(AnalysisMode.BASELINE), "comparison.analyzer.time_ms");
        Double after = median(result.samples().get(mode), "comparison.analyzer.time_ms");
        if (before != null && after != null) {
          report.append("; JavaSensor median ").append(signedTiming(after - before)).append(" ms versus baseline");
        }
        if (mode.compilation()) {
          report.append("; compilation ").append(distinct(result.samples().get(mode), "comparison.compilation.status", "UNAVAILABLE"));
        }
        report.append(".\n");
      }
    }
    report.append("\nThese observations apply to the recorded source snapshot and environment. No universal winner or statistical significance is inferred.\n");
  }

  private static void appendGraphs(StringBuilder report, List<DatasetResult> datasets) {
    report.append("\n## Semantic graph diagnostics\n\nUnique symbol/type references reached during bounded traversal, which can expand as names resolve. Cells show the observed count or min–max range across measured repetitions. These are counts, not resolution-coverage percentages.\n\n")
      .append("Recursive graph inspection is disabled by default for feature timing. Optional budgeted diagnostics contribute their own overhead. An earlier reporter revision stalled on a two-file smoke scan; the target branch now fixes that recursive generic traversal, and the aborted scan is excluded. Identifier counts, per-file coverage, and product findings still cover the full analyzed AST.\n\n")
      .append("**DISABLED** means a zero expansion limit; graph counts are N/A rather than zero. **COMPLETE** means all measured traversals finished. **PARTIAL** means at least one hit the budget. **UNKNOWN** means completeness was not recorded. Location-change interpretations require complete, stable graph observations. Dataset matrices show representative graph counts.\n\n")
      .append("| Dataset | Mode | Traversal | Expansion limit / module | Expansions executed | Resolved symbols | Unknown symbols | Resolved types | Unknown types |\n")
      .append("|---|---|---|---:|---:|---:|---:|---:|---:|\n");
    for (DatasetResult result : datasets) {
      for (AnalysisMode mode : AnalysisMode.values()) {
        var samples = result.samples().get(mode);
        report.append("| ").append(escape(result.dataset().label())).append(" | ").append(mode.label()).append(" | ")
          .append(AnalysisMatrix.graphTraversalStatus(samples)).append(" | ")
          .append(traversalRange(samples, SemanticReport.GraphTraversal::limit)).append(" | ")
          .append(traversalRange(samples, SemanticReport.GraphTraversal::expansions)).append(" | ")
          .append(graphRange(samples, SemanticReport.GraphCounts::resolvedSymbols)).append(" | ")
          .append(graphRange(samples, SemanticReport.GraphCounts::unknownSymbols)).append(" | ")
          .append(graphRange(samples, SemanticReport.GraphCounts::resolvedTypes)).append(" | ")
          .append(graphRange(samples, SemanticReport.GraphCounts::unknownTypes))
          .append(" |\n");
      }
    }
  }

  private static String traversalRange(List<SourceOnlyComparison.Run> samples, Function<SemanticReport.GraphTraversal, Integer> value) {
    if (samples.stream().anyMatch(run -> !run.success() || run.semantics() == null || run.semantics().graphTraversal() == null)) {
      return "N/A";
    }
    var values = samples.stream().map(run -> value.apply(run.semantics().graphTraversal())).sorted().toList();
    return values.getFirst().equals(values.getLast()) ? values.getFirst().toString() : values.getFirst() + "–" + values.getLast();
  }

  private static String graphRange(List<SourceOnlyComparison.Run> samples, Function<SemanticReport.GraphCounts, Integer> value) {
    if (samples.stream().anyMatch(run -> !run.success() || run.semantics() == null || run.semantics().graphTotals() == null
      || run.semantics().graphTraversal() != null && run.semantics().graphTraversal().limit() == 0)) {
      return "N/A";
    }
    var values = samples.stream().map(run -> value.apply(run.semantics().graphTotals())).sorted().toList();
    return values.getFirst().equals(values.getLast()) ? values.getFirst().toString() : values.getFirst() + "–" + values.getLast();
  }

  private static SourceOnlyComparison.Comparison compare(DatasetResult result, AnalysisMode mode, List<String> activeRules) {
    var runs = result.runs();
    var baseline = AnalysisMatrix.productRun(runs.get(AnalysisMode.BASELINE));
    var candidate = AnalysisMatrix.productRun(runs.get(mode));
    if (!result.stable()) {
      return new SourceOnlyComparison.Comparison(false, false, "Measured repetitions are unstable; comparison metrics are unavailable.",
        baseline, candidate, List.of(), List.of(), List.of());
    }
    return SourceOnlyComparison.compare(baseline, candidate, false, activeRules);
  }

  private static Double median(List<SourceOnlyComparison.Run> runs, String key) {
    var sorted = timingValues(runs, key);
    return median(sorted);
  }

  private static Double median(List<Double> sorted) {
    if (sorted == null) {
      return null;
    }
    int middle = sorted.size() / 2;
    return sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2 : sorted.get(middle);
  }

  private static String dispersion(List<SourceOnlyComparison.Run> runs, String key) {
    return dispersion(timingValues(runs, key));
  }

  private static String dispersion(List<Double> sorted) {
    return sorted == null ? "N/A" : timing(sorted.getFirst()) + " / " + timing(median(sorted)) + " / " + timing(sorted.getLast());
  }

  private static String nonCompilationDispersion(List<SourceOnlyComparison.Run> runs) {
    var values = runs.stream().map(run -> {
      Double sensor = number(run, "comparison.analyzer.time_ms");
      Double compiler = number(run, "comparison.compilation.time_ms");
      return sensor == null || compiler == null || sensor < compiler ? null : sensor - compiler;
    }).toList();
    return dispersion(sortedTimings(values));
  }

  private static List<Double> timingValues(List<SourceOnlyComparison.Run> runs, String key) {
    return sortedTimings(runs.stream().map(run -> number(run, key)).toList());
  }

  private static List<Double> sortedTimings(List<Double> values) {
    if (values.isEmpty() || values.stream().anyMatch(value -> value == null)) {
      return null;
    }
    return values.stream().sorted().toList();
  }

  private static Double number(SourceOnlyComparison.Run run, String key) {
    try {
      double value = Double.parseDouble(run.telemetry().getOrDefault(key, ""));
      return Double.isFinite(value) && value >= 0 ? value : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String distinct(List<SourceOnlyComparison.Run> runs, String key, String fallback) {
    return runs.stream().map(run -> run.telemetry().getOrDefault(key, fallback)).distinct().collect(Collectors.joining(", "));
  }

  private static void appendMetadata(StringBuilder report, Map<String, String> metadata) {
    if (!metadata.isEmpty()) {
      report.append("\n## Run metadata\n\n| Setting | Value |\n|---|---|\n");
      metadata.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> report.append("| ")
        .append(escape(entry.getKey())).append(" | ").append(escape(entry.getValue())).append(" |\n"));
    }
  }

  private static void header(StringBuilder report, String label) {
    report.append("| ").append(label);
    for (AnalysisMode mode : AnalysisMode.values()) {
      report.append(" | ").append(mode.label());
    }
    report.append(" |\n|---|---:|---:|---:|---:|\n");
  }

  private static void row(StringBuilder report, String label, Function<AnalysisMode, Object> value) {
    report.append("| ").append(escape(label));
    for (AnalysisMode mode : AnalysisMode.values()) {
      report.append(" | ").append(escape(String.valueOf(value.apply(mode))));
    }
    report.append(" |\n");
  }

  private static String signed(int value) {
    return String.format(Locale.ROOT, "%+d", value);
  }

  private static String signedTiming(double value) {
    return String.format(Locale.ROOT, "%+.1f", value);
  }

  private static String percentage(Double value) {
    return value == null ? "N/A" : String.format(Locale.ROOT, "%.3f%%", value);
  }

  private static String timing(Double value) {
    return value == null ? "N/A" : String.format(Locale.ROOT, "%.1f", value);
  }

  private static String escape(String value) {
    return value.replace("|", "\\|").replace('\n', ' ').replace('\r', ' ');
  }

  private static String html(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
