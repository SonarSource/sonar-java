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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

final class SourceOnlyComparison {

  record Finding(String rule, String path, Integer line, String message, JsonElement details) {
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
             Map<String, String> telemetry, String error) {
  }

  record RuleResult(String rule, int current, int candidate, int shared, int currentOnly, int candidateOnly, Double retention) {
  }

  record Comparison(boolean valid, boolean placeholder, String error, Run current, Run candidate,
                    List<RuleResult> rules, List<Finding> currentOnly, List<Finding> candidateOnly) {
  }

  private SourceOnlyComparison() {
  }

  static Comparison compare(Run current, Run candidate, boolean placeholder, List<String> activeRules) {
    if (!current.success() || !candidate.success()) {
      return invalid(current, candidate, placeholder, "At least one scan failed; agreement metrics are unavailable.");
    }
    if (!new TreeSet<>(current.files()).equals(new TreeSet<>(candidate.files()))) {
      return invalid(current, candidate, placeholder, "The scans indexed different files; agreement metrics are unavailable.");
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
    Files.writeString(directory.resolve("diff.json"), new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(comparison));
    Files.writeString(directory.resolve("report.md"), markdown(comparison));
  }

  static String markdown(Comparison comparison) {
    var report = new StringBuilder("# Source-only Java analysis comparison\n\n");
    if (comparison.placeholder()) {
      report.append("Candidate is a placeholder: it uses the same configuration as current source-only analysis.\n\n");
    }
    report.append("Both modes use sources and the JDK, without project bytecode or dependency JARs.\n")
      .append("Retention measures agreement with current source-only findings, not accuracy.\n\n");
    appendRun(report, comparison.current());
    appendRun(report, comparison.candidate());
    if (!comparison.valid()) {
      return report.append("\nComparison unavailable: ").append(comparison.error()).append('\n').toString();
    }
    if (comparison.current().findings().isEmpty() || comparison.candidate().findings().isEmpty()) {
      report.append("\nAt least one scan reported no findings; inspect coverage and logs before interpreting agreement.\n");
    }
    report.append("\n| Rule | Current | Candidate | Shared | Current only | Candidate only | Retention |\n")
      .append("|---|---:|---:|---:|---:|---:|---:|\n");
    for (RuleResult row : comparison.rules()) {
      report.append("| ").append(row.rule()).append(" | ").append(row.current()).append(" | ").append(row.candidate())
        .append(" | ").append(row.shared()).append(" | ").append(row.currentOnly()).append(" | ").append(row.candidateOnly())
        .append(" | ").append(row.retention() == null ? "N/A" : String.format(Locale.ROOT, "%.1f%%", row.retention() * 100))
        .append(" |\n");
    }
    report.append("\nSee `diff.json` for individual differences and full issue details, and `current.log` / `candidate.log` for scanner logs.\n")
      .append("Unresolved-type telemetry counts particular undefined-type errors; it is not resolution coverage.\n");
    return report.toString();
  }

  private static void appendRun(StringBuilder report, Run run) {
    report.append("\n").append(run.label()).append(": ").append(run.success() ? "SUCCESS" : "FAILED")
      .append(", ").append(run.files().size()).append(" files, ").append(run.findings().size()).append(" findings, ")
      .append(run.scanMillis()).append(" ms scanner wall time.\n");
    if (run.error() != null) {
      report.append("\nFailure: ").append(run.error()).append('\n');
    }
    if (!run.telemetry().isEmpty()) {
      report.append("\nTelemetry:\n\n");
      run.telemetry().entrySet().stream().sorted(Map.Entry.comparingByKey())
        .forEach(entry -> report.append("- `").append(entry.getKey()).append("`: ").append(entry.getValue()).append('\n'));
    }
  }
}
