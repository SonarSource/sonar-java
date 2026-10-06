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
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("report.md"), markdown(comparison));
  }

  static String markdown(Comparison comparison) {
    var report = new StringBuilder("# Source-only Java analysis comparison\n\n");
    report.append("**Comparison:** ").append(comparison.valid() ? "VALID" : "INVALID")
      .append(" · **Candidate:** ").append(comparison.placeholder() ? "placeholder (same analyzer and settings)" : "configured")
      .append("\n\n## Summary\n\n| Metric | Current | Candidate |\n|---|---:|---:|\n");
    Run current = comparison.current();
    Run candidate = comparison.candidate();
    appendMetric(report, "Scan status", current.success() ? "SUCCESS" : "FAILED", candidate.success() ? "SUCCESS" : "FAILED");
    appendMetric(report, "Java files analyzed", current.files().size(), candidate.files().size());
    appendSemanticSummary(report, current.semantics(), candidate.semantics());
    appendMetric(report, "Findings", current.findings().size(), candidate.findings().size());
    appendMetric(report, "Scan time (ms)", current.scanMillis(), candidate.scanMillis());
    appendMetric(report, "Source characters analyzed", telemetry(current, "success.size_chars"), telemetry(candidate, "success.size_chars"));
    appendMetric(report, "Undefined-type errors", telemetry(current, "success.type_error_count"), telemetry(candidate, "success.type_error_count"));
    appendMetric(report, "Source characters with parse errors", telemetry(current, "parse_errors.size_chars"), telemetry(candidate, "parse_errors.size_chars"));
    appendMetric(report, "Source characters with analysis exceptions", telemetry(current, "exceptions.size_chars"), telemetry(candidate, "exceptions.size_chars"));
    if (!comparison.valid()) {
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
      .append("- Unknown percentage = unknown / total × 100. The project percentage uses aggregate counts, not an average of file percentages. No identifiers means N/A.\n")
      .append("- Per-file counts are current / candidate. A negative change in unknown percentage means fewer unresolved identifiers; it does not prove semantic correctness.\n")
      .append("- Files with no unknown identifiers must contain at least one identifier; their percentage uses all analyzed files. Files with zero identifiers are excluded from improved/unchanged/regressed counts.\n")
      .append("- Configured rules may be disabled when dependencies are absent; zero findings do not prove a rule ran.\n")
      .append("- Scan times are individual wall-time samples, including engine setup.\n");
    return report.toString();
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
