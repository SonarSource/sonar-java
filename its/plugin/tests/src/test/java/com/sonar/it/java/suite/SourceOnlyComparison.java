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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

final class SourceOnlyComparison {

  record Finding(String rule, String path, Integer line, String message) {
    Finding {
      path = path.replace('\\', '/');
    }

    Key key() {
      return new Key(rule, path, line);
    }
  }

  private record Key(String rule, String path, Integer line) {
  }

  private record UnknownOccurrence(String path, SemanticReport.UnknownIdentifier identifier) {
    UnknownKey key() {
      return new UnknownKey(path, identifier.name(), identifier.range());
    }
  }

  private record UnknownKey(String path, String name, String range) {
  }

  record Run(String label, boolean success, long scanMillis, List<String> files, List<Finding> findings,
             Map<String, String> telemetry, SemanticReport semantics, String error) {
  }

  record RuleResult(String rule, int current, int candidate, int shared, int currentOnly, int candidateOnly, Double retention) {
  }

  record Comparison(boolean valid, boolean placeholder, String error, Run current, Run candidate,
                    List<RuleResult> rules, List<Finding> currentOnly, List<Finding> candidateOnly) {
  }

  private SourceOnlyComparison() {
  }

  static Path createRunDirectory(Path resultsDirectory) throws IOException {
    Files.createDirectories(resultsDirectory);
    int next;
    try (var entries = Files.list(resultsDirectory)) {
      next = entries.map(path -> path.getFileName().toString())
        .filter(name -> name.matches("run-\\d+"))
        .mapToInt(name -> Integer.parseInt(name.substring(4))).max().orElse(0) + 1;
    }
    while (true) {
      Path directory = resultsDirectory.resolve(String.format(Locale.ROOT, "run-%03d", next));
      try {
        return Files.createDirectory(directory);
      } catch (FileAlreadyExistsException e) {
        next++;
      }
    }
  }

  static Comparison compare(Run current, Run candidate, boolean placeholder, List<String> activeRules) {
    if (!current.success() || !candidate.success()) {
      return invalid(current, candidate, placeholder, "At least one scan failed; agreement metrics are unavailable.");
    }
    if (!new TreeSet<>(current.files()).equals(new TreeSet<>(candidate.files()))) {
      return invalid(current, candidate, placeholder, "The scans indexed different files; agreement metrics are unavailable.");
    }
    if (current.semantics() == null || candidate.semantics() == null) {
      return invalid(current, candidate, placeholder, "A semantic report is missing; comparison metrics are unavailable.");
    }

    if (!current.semantics().files().keySet().equals(candidate.semantics().files().keySet())) {
      var currentOnlyFiles = new TreeSet<>(current.semantics().files().keySet());
      currentOnlyFiles.removeAll(candidate.semantics().files().keySet());
      var candidateOnlyFiles = new TreeSet<>(candidate.semantics().files().keySet());
      candidateOnlyFiles.removeAll(current.semantics().files().keySet());
      return invalid(current, candidate, placeholder, "Semantic reports cover different files; current-only: "
        + currentOnlyFiles + "; candidate-only: " + candidateOnlyFiles + ". Comparison metrics are unavailable.");
    }
    for (String path : new TreeSet<>(current.semantics().files().keySet())) {
      int before = current.semantics().files().get(path).total();
      int after = candidate.semantics().files().get(path).total();
      if (before != after) {
        return invalid(current, candidate, placeholder, "Identifier coverage differs for " + path + ": current="
          + before + ", candidate=" + after + ". Comparison metrics are unavailable.");
      }
    }

    var candidateOnly = sorted(candidate.findings());
    var currentOnly = new ArrayList<Finding>();
    for (Finding finding : sorted(current.findings())) {
      int match = -1;
      for (int i = 0; i < candidateOnly.size(); i++) {
        if (finding.key().equals(candidateOnly.get(i).key())) {
          match = i;
          break;
        }
      }
      if (match < 0) {
        currentOnly.add(finding);
      } else {
        candidateOnly.remove(match);
      }
    }

    var ruleKeys = new TreeSet<>(activeRules);
    current.findings().forEach(finding -> ruleKeys.add(finding.rule()));
    candidate.findings().forEach(finding -> ruleKeys.add(finding.rule()));
    var rows = ruleKeys.stream().map(rule -> {
      int currentCount = count(current.findings(), rule);
      int candidateCount = count(candidate.findings(), rule);
      int lost = count(currentOnly, rule);
      int added = count(candidateOnly, rule);
      int shared = currentCount - lost;
      return new RuleResult(rule, currentCount, candidateCount, shared, lost, added,
        currentCount == 0 ? null : (double) shared / currentCount);
    }).toList();
    return new Comparison(true, placeholder, null, current, candidate, rows, currentOnly, candidateOnly);
  }

