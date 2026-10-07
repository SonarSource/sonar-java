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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class AnalysisMatrix {

  record Scenario(String name, String description, Map<AnalysisMode, SourceOnlyComparison.Run> runs) {
    Scenario {
      requireModes(runs);
      runs = Map.copyOf(runs);
    }
  }

  private record Pair(AnalysisMode current, AnalysisMode candidate) {
    String title() {
      return candidate.label() + " versus " + current.label();
    }
  }

  private record UnknownOccurrence(String path, SemanticReport.UnknownIdentifier identifier) {
    UnknownKey key() {
      return new UnknownKey(path, identifier.name(), identifier.range());
    }
  }

  private record UnknownKey(String path, String name, String range) {
  }

  private static final List<Pair> PAIRS = List.of(
    new Pair(AnalysisMode.BASELINE, AnalysisMode.SOURCE_PATHS),
    new Pair(AnalysisMode.BASELINE, AnalysisMode.BYTECODE),
    new Pair(AnalysisMode.BASELINE, AnalysisMode.COMBINED),
    new Pair(AnalysisMode.SOURCE_PATHS, AnalysisMode.COMBINED),
    new Pair(AnalysisMode.BYTECODE, AnalysisMode.COMBINED));
  private static final Pattern BINDING_COMPLETION = Pattern.compile("Semantic binding probes: (\\d+) checked, (\\d+) failed\\.");

  private AnalysisMatrix() {
  }

  static void write(Path directory, Map<AnalysisMode, List<SourceOnlyComparison.Run>> samples,
                    List<String> activeRules, Map<String, String> metadata, List<Scenario> scenarios) throws IOException {
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("report.md"), markdown(samples, activeRules, metadata, scenarios));
  }

  static String markdown(Map<AnalysisMode, List<SourceOnlyComparison.Run>> samples,
                         List<String> activeRules, Map<String, String> metadata, List<Scenario> scenarios) {
    return markdown(samples, activeRules, metadata, scenarios, true);
  }

  static String detailsMarkdown(Map<AnalysisMode, List<SourceOnlyComparison.Run>> samples,
                                List<String> activeRules, Map<String, String> metadata) {
    return markdown(samples, activeRules, metadata, List.of(), false);
  }

  private static String markdown(Map<AnalysisMode, List<SourceOnlyComparison.Run>> measured,
                                 List<String> activeRules, Map<String, String> metadata, List<Scenario> scenarios, boolean standalone) {
    requireSamples(measured);
    var samples = measured.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
      entry -> entry.getValue().stream().map(AnalysisMatrix::productRun).toList()));
    Map<AnalysisMode, SourceOnlyComparison.Run> runs = representatives(samples);
    String stabilityError = "unstable".equalsIgnoreCase(metadata.get("Repeated result stability"))
      ? "Measured repetitions are unstable; comparison and agreement metrics are unavailable." : null;
    var report = new StringBuilder();
    if (standalone) {
      report.append("# Java source-path and internal-compilation comparison\n\n")
        .append("**Runner:** Orchestrator MavenBuild (`sonar:sonar`). Every mode uses the same analyzer artifact and source copies, without supplied project bytecode. External dependency JARs, when supplied, are identical across all modes within a dataset.\n\n")
        .append("Source paths resolve project declarations from source (#6308); internal compilation creates temporary project bytecode before analysis (#6309).\n\n");
    }
    appendSummary(report, samples, runs);
    appendModuleMatrix(report, runs);
    if (stabilityError != null) {
      report.append("\n**Comparison unavailable:** ").append(stabilityError).append("\n");
    }
    boolean graphStable = !"variable".equalsIgnoreCase(metadata.get("Graph diagnostic stability"));
    if (!graphStable) {
      report.append("\n**Graph diagnostics vary between measured repetitions.** Graph counts above use the first representative sample; primary identifier/finding comparisons remain independent. Graph-location change interpretations are unavailable.\n");
    }
    appendScenarios(report, scenarios);
    appendPairSummary(report, runs, activeRules, stabilityError);
    appendAstContexts(report, runs);
    appendTopUnknownFiles(report, runs);
    appendFileMatrix(report, runs);
    appendRuleMatrix(report, runs, activeRules);
    var completeGraphModes = samples.entrySet().stream().filter(entry -> graphTraversalStatus(entry.getValue()).equals("COMPLETE"))
      .map(Map.Entry::getKey).collect(Collectors.toSet());
    appendPairDetails(report, runs, activeRules, stabilityError, graphStable, completeGraphModes);
    appendTimings(report, samples);
    appendMetadata(report, metadata);
    if (standalone) {
      report.append("\n## Reading the data\n\n")
      .append("- Scan success and compilation success are separate outcomes. A failed compilation can still produce a successful analysis. Missing compiler measurements are UNAVAILABLE or N/A, never assumed successful.\n")
      .append("- Identifier counts come from the semantic report: known = total − unknown. Project unknown percentage uses aggregate counts, not an average of file percentages. Zero identifiers means N/A.\n")
      .append("- Comparisons require successful scans, identical indexed files, identical module coverage and module identifier totals, and identical global identifier totals. When both reports have per-file observations, file coverage and identifier totals must also match. Invalid pairs have no change or agreement metrics.\n")
      .append("- Canonical module counts remain available without optional per-file observations. Missing per-file counts, rankings, and occurrence details are UNAVAILABLE rather than zero.\n")
      .append("- Unknown occurrences match by file, name, and token range. No longer reported unknown does not prove correct resolution; the focused fixtures check exact bindings separately. AST context is diagnostic, not a semantic classification.\n")
      .append("- Findings match by rule, path, and line, preserving duplicates. Retention measures agreement rather than accuracy; zero findings do not prove a configured rule ran.\n")
      .append("- JavaSensor time includes internal compilation. Maven/server wall time also includes startup and server processing. Medians exclude warm-ups; missing measurements are N/A.\n")
        .append("- Semantic graph counts describe unique references reached during bounded traversal. A newly resolved identifier can expose more graph nodes; graph counts are diagnostics, not coverage percentages or correctness scores.\n")
        .append("- Recursive graph diagnostics are DISABLED when the recorded expansion limit is zero; graph counts then show N/A. Otherwise traversal is COMPLETE only when every measured observation confirms completion, PARTIAL when the budget is reached, and UNKNOWN when completeness metadata is missing. These graph states do not truncate AST identifier or product-finding measurements. Graph-location change interpretations require complete, stable observations.\n")
        .append("- Source paths do not expand analysis scope. Annotation processing is disabled during internal compilation. Probe findings are instrumentation and are excluded from product finding counts and agreement.\n");
    }
    return report.toString();
  }

  static void requireModes(Map<AnalysisMode, ?> values) {
    if (!values.keySet().equals(Set.of(AnalysisMode.values())) || values.values().stream().anyMatch(value -> value == null)) {
      throw new IllegalArgumentException("All four analysis modes are required");
    }
  }

  static void requireSamples(Map<AnalysisMode, List<SourceOnlyComparison.Run>> samples) {
    requireModes(samples);
    samples.forEach((mode, runs) -> {
      if (runs.isEmpty() || runs.stream().anyMatch(run -> run == null)) {
        throw new IllegalArgumentException("Measured samples are required for " + mode.label());
      }
    });
  }

  static Map<AnalysisMode, SourceOnlyComparison.Run> representatives(Map<AnalysisMode, List<SourceOnlyComparison.Run>> samples) {
    requireSamples(samples);
    return samples.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
      entry -> entry.getValue().stream().filter(run -> !run.success()).findFirst().orElse(entry.getValue().getFirst())));
  }

  static SourceOnlyComparison.Run productRun(SourceOnlyComparison.Run run) {
    return new SourceOnlyComparison.Run(run.label(), run.success(), run.scanMillis(), run.files(),
      run.findings().stream().filter(finding -> finding.rule().startsWith("java:")).toList(), run.telemetry(), run.semantics(), run.error());
  }

  private static void appendSummary(StringBuilder report, Map<AnalysisMode, List<SourceOnlyComparison.Run>> samples,
                                    Map<AnalysisMode, SourceOnlyComparison.Run> runs) {
    report.append("## Summary\n\n");
    appendHeader(report, "Metric");
    appendRow(report, "Source paths enabled", mode -> mode.sourcePaths() ? "YES" : "NO");
    appendRow(report, "Internal compilation enabled", mode -> mode.compilation() ? "YES" : "NO");
    appendRow(report, "Scan status", mode -> distinct(samples.get(mode), run -> run.success() ? "SUCCESS" : "FAILED"));
    appendRow(report, "Java files analyzed", mode -> runs.get(mode).files().size());
    appendRow(report, "Identifiers (total)", mode -> counts(runs.get(mode), SemanticReport.Counts::total));
    appendRow(report, "Known identifiers", mode -> counts(runs.get(mode), SemanticReport.Counts::known));
    appendRow(report, "Unknown identifiers", mode -> counts(runs.get(mode), SemanticReport.Counts::unknown));
    appendRow(report, "Unknown identifiers (%)", mode -> percentage(runs.get(mode).semantics() == null ? null : runs.get(mode).semantics().totals().unknownPercentage()));
    appendRow(report, "Semantic graph resolved symbols", mode -> graphCounts(runs.get(mode), SemanticReport.GraphCounts::resolvedSymbols));
    appendRow(report, "Semantic graph unknown symbols", mode -> graphCounts(runs.get(mode), SemanticReport.GraphCounts::unknownSymbols));
    appendRow(report, "Semantic graph resolved types", mode -> graphCounts(runs.get(mode), SemanticReport.GraphCounts::resolvedTypes));
    appendRow(report, "Semantic graph unknown types", mode -> graphCounts(runs.get(mode), SemanticReport.GraphCounts::unknownTypes));
    appendRow(report, "Semantic graph traversal", mode -> graphTraversalStatus(samples.get(mode)));
    appendRow(report, "Graph expansion limit per module", mode -> graphTraversalCounts(runs.get(mode), SemanticReport.GraphTraversal::limit));
    appendRow(report, "Graph expansions executed (representative sample)", mode -> graphTraversalCounts(runs.get(mode), SemanticReport.GraphTraversal::expansions));
    appendRow(report, "Per-file semantic observations", mode -> {
      var semantics = runs.get(mode).semantics();
      return semantics == null ? "N/A" : semantics.hasFileObservations() ? "AVAILABLE" : "UNAVAILABLE";
    });
    appendRow(report, "Files with no unknown identifiers", mode -> filesWithNoUnknowns(runs.get(mode).semantics()));
    appendRow(report, "Findings", mode -> runs.get(mode).findings().size());
    appendRow(report, "Median Maven/server time (ms)", mode -> median(samples.get(mode).stream().map(run -> (double) run.scanMillis()).toList()));
    appendRow(report, "Median JavaSensor time, including compilation (ms)", mode -> telemetryMedian(samples.get(mode), "comparison.analyzer.time_ms"));
    appendRow(report, "Compilation outcome", mode -> distinct(samples.get(mode), run -> telemetry(run, "comparison.compilation.status", "UNAVAILABLE")));
    appendRow(report, "Main compilation modules (success / failed / skipped, including empty aggregators)", mode -> mode.compilation()
      ? List.of("success", "failed", "skipped").stream()
        .map(outcome -> telemetry(runs.get(mode), "comparison.compilation.main." + outcome, "N/A"))
        .collect(Collectors.joining(" / ")) : "DISABLED");
    appendRow(report, "Median internal compilation time (ms)", mode -> telemetryMedian(samples.get(mode), "comparison.compilation.time_ms"));
    appendRow(report, "Compilation source files", mode -> telemetry(runs.get(mode), "comparison.compilation.sources", "N/A"));
    appendRow(report, "Generated class files before cleanup", mode -> telemetry(runs.get(mode), "comparison.compilation.classes", "N/A"));
    appendRow(report, "Temporary bytecode cleaned up", mode -> distinct(samples.get(mode), AnalysisMatrix::cleanup));
    appendRow(report, "Source characters analyzed", mode -> telemetry(runs.get(mode), "java.analysis.main.success.size_chars", "N/A"));
    appendRow(report, "Undefined-type errors", mode -> telemetry(runs.get(mode), "java.analysis.main.success.type_error_count", "N/A"));
    appendRow(report, "Source characters with parse errors", mode -> telemetry(runs.get(mode), "java.analysis.main.parse_errors.size_chars", "N/A"));
    appendRow(report, "Source characters with analysis exceptions", mode -> telemetry(runs.get(mode), "java.analysis.main.exceptions.size_chars", "N/A"));
  }

  static void appendScenarios(StringBuilder report, List<Scenario> scenarios) {
    if (scenarios.isEmpty()) {
      return;
    }
    report.append("\n## Focused correctness scenarios\n\n");
    for (Scenario scenario : scenarios) {
      report.append("### ").append(escape(scenario.name())).append("\n\n").append(escape(scenario.description())).append("\n\n");
      appendHeader(report, "Metric");
      appendRow(report, "Scan status", mode -> scenario.runs().get(mode).success() ? "SUCCESS" : "FAILED");
      appendRow(report, "Files analyzed", mode -> scenario.runs().get(mode).files().size());
      appendRow(report, "Identifiers (total)", mode -> counts(scenario.runs().get(mode), SemanticReport.Counts::total));
      appendRow(report, "Known identifiers", mode -> counts(scenario.runs().get(mode), SemanticReport.Counts::known));
      appendRow(report, "Unknown identifiers", mode -> counts(scenario.runs().get(mode), SemanticReport.Counts::unknown));
      appendRow(report, "Compilation outcome", mode -> telemetry(scenario.runs().get(mode), "comparison.compilation.status", "UNAVAILABLE"));
      appendRow(report, "Generated class files before cleanup", mode -> telemetry(scenario.runs().get(mode), "comparison.compilation.classes", "N/A"));
      appendRow(report, "Temporary bytecode cleaned up", mode -> cleanup(scenario.runs().get(mode)));
      appendRow(report, "Exact binding probe completion", mode -> probeMessages(scenario.runs().get(mode), "Semantic binding probes:"));
      appendRow(report, "Exact binding probes checked", mode -> probeCount(scenario.runs().get(mode), 1));
      appendRow(report, "Exact binding probe failures", mode -> probeCount(scenario.runs().get(mode), 2));
      appendRow(report, "Bytecode during analysis", mode -> probeMessages(scenario.runs().get(mode), "Generated bytecode probe:"));
      appendRow(report, "Coverage versus baseline", mode -> {
        var comparison = SourceOnlyComparison.compare(scenario.runs().get(AnalysisMode.BASELINE), scenario.runs().get(mode), false, List.of());
        return comparison.valid() ? "VALID" : "INVALID: " + comparison.error();
      });
    }
  }

  private static String probeMessages(SourceOnlyComparison.Run run, String prefix) {
    var messages = run.findings().stream().map(SourceOnlyComparison.Finding::message).filter(message -> message.startsWith(prefix)).toList();
    return messages.isEmpty() ? "N/A" : String.join("; ", messages);
  }

  private static Object probeCount(SourceOnlyComparison.Run run, int group) {
    var completions = run.findings().stream().map(SourceOnlyComparison.Finding::message).map(BINDING_COMPLETION::matcher)
      .filter(matcher -> matcher.matches()).toList();
    return completions.isEmpty() ? "N/A" : completions.stream().mapToInt(matcher -> Integer.parseInt(matcher.group(group))).sum();
  }

  private static void appendPairSummary(StringBuilder report, Map<AnalysisMode, SourceOnlyComparison.Run> runs,
                                        List<String> activeRules, String stabilityError) {
    report.append("\n### Feature comparisons\n\nThe mode after 'versus' is the reference. Changes subtract its counts from the compared mode's counts.\n\n")
      .append("| Comparison | Status | Unknown count change | Unknown % change (pp) | Files improved / unchanged / regressed | Shared findings | Reference only | Compared only | Retention |\n")
      .append("|---|---|---:|---:|---:|---:|---:|---:|---:|\n");
    for (Pair pair : PAIRS) {
      var comparison = compare(pair, runs, activeRules, stabilityError);
      report.append("| ").append(pair.title()).append(" | ").append(comparison.valid() ? "VALID" : "INVALID");
      if (comparison.valid()) {
        var before = comparison.current().semantics().totals();
        var after = comparison.candidate().semantics().totals();
        int shared = comparison.current().findings().size() - comparison.currentOnly().size();
        report.append(" | ").append(String.format(Locale.ROOT, "%+d", after.unknown() - before.unknown()))
          .append(" | ").append(change(before.unknownPercentage(), after.unknownPercentage()))
          .append(" | ").append(fileChanges(comparison.current().semantics(), comparison.candidate().semantics()))
          .append(" | ").append(shared).append(" | ").append(comparison.currentOnly().size()).append(" | ").append(comparison.candidateOnly().size())
          .append(" | ").append(percentage(comparison.current().findings().isEmpty() ? null : 100.0 * shared / comparison.current().findings().size()));
      } else {
        report.append(" | N/A | N/A | N/A | N/A | N/A | N/A | N/A");
      }
      report.append(" |\n");
    }
  }

  private static String fileChanges(SemanticReport current, SemanticReport candidate) {
    if (!SourceOnlyComparison.fileObservationsAvailable(current, candidate)) {
      return "UNAVAILABLE";
    }
    int improved = 0;
    int unchanged = 0;
    int regressed = 0;
    for (String path : current.files().keySet()) {
      Double before = current.files().get(path).unknownPercentage();
      Double after = candidate.files().get(path).unknownPercentage();
      if (before == null || after == null) {
        continue;
      }
      if (after < before) {
        improved++;
      } else if (after > before) {
        regressed++;
      } else {
        unchanged++;
      }
    }
    return improved + " / " + unchanged + " / " + regressed;
  }

  private static SourceOnlyComparison.Comparison compare(Pair pair, Map<AnalysisMode, SourceOnlyComparison.Run> runs,
                                                        List<String> activeRules, String stabilityError) {
    if (stabilityError != null) {
      return new SourceOnlyComparison.Comparison(false, false, stabilityError, runs.get(pair.current()), runs.get(pair.candidate()),
        List.of(), List.of(), List.of());
    }
    return SourceOnlyComparison.compare(runs.get(pair.current()), runs.get(pair.candidate()), false, activeRules);
  }

  private static void appendAstContexts(StringBuilder report, Map<AnalysisMode, SourceOnlyComparison.Run> runs) {
    report.append("\n## Unknown occurrences by AST context\n\nContext is the identifier's AST parent kind. Modes without occurrence details are UNAVAILABLE.\n\n");
    if (runs.values().stream().noneMatch(run -> run.semantics() != null && run.semantics().hasUnknownDetails())) {
      report.append("UNAVAILABLE: unknown occurrence details are absent in all modes.\n");
      return;
    }
    var contexts = new TreeSet<String>();
    runs.values().stream().filter(run -> run.semantics() != null && run.semantics().hasUnknownDetails())
      .forEach(run -> unknownOccurrences(run.semantics()).forEach(value -> contexts.add(value.identifier().parentKind())));
    if (contexts.isEmpty()) {
      report.append("No unknown contexts available.\n");
      return;
    }
    appendHeader(report, "AST context");
    for (String context : contexts) {
      appendRow(report, context, mode -> {
        var semantics = runs.get(mode).semantics();
        return semantics == null ? "N/A" : !semantics.hasUnknownDetails() ? "UNAVAILABLE"
          : unknownOccurrences(semantics).stream().filter(value -> value.identifier().parentKind().equals(context)).count();
      });
    }
  }

  private static void appendTopUnknownFiles(StringBuilder report, Map<AnalysisMode, SourceOnlyComparison.Run> runs) {
    report.append("\n## Top files contributing unknown identifiers\n\nTop five by the largest unknown count in any mode. Cells show **unknown count (share of project unknowns)**.\n\n");
    if (runs.values().stream().noneMatch(run -> run.semantics() != null && run.semantics().hasFileObservations())) {
      report.append("UNAVAILABLE: per-file observations are absent in all modes.\n");
      return;
    }
    var paths = new TreeSet<String>();
    runs.values().stream().filter(run -> run.semantics() != null && run.semantics().hasFileObservations())
      .forEach(run -> paths.addAll(run.semantics().files().keySet()));
    var contributors = paths.stream().filter(path -> maximumUnknown(runs, path) > 0)
      .sorted(Comparator.comparingInt((String path) -> maximumUnknown(runs, path)).reversed().thenComparing(Comparator.naturalOrder())).limit(5).toList();
    if (contributors.isEmpty()) {
      report.append("None.\n");
      return;
    }
    appendHeader(report, "File");
    for (String path : contributors) {
      appendRow(report, path, mode -> {
        var semantics = runs.get(mode).semantics();
        if (semantics != null && !semantics.hasFileObservations()) {
          return "UNAVAILABLE";
        }
        var counts = semantics == null ? null : semantics.files().get(path);
        return counts == null ? "N/A" : counts.unknown() + " (" + percentage(semantics.totals().unknown() == 0 ? null
          : 100.0 * counts.unknown() / semantics.totals().unknown()) + ")";
      });
    }
  }

  private static int maximumUnknown(Map<AnalysisMode, SourceOnlyComparison.Run> runs, String path) {
    return runs.values().stream().filter(run -> run.semantics() != null && run.semantics().hasFileObservations() && run.semantics().files().containsKey(path))
      .mapToInt(run -> run.semantics().files().get(path).unknown()).max().orElse(0);
  }

  private static void appendFileMatrix(StringBuilder report, Map<AnalysisMode, SourceOnlyComparison.Run> runs) {
    report.append("\n## Semantics per file\n\nEach cell shows **total / known / unknown (unknown %)**. Absent observations are UNAVAILABLE; missing coverage in an observed mode is N/A.\n\n");
    if (runs.values().stream().noneMatch(run -> run.semantics() != null && run.semantics().hasFileObservations())) {
      report.append("UNAVAILABLE: per-file observations are absent in all modes.\n");
      return;
    }
    appendHeader(report, "File");
    var paths = new TreeSet<String>();
    runs.values().stream().filter(run -> run.semantics() != null && run.semantics().hasFileObservations())
      .forEach(run -> paths.addAll(run.semantics().files().keySet()));
    for (String path : paths) {
      appendRow(report, path, mode -> {
        var semantics = runs.get(mode).semantics();
        if (semantics != null && !semantics.hasFileObservations()) {
          return "UNAVAILABLE";
        }
        var counts = semantics == null ? null : semantics.files().get(path);
        return counts == null ? "N/A" : counts.total() + " / " + counts.known() + " / " + counts.unknown() + " (" + percentage(counts.unknownPercentage()) + ")";
      });
    }
  }

  private static void appendModuleMatrix(StringBuilder report, Map<AnalysisMode, SourceOnlyComparison.Run> runs) {
    var modules = new TreeSet<String>();
    runs.values().stream().filter(run -> run.semantics() != null)
      .forEach(run -> modules.addAll(run.semantics().moduleIdentifierCounts().keySet()));
    if (modules.isEmpty()) {
      return;
    }
    report.append("\n## Semantics per module\n\nEach cell shows **total / known / unknown (unknown %)** from canonical module counts. Empty aggregator modules retain their actual zero counts.\n\n");
    appendHeader(report, "Module");
    for (String module : modules) {
      appendRow(report, module, mode -> {
        var semantics = runs.get(mode).semantics();
        var counts = semantics == null ? null : semantics.moduleIdentifierCounts().get(module);
        return counts == null ? "N/A" : counts.total() + " / " + counts.known() + " / " + counts.unknown() + " (" + percentage(counts.unknownPercentage()) + ")";
      });
    }
  }

  private static void appendRuleMatrix(StringBuilder report, Map<AnalysisMode, SourceOnlyComparison.Run> runs, List<String> activeRules) {
    report.append("\n## Rules with findings\n\n");
    appendHeader(report, "Rule");
    var rules = new TreeSet<>(activeRules);
    runs.values().forEach(run -> run.findings().forEach(finding -> rules.add(finding.rule())));
    int omitted = 0;
    for (String rule : rules) {
      if (runs.values().stream().allMatch(run -> findingCount(run, rule) == 0)) {
        omitted++;
      } else {
        appendRow(report, rule, mode -> findingCount(runs.get(mode), rule));
      }
    }
    report.append("\n").append(omitted).append(" configured rules had no findings in any mode and are omitted.\n");
  }

  private static long findingCount(SourceOnlyComparison.Run run, String rule) {
    return run.findings().stream().filter(finding -> finding.rule().equals(rule)).count();
  }

  private static void appendPairDetails(StringBuilder report, Map<AnalysisMode, SourceOnlyComparison.Run> runs,
                                        List<String> activeRules, String stabilityError, boolean graphStable, Set<AnalysisMode> completeGraphModes) {
    report.append("\n## Detailed differences\n\n");
    for (Pair pair : PAIRS) {
      var comparison = compare(pair, runs, activeRules, stabilityError);
      report.append("<details>\n<summary>").append(pair.title()).append("</summary>\n\n");
      if (!comparison.valid()) {
        report.append("Comparison unavailable: ").append(escape(comparison.error())).append("\n\n");
      } else {
        appendRankedChanges(report, comparison.current().semantics(), comparison.candidate().semantics(), pair, true);
        appendRankedChanges(report, comparison.current().semantics(), comparison.candidate().semantics(), pair, false);
        appendUnknownDifferences(report, comparison.current().semantics(), comparison.candidate().semantics());
        if (graphStable && completeGraphModes.contains(pair.current()) && completeGraphModes.contains(pair.candidate())) {
          appendGraphDifferences(report, comparison.current().semantics(), comparison.candidate().semantics());
        } else if (!graphStable) {
          report.append("\nSemantic graph location differences unavailable: graph diagnostics vary between measured repetitions.\n\n");
        } else {
          report.append("\nSemantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.\n\n");
        }
        appendFindings(report, "Findings only in " + pair.current().label(), comparison.currentOnly());
        appendFindings(report, "Findings only in " + pair.candidate().label(), comparison.candidateOnly());
      }
      report.append("</details>\n\n");
    }
    runs.forEach((mode, run) -> {
      if (run.error() != null) {
        report.append("### ").append(mode.label()).append(" scan failure\n\n").append(escape(run.error())).append("\n\n");
      }
    });
  }

  private static void appendGraphDifferences(StringBuilder report, SemanticReport current, SemanticReport candidate) {
    report.append("\n### Semantic graph location differences\n\n")
      .append("Locations identify unknown graph references reached during traversal. Expanded traversal can add locations; removals are not proof of correct bindings.\n\n");
    if (current.graphTotals() == null || candidate.graphTotals() == null) {
      report.append("Unavailable: at least one report does not contain semantic graph measurements.\n\n");
      return;
    }
    appendGraphLocations(report, "Unknown symbol", current.unknownSymbols(), candidate.unknownSymbols());
    appendGraphLocations(report, "Unknown type", current.unknownTypes(), candidate.unknownTypes());
  }

  private static void appendGraphLocations(StringBuilder report, String name, List<String> current, List<String> candidate) {
    var before = Set.copyOf(current);
    var after = Set.copyOf(candidate);
    var removed = before.stream().filter(location -> !after.contains(location)).sorted().toList();
    var added = after.stream().filter(location -> !before.contains(location)).sorted().toList();
    report.append("| ").append(name).append(" graph locations | Count |\n|---|---:|\n")
      .append("| Unknown locations present in both observations | ").append(before.size() - removed.size()).append(" |\n")
      .append("| Unknown locations absent from candidate observation | ").append(removed.size()).append(" |\n")
      .append("| Unknown locations absent from baseline observation | ").append(added.size()).append(" |\n\n");
    appendGraphLocationList(report, name + " locations absent from candidate observation", removed);
    appendGraphLocationList(report, name + " locations absent from baseline observation", added);
  }

  private static void appendGraphLocationList(StringBuilder report, String title, List<String> locations) {
    report.append("#### ").append(title).append("\n\n");
    if (locations.isEmpty()) {
      report.append("None.\n\n");
      return;
    }
    report.append("| Location |\n|---|\n");
    locations.stream().limit(20).forEach(location -> report.append("| ").append(escape(location)).append(" |\n"));
    if (locations.size() > 20) {
      report.append("\n").append(locations.size() - 20).append(" additional graph locations omitted.\n");
    }
    report.append('\n');
  }

  private static void appendRankedChanges(StringBuilder report, SemanticReport current, SemanticReport candidate, Pair pair, boolean improvements) {
    report.append("### Largest semantic ").append(improvements ? "improvements" : "regressions")
      .append("\n\nTop five by absolute change in unknown percentage.\n\n");
    if (!SourceOnlyComparison.fileObservationsAvailable(current, candidate)) {
      report.append("UNAVAILABLE: per-file observations are absent in at least one compared mode.\n\n");
      return;
    }
    var paths = current.files().keySet().stream().filter(path -> {
      Double before = current.files().get(path).unknownPercentage();
      Double after = candidate.files().get(path).unknownPercentage();
      return before != null && after != null && (improvements ? after < before : after > before);
    }).sorted(Comparator.comparingDouble((String path) -> Math.abs(candidate.files().get(path).unknownPercentage()
      - current.files().get(path).unknownPercentage())).reversed().thenComparing(Comparator.naturalOrder())).limit(5).toList();
    if (paths.isEmpty()) {
      report.append("None.\n\n");
      return;
    }
    report.append("| File | ").append(pair.current().label()).append(" unknown % | ").append(pair.candidate().label())
      .append(" unknown % | Change (pp) |\n|---|---:|---:|---:|\n");
    for (String path : paths) {
      Double before = current.files().get(path).unknownPercentage();
      Double after = candidate.files().get(path).unknownPercentage();
      report.append("| ").append(escape(path)).append(" | ").append(percentage(before)).append(" | ").append(percentage(after))
        .append(" | ").append(change(before, after)).append(" |\n");
    }
    report.append('\n');
  }

  private static void appendUnknownDifferences(StringBuilder report, SemanticReport current, SemanticReport candidate) {
    if (!current.hasUnknownDetails() || !candidate.hasUnknownDetails()) {
      report.append("Unknown occurrence differences UNAVAILABLE: details are absent in at least one compared mode.\n\n");
      return;
    }
    var before = unknownOccurrences(current);
    var after = unknownOccurrences(candidate);
    var beforeKeys = before.stream().map(UnknownOccurrence::key).collect(Collectors.toSet());
    var afterKeys = after.stream().map(UnknownOccurrence::key).collect(Collectors.toSet());
    var removed = before.stream().filter(value -> !afterKeys.contains(value.key())).toList();
    var added = after.stream().filter(value -> !beforeKeys.contains(value.key())).toList();
    report.append("| Unknown occurrences | Count |\n|---|---:|\n")
      .append("| Still unknown in both modes | ").append(before.size() - removed.size()).append(" |\n")
      .append("| No longer reported unknown | ").append(removed.size()).append(" |\n")
      .append("| Newly reported unknown | ").append(added.size()).append(" |\n");
    appendUnknownList(report, "No longer reported unknown", removed);
    appendUnknownList(report, "Newly reported unknown", added);
  }

  private static List<UnknownOccurrence> unknownOccurrences(SemanticReport semantics) {
    return semantics.unknownIdentifiers().entrySet().stream()
      .flatMap(entry -> entry.getValue().stream().map(identifier -> new UnknownOccurrence(entry.getKey(), identifier)))
      .sorted(Comparator.comparing(UnknownOccurrence::path).thenComparing(value -> value.identifier().range()).thenComparing(value -> value.identifier().name())).toList();
  }

  private static void appendUnknownList(StringBuilder report, String title, List<UnknownOccurrence> values) {
    report.append("\n### ").append(title).append("\n\n");
    if (values.isEmpty()) {
      report.append("None.\n\n");
      return;
    }
    report.append("| File | Range | Identifier | AST context |\n|---|---|---|---|\n");
    values.stream().limit(20).forEach(value -> report.append("| ").append(escape(value.path())).append(" | ")
      .append(escape(value.identifier().range())).append(" | ").append(escape(value.identifier().name())).append(" | ")
      .append(escape(value.identifier().parentKind())).append(" |\n"));
    if (values.size() > 20) {
      report.append("\n").append(values.size() - 20).append(" additional occurrences omitted.\n");
    }
    report.append('\n');
  }

  private static void appendFindings(StringBuilder report, String title, List<SourceOnlyComparison.Finding> findings) {
    report.append("### ").append(title).append("\n\n");
    if (findings.isEmpty()) {
      report.append("None.\n\n");
      return;
    }
    report.append("| Rule | File | Line | Message |\n|---|---|---:|---|\n");
    findings.forEach(finding -> report.append("| ").append(escape(finding.rule())).append(" | ").append(escape(finding.path()))
      .append(" | ").append(finding.line() == null ? "N/A" : finding.line()).append(" | ").append(escape(finding.message())).append(" |\n"));
    report.append('\n');
  }

  private static void appendTimings(StringBuilder report, Map<AnalysisMode, List<SourceOnlyComparison.Run>> samples) {
    report.append("\n## Measured timings\n\nWarm-ups are excluded. JavaSensor time includes compilation.\n\n")
      .append("| Sample | Mode | Maven/server wall (ms) | JavaSensor (ms) | Compilation (ms) | Compilation outcome | Generated classes |\n")
      .append("|---|---|---:|---:|---:|---|---:|\n");
    int repetitions = samples.values().stream().mapToInt(List::size).max().orElse(0);
    for (int i = 0; i < repetitions; i++) {
      for (AnalysisMode mode : AnalysisMode.values()) {
        if (i >= samples.get(mode).size()) {
          continue;
        }
        var run = samples.get(mode).get(i);
        report.append("| ").append(i + 1).append(" | ").append(mode.label()).append(" | ").append(run.scanMillis())
          .append(" | ").append(timing(number(run, "comparison.analyzer.time_ms")))
          .append(" | ").append(timing(number(run, "comparison.compilation.time_ms")))
          .append(" | ").append(telemetry(run, "comparison.compilation.status", "UNAVAILABLE"))
          .append(" | ").append(telemetry(run, "comparison.compilation.classes", "N/A")).append(" |\n");
      }
    }
  }

  private static void appendMetadata(StringBuilder report, Map<String, String> metadata) {
    if (metadata.isEmpty()) {
      return;
    }
    report.append("\n## Run metadata\n\n| Setting | Value |\n|---|---|\n");
    metadata.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> report.append("| ")
      .append(escape(entry.getKey())).append(" | ").append(escape(entry.getValue())).append(" |\n"));
  }

  private static void appendHeader(StringBuilder report, String firstColumn) {
    report.append("| ").append(firstColumn);
    Arrays.stream(AnalysisMode.values()).forEach(mode -> report.append(" | ").append(mode.label()));
    report.append(" |\n|---|---:|---:|---:|---:|\n");
  }

  private static void appendRow(StringBuilder report, String label, Function<AnalysisMode, Object> value) {
    report.append("| ").append(escape(label));
    Arrays.stream(AnalysisMode.values()).forEach(mode -> report.append(" | ").append(escape(String.valueOf(value.apply(mode)))));
    report.append(" |\n");
  }

  private static Object counts(SourceOnlyComparison.Run run, Function<SemanticReport.Counts, Integer> value) {
    return run.semantics() == null ? "N/A" : value.apply(run.semantics().totals());
  }

  private static Object graphCounts(SourceOnlyComparison.Run run, Function<SemanticReport.GraphCounts, Integer> value) {
    return run.semantics() == null || run.semantics().graphTotals() == null
      || run.semantics().graphTraversal() != null && run.semantics().graphTraversal().limit() == 0 ? "N/A" : value.apply(run.semantics().graphTotals());
  }

  private static Object graphTraversalCounts(SourceOnlyComparison.Run run, Function<SemanticReport.GraphTraversal, Integer> value) {
    return run.semantics() == null || run.semantics().graphTraversal() == null ? "N/A" : value.apply(run.semantics().graphTraversal());
  }

  static String graphTraversalStatus(List<SourceOnlyComparison.Run> samples) {
    if (samples.stream().allMatch(run -> run.success() && run.semantics() != null && run.semantics().graphTraversal() != null
      && run.semantics().graphTraversal().limit() == 0)) {
      return "DISABLED";
    }
    if (samples.stream().anyMatch(run -> run.semantics() != null && run.semantics().graphTraversal() != null
      && run.semantics().graphTraversal().limit() > 0 && !run.semantics().graphTraversal().complete())) {
      return "PARTIAL";
    }
    return samples.stream().allMatch(run -> run.success() && run.semantics() != null && run.semantics().graphTraversal() != null
      && run.semantics().graphTraversal().limit() > 0 && run.semantics().graphTraversal().complete())
      ? "COMPLETE" : "UNKNOWN";
  }

  private static String filesWithNoUnknowns(SemanticReport semantics) {
    if (semantics == null) {
      return "N/A";
    }
    if (!semantics.hasFileObservations()) {
      return "UNAVAILABLE";
    }
    long count = semantics.files().values().stream().filter(value -> value.total() > 0 && value.unknown() == 0).count();
    return count + " / " + semantics.files().size();
  }

  private static String cleanup(SourceOnlyComparison.Run run) {
    return switch (telemetry(run, "comparison.bytecode.cleaned", "")) {
      case "true" -> "YES";
      case "false" -> "NO";
      default -> "N/A";
    };
  }

  private static String distinct(List<SourceOnlyComparison.Run> samples, Function<SourceOnlyComparison.Run, String> value) {
    return samples.stream().map(value).distinct().collect(Collectors.joining(", "));
  }

  private static String telemetry(SourceOnlyComparison.Run run, String key, String fallback) {
    return run.telemetry().getOrDefault(key, fallback);
  }

  private static String telemetryMedian(List<SourceOnlyComparison.Run> samples, String key) {
    return median(samples.stream().map(run -> number(run, key)).toList());
  }

  private static Double number(SourceOnlyComparison.Run run, String key) {
    try {
      double value = Double.parseDouble(telemetry(run, key, ""));
      return Double.isFinite(value) && value >= 0 ? value : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String median(List<Double> values) {
    if (values.isEmpty() || values.stream().anyMatch(value -> value == null)) {
      return "N/A";
    }
    var sorted = values.stream().sorted().toList();
    int middle = sorted.size() / 2;
    return timing(sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2 : sorted.get(middle));
  }

  private static String timing(Double value) {
    return value == null ? "N/A" : String.format(Locale.ROOT, "%.1f", value);
  }

  private static String percentage(Double value) {
    return value == null ? "N/A" : String.format(Locale.ROOT, "%.3f%%", value);
  }

  private static String change(Double before, Double after) {
    return before == null || after == null ? "N/A" : String.format(Locale.ROOT, "%+.3f", after - before);
  }

  private static String escape(String value) {
    return value.replace("|", "\\|").replace('\n', ' ').replace('\r', ' ');
  }
}
