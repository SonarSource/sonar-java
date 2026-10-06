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

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
    var failed = new SourceOnlyComparison.Run("candidate", false, 12, List.of(), List.of(), Map.of(), "Scanner exited with code 1");
    var comparison = SourceOnlyComparison.compare(run(List.of()), failed, false, List.of());
    assertThat(comparison.valid()).isFalse();
    assertThat(comparison.rules()).isEmpty();
    assertThat(SourceOnlyComparison.markdown(comparison)).contains("FAILED", "Scanner exited with code 1", "Comparison unavailable");

    var differentFiles = new SourceOnlyComparison.Run("candidate", true, 12, List.of("Other.java"), List.of(), Map.of(), null);
    assertThat(SourceOnlyComparison.compare(run(List.of()), differentFiles, false, List.of()).valid()).isFalse();
  }

  @Test
  void writes_a_json_diff_and_markdown_report(@TempDir Path directory) throws IOException {
    var lost = finding("java:S1116", "src/Example.java", 3, "lost");
    var comparison = SourceOnlyComparison.compare(run(List.of(lost)), run(List.of()), false, List.of());
    SourceOnlyComparison.write(directory, comparison);

    var json = JsonParser.parseString(Files.readString(directory.resolve("diff.json"))).getAsJsonObject();
    assertThat(json.getAsJsonArray("currentOnly")).hasSize(1);
    assertThat(json.getAsJsonArray("candidateOnly")).isEmpty();
    assertThat(Files.readString(directory.resolve("report.md"))).contains("java:S1116", "0.0%", "12 ms", "not accuracy");
  }

  private static SourceOnlyComparison.Finding finding(String rule, String path, Integer line, String message) {
    return new SourceOnlyComparison.Finding(rule, path, line, message, JsonParser.parseString("{}"));
  }

  private static SourceOnlyComparison.Run run(List<SourceOnlyComparison.Finding> findings) {
    return new SourceOnlyComparison.Run("scan", true, 12, List.of("src/Example.java"), findings, Map.of(), null);
  }
}