  private static Comparison invalid(Run current, Run candidate, boolean placeholder, String error) {
    return new Comparison(false, placeholder, error, current, candidate, List.of(), List.of(), List.of());
  }

  private static ArrayList<Finding> sorted(List<Finding> findings) {
    var result = new ArrayList<>(findings);
    result.sort(Comparator.comparing(Finding::rule).thenComparing(Finding::path)
      .thenComparing(Finding::line, Comparator.nullsFirst(Comparator.naturalOrder()))
      .thenComparing(Finding::message));
    return result;
  }

  private static int count(List<Finding> findings, String rule) {
    return (int) findings.stream().filter(finding -> finding.rule().equals(rule)).count();
  }

  static void write(Path directory, Comparison comparison) throws IOException {
    write(directory, comparison, Map.of(), List.of(), List.of());
  }

  static void write(Path directory, Comparison comparison, Map<String, String> metadata,
                    List<Run> currentSamples, List<Run> candidateSamples) throws IOException {
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("report.md"), markdown(comparison, metadata, currentSamples, candidateSamples));
  }

  static String markdown(Comparison comparison) {
    return markdown(comparison, Map.of(), List.of(), List.of());
  }

  static String markdown(Comparison comparison, Map<String, String> metadata, List<Run> currentSamples, List<Run> candidateSamples) {
    var report = new StringBuilder("# Source-only Java analysis comparison\n\n")
      .append("**Runner:** Orchestrator MavenBuild (`sonar:sonar`)\n\n");
    report.append("**Comparison:** ").append(comparison.valid() ? "VALID" : "INVALID")
      .append(" · **Candidate:** ").append(comparison.placeholder() ? "placeholder (same analyzer and settings)" : "configured")
      .append("\n\n## Summary\n\n| Metric | Current | Candidate |\n|---|---:|---:|\n");
    Run current = comparison.current();
    Run candidate = comparison.candidate();
    appendMetric(report, "Scan status", current.success() ? "SUCCESS" : "FAILED", candidate.success() ? "SUCCESS" : "FAILED");
    appendMetric(report, "Java files analyzed", current.files().size(), candidate.files().size());
    appendSemanticSummary(report, current.semantics(), candidate.semantics());
    appendMetric(report, "Findings", current.findings().size(), candidate.findings().size());
    if (currentSamples.isEmpty() && candidateSamples.isEmpty()) {
      appendMetric(report, "Scan time (ms)", current.scanMillis(), candidate.scanMillis());
    } else {
      appendMetric(report, "Median Maven/server time (ms)", median(currentSamples.stream().map(run -> (double) run.scanMillis()).toList()),
        median(candidateSamples.stream().map(run -> (double) run.scanMillis()).toList()));
      appendMetric(report, "Median JavaSensor time (ms)", analyzerMedian(currentSamples), analyzerMedian(candidateSamples));
    }
    appendMetric(report, "Source characters analyzed", telemetry(current, "success.size_chars"), telemetry(candidate, "success.size_chars"));
    appendMetric(report, "Undefined-type errors", telemetry(current, "success.type_error_count"), telemetry(candidate, "success.type_error_count"));
    appendMetric(report, "Source characters with parse errors", telemetry(current, "parse_errors.size_chars"), telemetry(candidate, "parse_errors.size_chars"));
    appendMetric(report, "Source characters with analysis exceptions", telemetry(current, "exceptions.size_chars"), telemetry(candidate, "exceptions.size_chars"));
    if (!comparison.valid()) {
      appendRunDetails(report, metadata, currentSamples, candidateSamples);
      report.append("\nComparison unavailable: ").append(comparison.error()).append('\n');
      appendFailure(report, current);
      appendFailure(report, candidate);
      return report.toString();
    }
    Double currentUnknown = current.semantics().totals().unknownPercentage();
    Double candidateUnknown = candidate.semantics().totals().unknownPercentage();
    int shared = current.findings().size() - comparison.currentOnly().size();
    report.append("\n| Comparison metric | Value |\n|---|---:|\n")
      .append("| Change in unknown identifiers (percentage points) | ").append(change(currentUnknown, candidateUnknown)).append(" |\n")
      .append("| Net change in unknown identifier count | ").append(String.format(Locale.ROOT, "%+d",
        candidate.semantics().totals().unknown() - current.semantics().totals().unknown())).append(" |\n");
    appendFileChanges(report, current.semantics(), candidate.semantics());
    report.append("| Rules compared | ").append(comparison.rules().size()).append(" |\n")
      .append("| Rules with findings | ").append(comparison.rules().stream().filter(row -> row.current() > 0 || row.candidate() > 0).count()).append(" |\n")
      .append("| Shared findings | ").append(shared).append(" |\n")
      .append("| Current-only findings | ").append(comparison.currentOnly().size()).append(" |\n")
      .append("| Candidate-only findings | ").append(comparison.candidateOnly().size()).append(" |\n")
      .append("| Retention of current findings | ").append(percentage(current.findings().isEmpty() ? null : (double) shared / current.findings().size())).append(" |\n");
    if (current.findings().isEmpty() || candidate.findings().isEmpty()) {
      report.append("\nAt least one scan reported no findings; agreement alone does not establish detection quality.\n");
    }
    appendSourcePathFixture(report, metadata);
    appendUnknownOccurrences(report, current.semantics(), candidate.semantics());
    appendRunDetails(report, metadata, currentSamples, candidateSamples);
    appendRankedChanges(report, current.semantics(), candidate.semantics(), true);
    appendRankedChanges(report, current.semantics(), candidate.semantics(), false);
    appendTopUnknownFiles(report, current.semantics(), candidate.semantics());
    appendFileSemantics(report, current.semantics(), candidate.semantics());
    report.append("\n## Rules with findings\n\n| Rule | Current | Candidate | Shared | Current only | Candidate only | Retention |\n")
      .append("|---|---:|---:|---:|---:|---:|---:|\n");
    for (RuleResult row : comparison.rules()) {
      if (row.current() == 0 && row.candidate() == 0) {
        continue;
      }
      report.append("| ").append(row.rule()).append(" | ").append(row.current()).append(" | ").append(row.candidate())
        .append(" | ").append(row.shared()).append(" | ").append(row.currentOnly()).append(" | ").append(row.candidateOnly())
        .append(" | ").append(percentage(row.retention()))
        .append(" |\n");
    }
    long noFindings = comparison.rules().stream().filter(row -> row.current() == 0 && row.candidate() == 0).count();
    report.append("\n").append(noFindings).append(" rules had no findings in either run and are omitted from this table.\n");
    appendFindings(report, "Current-only findings", comparison.currentOnly());
    appendFindings(report, "Candidate-only findings", comparison.candidateOnly());
    appendFindings(report, "Current findings", sorted(current.findings()));
    report.append("\n## Reading the data\n\n")
      .append("- Both modes use sources and the JDK, without project bytecode or dependency JARs.\n")
      .append("- Retention measures agreement with current source-only findings, not accuracy.\n")
      .append("- Undefined-type errors are a diagnostic count, not resolution coverage. Missing telemetry is shown as N/A.\n")
      .append("- Identifier counts come from the semantic report: known = total − unknown. They count identifier occurrences, not distinct fields or properties.\n")
      .append("- Unknown occurrences match by file, name, and token range. Removed means no longer reported unknown; correctness is checked separately. AST context is diagnostic, not a method/type/field classification.\n")
      .append("- Unknown percentage = unknown / total × 100. The project percentage uses aggregate counts, not an average of file percentages. No identifiers means N/A.\n")
      .append("- Per-file counts are current / candidate. A negative change in unknown percentage means fewer unresolved identifiers; it does not prove semantic correctness.\n")
      .append("- Files with no unknown identifiers must contain at least one identifier; their percentage uses all analyzed files. Files with zero identifiers are excluded from improved/unchanged/regressed counts.\n")
      .append("- Configured rules may be disabled when dependencies are absent; zero findings do not prove a rule ran.\n")
      .append("- Wall times include Maven startup, scanning, and server processing. Analyzer timings are reported separately when available; test-server startup is excluded. Repeated samples exclude warm-up runs.\n");
    return report.toString();
  }

  private static void appendRunDetails(StringBuilder report, Map<String, String> metadata, List<Run> currentSamples, List<Run> candidateSamples) {
    if (!metadata.isEmpty()) {
      report.append("\n## Run metadata\n\n| Setting | Value |\n|---|---|\n");
      metadata.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
        report.append("| ").append(escape(entry.getKey())).append(" | ").append(escape(entry.getValue())).append(" |\n"));
    }
    if (currentSamples.isEmpty() && candidateSamples.isEmpty()) {
      return;
    }
    report.append("\n## Measured timings\n\nWall time includes Maven and server processing; analyzer time measures JavaSensor execution (parsing, semantics, checks, and metrics). Warm-ups are excluded. Missing analyzer timings are N/A.\n\n")
      .append("| Sample | Current wall (ms) | Candidate wall (ms) | Current analyzer (ms) | Candidate analyzer (ms) |\n")
      .append("|---|---:|---:|---:|---:|\n");
    for (int i = 0; i < Math.max(currentSamples.size(), candidateSamples.size()); i++) {
      Run current = i < currentSamples.size() ? currentSamples.get(i) : null;
      Run candidate = i < candidateSamples.size() ? candidateSamples.get(i) : null;
      report.append("| ").append(i + 1).append(" | ").append(current == null ? "N/A" : current.scanMillis())
        .append(" | ").append(candidate == null ? "N/A" : candidate.scanMillis())
        .append(" | ").append(timing(analyzerMillis(current))).append(" | ").append(timing(analyzerMillis(candidate))).append(" |\n");
    }
    report.append("| Median | ").append(median(currentSamples.stream().map(run -> (double) run.scanMillis()).toList()))
      .append(" | ").append(median(candidateSamples.stream().map(run -> (double) run.scanMillis()).toList()))
      .append(" | ").append(analyzerMedian(currentSamples)).append(" | ").append(analyzerMedian(candidateSamples)).append(" |\n");
  }

  private static List<UnknownOccurrence> unknownOccurrences(SemanticReport semantics) {
    return semantics.unknownIdentifiers().entrySet().stream()
      .flatMap(entry -> entry.getValue().stream().map(identifier -> new UnknownOccurrence(entry.getKey(), identifier)))
      .sorted(Comparator.comparing(UnknownOccurrence::path).thenComparing(value -> value.identifier().range())
        .thenComparing(value -> value.identifier().name())).toList();
  }

  private static void appendSourcePathFixture(StringBuilder report, Map<String, String> metadata) {
    if (!metadata.containsKey("Source-path fixture/current identifiers")) {
      return;
    }
    report.append("\n## Source-path correctness fixture\n\nOnly the consumer is analyzed; dependency files are used for resolution only.\n\n")
      .append("| Metric | Without source paths | With source paths |\n|---|---:|---:|\n");
    for (String metric : List.of("files", "identifiers", "known identifiers", "unknown identifiers")) {
      appendMetric(report, metric, metadata.get("Source-path fixture/current " + metric), metadata.get("Source-path fixture/candidate " + metric));
    }
    report.append("\nExact type, overload, and inherited-field checks expect project references unknown in the baseline and known in the candidate. MissingType must remain unknown in both.\n");
  }

  private static void appendUnknownOccurrences(StringBuilder report, SemanticReport current, SemanticReport candidate) {
    report.append("\n## Unknown identifier occurrences\n\n");
    if (!current.hasUnknownDetails() || !candidate.hasUnknownDetails()) {
      report.append("Unavailable: at least one semantic report contains counts only.\n");
      return;
    }
    var before = unknownOccurrences(current);
    var after = unknownOccurrences(candidate);
    var beforeKeys = before.stream().map(UnknownOccurrence::key).collect(java.util.stream.Collectors.toSet());
    var afterKeys = after.stream().map(UnknownOccurrence::key).collect(java.util.stream.Collectors.toSet());
    var removed = before.stream().filter(value -> !afterKeys.contains(value.key())).toList();
    var added = after.stream().filter(value -> !beforeKeys.contains(value.key())).toList();
    report.append("| Metric | Count |\n|---|---:|\n")
      .append("| Still unknown in both runs | ").append(before.size() - removed.size()).append(" |\n")
      .append("| No longer reported unknown | ").append(removed.size()).append(" |\n")
      .append("| Newly reported unknown | ").append(added.size()).append(" |\n");
    appendUnknownList(report, "No longer reported unknown", removed);
    appendUnknownList(report, "Newly reported unknown", added);
    var contexts = new TreeSet<String>();
    before.forEach(value -> contexts.add(value.identifier().parentKind()));
    after.forEach(value -> contexts.add(value.identifier().parentKind()));
    report.append("\n### Unknown occurrences by AST context\n\n| Context | Current | Candidate |\n|---|---:|---:|\n");
    for (String context : contexts) {
      report.append("| ").append(context).append(" | ")
        .append(before.stream().filter(value -> value.identifier().parentKind().equals(context)).count()).append(" | ")
        .append(after.stream().filter(value -> value.identifier().parentKind().equals(context)).count()).append(" |\n");
    }
  }

  private static void appendUnknownList(StringBuilder report, String title, List<UnknownOccurrence> values) {
    report.append("\n### ").append(title).append("\n\n");
    if (values.isEmpty()) {
      report.append("None.\n");
      return;
    }
    report.append("| File | Range | Identifier | AST context |\n|---|---|---|---|\n");
    values.stream().limit(20).forEach(value -> report.append("| ").append(escape(value.path())).append(" | ")
      .append(escape(value.identifier().range())).append(" | ").append(escape(value.identifier().name())).append(" | ")
      .append(escape(value.identifier().parentKind())).append(" |\n"));
    if (values.size() > 20) {
      report.append("\n").append(values.size() - 20).append(" additional occurrences omitted.\n");
    }
  }

  private static Double analyzerMillis(Run run) {
    if (run == null) {
      return null;
    }
    String value = run.telemetry().get("comparison.analyzer.time_ms");
    if (value == null) {
      return null;
    }
    try {
      double millis = Double.parseDouble(value);
      return Double.isFinite(millis) && millis >= 0 ? millis : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String analyzerMedian(List<Run> samples) {
    var values = samples.stream().map(SourceOnlyComparison::analyzerMillis).toList();
    return values.stream().anyMatch(value -> value == null) ? "N/A" : median(values);
  }

  private static String median(List<Double> values) {
    if (values.isEmpty()) {
      return "N/A";
    }
    var sorted = values.stream().sorted().toList();
    int middle = sorted.size() / 2;
    return timing(sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2 : sorted.get(middle));
  }

  private static String timing(Double millis) {
    return millis == null ? "N/A" : String.format(Locale.ROOT, "%.1f", millis);
  }

  private static void appendRankedChanges(StringBuilder report, SemanticReport current, SemanticReport candidate, boolean improvements) {
    var paths = current.files().keySet().stream().filter(path -> {
      Double before = current.files().get(path).unknownPercentage();
      Double after = candidate.files().get(path).unknownPercentage();
      return before != null && after != null && (improvements ? after < before : after > before);
    }).sorted(Comparator.comparingDouble((String path) -> Math.abs(candidate.files().get(path).unknownPercentage()
      - current.files().get(path).unknownPercentage())).reversed().thenComparing(Comparator.naturalOrder())).limit(5).toList();
    report.append("\n## Largest semantic ").append(improvements ? "improvements" : "regressions")
      .append("\n\nTop five by absolute change in unknown percentage.\n\n");
    if (paths.isEmpty()) {
      report.append("None.\n");
      return;
    }
    report.append("| File | Current unknown % | Candidate unknown % | Change (pp) |\n|---|---:|---:|---:|\n");
    for (String path : paths) {
      Double before = current.files().get(path).unknownPercentage();
      Double after = candidate.files().get(path).unknownPercentage();
      report.append("| ").append(escape(path)).append(" | ").append(identifierPercentage(before)).append(" | ")
        .append(identifierPercentage(after)).append(" | ").append(change(before, after)).append(" |\n");
    }
  }

  private static void appendMetric(StringBuilder report, String metric, Object current, Object candidate) {
    report.append("| ").append(metric).append(" | ").append(current).append(" | ").append(candidate).append(" |\n");
  }

  private static String telemetry(Run run, String suffix) {
    return run.telemetry().getOrDefault("java.analysis.main." + suffix, "N/A");
  }

  private static String percentage(Double value) {
    return value == null ? "N/A" : String.format(Locale.ROOT, "%.1f%%", value * 100);
  }

  private static String identifierPercentage(Double value) {
    return value == null ? "N/A" : String.format(Locale.ROOT, "%.3f%%", value);
  }

  private static String change(Double current, Double candidate) {
    return current == null || candidate == null ? "N/A" : String.format(Locale.ROOT, "%+.3f", candidate - current);
  }

  private static void appendSemanticSummary(StringBuilder report, SemanticReport current, SemanticReport candidate) {
    appendMetric(report, "Identifiers (total)", current == null ? "N/A" : current.totals().total(), candidate == null ? "N/A" : candidate.totals().total());
    appendMetric(report, "Known identifiers", current == null ? "N/A" : current.totals().known(), candidate == null ? "N/A" : candidate.totals().known());
    appendMetric(report, "Unknown identifiers", current == null ? "N/A" : current.totals().unknown(), candidate == null ? "N/A" : candidate.totals().unknown());
    appendMetric(report, "Unknown identifiers (%)", identifierPercentage(current == null ? null : current.totals().unknownPercentage()),
      identifierPercentage(candidate == null ? null : candidate.totals().unknownPercentage()));
    appendMetric(report, "Files with no unknown identifiers", filesWithNoUnknowns(current), filesWithNoUnknowns(candidate));
  }

  private static String filesWithNoUnknowns(SemanticReport semantics) {
    if (semantics == null) {
      return "N/A";
    }
    long count = semantics.files().values().stream().filter(counts -> counts.total() > 0 && counts.unknown() == 0).count();
    int files = semantics.files().size();
    return count + " / " + files + " (" + identifierPercentage(files == 0 ? null : 100.0 * count / files) + ")";
  }

  private static void appendFileChanges(StringBuilder report, SemanticReport current, SemanticReport candidate) {
    int improved = 0;
    int unchanged = 0;
    int regressed = 0;
    int unavailable = 0;
    for (String path : current.files().keySet()) {
      Double before = current.files().get(path).unknownPercentage();
      Double after = candidate.files().get(path).unknownPercentage();
      if (before == null || after == null) {
        unavailable++;
      } else if (after < before) {
        improved++;
      } else if (after > before) {
        regressed++;
      } else {
        unchanged++;
      }
    }
    report.append("| Files improved (unknown %) | ").append(improved).append(" |\n")
      .append("| Files unchanged (unknown %) | ").append(unchanged).append(" |\n")
      .append("| Files regressed (unknown %) | ").append(regressed).append(" |\n")
      .append("| Files without comparable identifier percentages | ").append(unavailable).append(" |\n");
  }

  private static void appendTopUnknownFiles(StringBuilder report, SemanticReport current, SemanticReport candidate) {
    var paths = current.files().keySet().stream()
      .filter(path -> current.files().get(path).unknown() > 0 || candidate.files().get(path).unknown() > 0)
      .sorted(Comparator.comparingInt((String path) -> Math.max(current.files().get(path).unknown(), candidate.files().get(path).unknown()))
        .reversed().thenComparing(Comparator.naturalOrder()))
      .limit(5).toList();
    report.append("\n## Top files contributing unknown identifiers\n\n")
      .append("Top five by the largest unknown count in either run. Values are **current / candidate**.\n\n");
    if (paths.isEmpty()) {
      report.append("None.\n");
      return;
    }
    report.append("| File | Unknown identifiers | Share of project unknown identifiers |\n|---|---:|---:|\n");
    for (String path : paths) {
      int before = current.files().get(path).unknown();
      int after = candidate.files().get(path).unknown();
      report.append("| ").append(escape(path)).append(" | ").append(before).append(" / ").append(after).append(" | ")
        .append(identifierPercentage(current.totals().unknown() == 0 ? null : 100.0 * before / current.totals().unknown()))
        .append(" / ").append(identifierPercentage(candidate.totals().unknown() == 0 ? null : 100.0 * after / candidate.totals().unknown()))
        .append(" |\n");
    }
  }

  private static void appendFileSemantics(StringBuilder report, SemanticReport current, SemanticReport candidate) {
    report.append("\n## Semantics per file\n\nCounts and percentages are **current / candidate**. Change is candidate minus current.\n\n")
      .append("| File | Total identifiers | Known identifiers | Unknown identifiers | Unknown % | Change (pp) |\n")
      .append("|---|---:|---:|---:|---:|---:|\n");
    for (String path : new TreeSet<>(current.files().keySet())) {
      SemanticReport.Counts before = current.files().get(path);
      SemanticReport.Counts after = candidate.files().get(path);
      report.append("| ").append(escape(path)).append(" | ").append(before.total()).append(" / ").append(after.total())
        .append(" | ").append(before.known()).append(" / ").append(after.known())
        .append(" | ").append(before.unknown()).append(" / ").append(after.unknown())
        .append(" | ").append(identifierPercentage(before.unknownPercentage())).append(" / ").append(identifierPercentage(after.unknownPercentage()))
        .append(" | ").append(change(before.unknownPercentage(), after.unknownPercentage())).append(" |\n");
    }
  }

  private static void appendFindings(StringBuilder report, String title, List<Finding> findings) {
    report.append("\n## ").append(title).append("\n\n");
    if (findings.isEmpty()) {
      report.append("None.\n");
      return;
    }
    report.append("| Rule | File | Line | Message |\n|---|---|---:|---|\n");
    for (Finding finding : findings) {
      report.append("| ").append(finding.rule()).append(" | ").append(escape(finding.path()))
        .append(" | ").append(finding.line() == null ? "N/A" : finding.line()).append(" | ")
        .append(escape(finding.message())).append(" |\n");
    }
  }

  private static String escape(String value) {
    return value.replace("|", "\\|").replace('\n', ' ').replace('\r', ' ');
  }

  private static void appendFailure(StringBuilder report, Run run) {
    if (run.error() != null) {
      report.append("\n### ").append(run.label()).append(" failure\n\n```text\n").append(run.error()).append("\n```\n");
    }
  }
}
