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
import com.sonar.orchestrator.locator.FileLocation;
import com.sonarsource.scanner.integrationtester.dsl.ActiveRule;
import com.sonarsource.scanner.integrationtester.dsl.EngineVersion;
import com.sonarsource.scanner.integrationtester.dsl.RuleKey;
import com.sonarsource.scanner.integrationtester.dsl.ScannerInput;
import com.sonarsource.scanner.integrationtester.dsl.ScannerResultSuccess;
import com.sonarsource.scanner.integrationtester.dsl.SonarProjectContext;
import com.sonarsource.scanner.integrationtester.dsl.SonarServerContext;
import com.sonarsource.scanner.integrationtester.dsl.issue.Issue;
import com.sonarsource.scanner.integrationtester.dsl.issue.TextRangeIssue;
import com.sonarsource.scanner.integrationtester.runner.ScannerRunner;
import com.sonarsource.scanner.integrationtester.runner.ScannerRunnerConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.test.classpath.TestClasspathUtils;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "comparison.project", matches = ".+")
class NoCompilationComparisonTest {

  private static final Map<String, String> CANDIDATE_PROPERTIES = Map.of();
  private static final String SOURCE_ROOT = "sonar-xml-plugin/src/main/java";
  private static final String SEMANTIC_REPORT_PROPERTY = "sonar.java.internal.semantic.report";
  private static final List<String> TELEMETRY_KEYS = List.of(
    "java.analysis.main.success.size_chars",
    "java.analysis.main.success.type_error_count",
    "java.analysis.main.parse_errors.size_chars",
    "java.analysis.main.exceptions.size_chars");

  @TempDir
  Path workspace;

  @Test
  void scanner_accepts_sources_without_binaries_or_libraries() throws IOException {
    Path project = workspace.resolve("smoke");
    Files.createDirectories(project);
    Files.writeString(project.resolve("Example.java"), "class Example { void example() { int value = 1; ; } }");
    Files.writeString(project.resolve("Other.java"), "class Other {}");

    var context = serverContext(List.of(activeRule(repositoryRoot(), "S1116")));
    var result = scan(context, project, ".", "smoke", Map.of(), List.of("Example.java", "Other.java"));

    assertThat(result.success()).as("Source-only smoke scan: %s", result.error()).isTrue();
    assertThat(result.findings()).hasSize(1);
    assertThat(result.findings().getFirst().rule()).isEqualTo("java:S1116");
    assertThat(result.semantics().totals().total()).isPositive();
  }

  @Test
  void compare_current_and_candidate_source_only_analysis() throws IOException {
    Path checkout = Path.of(System.getProperty("comparison.project")).toAbsolutePath();
    Path sources = checkout.resolve(SOURCE_ROOT);
    assertThat(sources).as("sonar-xml production source directory").isDirectory();

    List<Path> javaFiles;
    try (var files = Files.walk(sources)) {
      javaFiles = files.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java")).sorted().toList();
    }
    assertThat(javaFiles).as("production Java files").isNotEmpty();
    var expectedFiles = javaFiles.stream().map(checkout::relativize).map(Path::toString).map(NoCompilationComparisonTest::normalize).sorted().toList();
    Path currentProject = workspace.resolve("current");
    Path candidateProject = workspace.resolve("candidate");
    for (Path source : javaFiles) {
      byte[] content = Files.readAllBytes(source);
      for (Path project : List.of(currentProject, candidateProject)) {
        Path target = project.resolve(checkout.relativize(source));
        Files.createDirectories(target.getParent());
        Files.write(target, content);
      }
    }

    Path root = repositoryRoot();
    List<ActiveRule> rules;
    try (var files = Files.list(root.resolve("sonar-java-plugin/src/main/resources/profiles/Sonar_way"))) {
      rules = files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
        .filter(key -> key.matches("S\\d+")).sorted().map(key -> activeRule(root, key)).toList();
    }
    assertThat(rules).isNotEmpty();
    var context = serverContext(rules);
    Path reportDirectory = SourceOnlyComparison.createRunDirectory(root
      .resolve("its/scanner-integration-tests/src/test/java/org/sonar/java/it/results"));
    System.out.println("Source-only analysis results: " + reportDirectory);
    var current = scan(context, currentProject, SOURCE_ROOT, "current", Map.of(), expectedFiles);
    var candidate = scan(context, candidateProject, SOURCE_ROOT, "candidate", CANDIDATE_PROPERTIES, expectedFiles);
    var comparison = SourceOnlyComparison.compare(current, candidate, CANDIDATE_PROPERTIES.isEmpty(),
      rules.stream().map(rule -> rule.ruleKey().toString()).toList());
    SourceOnlyComparison.write(reportDirectory, comparison);
    System.out.printf("Source-only comparison: current=%d findings (%d ms), candidate=%d findings (%d ms). Report: %s%n",
      current.findings().size(), current.scanMillis(), candidate.findings().size(), candidate.scanMillis(),
      reportDirectory.resolve("report.md"));

    assertThat(current.success()).as("Current scan: %s", current.error()).isTrue();
    assertThat(candidate.success()).as("Candidate scan: %s", candidate.error()).isTrue();
    assertThat(comparison.valid()).as("Comparison: %s", comparison.error()).isTrue();
    if (CANDIDATE_PROPERTIES.isEmpty()) {
      assertThat(comparison.currentOnly()).isEmpty();
      assertThat(comparison.candidateOnly()).isEmpty();
      assertThat(candidate.semantics()).isEqualTo(current.semantics());
    }
  }

  private static SonarServerContext serverContext(List<ActiveRule> rules) {
    return SonarServerContext.builder()
      .withProduct(SonarServerContext.Product.SERVER)
      .withEngineVersion(EngineVersion.latestRelease())
      .withLanguage("java", "Java", ".java")
      .withPlugin(FileLocation.of(TestClasspathUtils.findModuleJarPath(repositoryRoot().resolve("sonar-java-plugin").toString()).toFile()))
      .withProjectContext(SonarProjectContext.builder().withActiveRules(rules).build())
      .build();
  }

  private static SourceOnlyComparison.Run scan(SonarServerContext context, Path project, String sourceRoot, String label,
                                               Map<String, String> candidateProperties, List<String> expectedFiles) {
    Path semanticReportPath = semanticReportPath(project, label);
    var properties = new TreeMap<>(Map.of(
      "sonar.sources", sourceRoot,
      "sonar.java.source", "21",
      "sonar.java.jdkHome", System.getProperty("java.home"),
      "sonar.java.skipUnchanged", "false",
      "sonar.internal.analysis.autoscan", "false",
      "sonar.internal.analysis.autoscan.filtering", "false",
      SEMANTIC_REPORT_PROPERTY, semanticReportPath.toString()));
    candidateProperties.forEach((key, value) -> {
      if (properties.containsKey(key) || key.endsWith("binaries") || key.endsWith("libraries")) {
        throw new IllegalArgumentException("Candidate properties must preserve source-only comparison settings: " + key);
      }
      properties.put(key, value);
    });
    StringBuilder logs = new StringBuilder("Scanner properties: ").append(properties).append('\n');
    long start = System.nanoTime();
    try {
      Files.createDirectories(semanticReportPath.getParent());
      Files.deleteIfExists(semanticReportPath);
      System.out.println(label + " semantic report: " + semanticReportPath);
      var result = ScannerRunner.run(context, ScannerInput.create("source-only-comparison", project)
        .withScannerProperties(properties).build(), ScannerRunnerConfig.builder().withLogsPrintedToStdOut(false).build());
      long scanMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
      if (result.logOutput() != null) {
        result.logOutput().forEach(log -> {
          logs.append('[').append(log.level()).append("] ").append(log.message()).append('\n');
          if (log.stacktrace() != null) {
            logs.append(log.stacktrace()).append('\n');
          }
        });
      }
      if (!(result instanceof ScannerResultSuccess success) || result.exitCode() != 0) {
        return failed(label, scanMillis, "Scanner exited with code " + result.exitCode() + "\n" + logs);
      }
      var output = success.scannerOutputReader();
      var files = output.getFiles().stream().map(file -> normalize(file.relativePath())).sorted().toList();
      var findings = output.getProject().getAllIssues().stream().map(NoCompilationComparisonTest::finding).toList();
      var telemetry = new TreeMap<String, String>();
      for (String key : TELEMETRY_KEYS) {
        var entry = output.getProject().getTelemetryEntry(key);
        if (entry != null) {
          telemetry.put(key, entry.value());
        }
      }
      boolean complete = files.equals(expectedFiles);
      SemanticReport semantics = SemanticReport.read(semanticReportPath, expectedFiles);
      return new SourceOnlyComparison.Run(label, complete, scanMillis, files, findings, telemetry,
        semantics,
        complete ? null : "Indexed files differ from the intended production Java files. Expected: " + expectedFiles + "; actual: " + files);
    } catch (IOException | RuntimeException e) {
      var stacktrace = new StringWriter();
      e.printStackTrace(new PrintWriter(stacktrace));
      return failed(label, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), stacktrace.toString());
    }
  }

  private static SourceOnlyComparison.Run failed(String label, long scanMillis, String error) {
    return new SourceOnlyComparison.Run(label, false, scanMillis, List.of(), List.of(), Map.of(), null, error);
  }

  static Path semanticReportPath(Path project, String label) {
    String configured = System.getProperty(SEMANTIC_REPORT_PROPERTY);
    if (configured == null || label.equals("smoke")) {
      return project.resolve("semantic-report.json").toAbsolutePath();
    }
    Path base = Path.of(configured).toAbsolutePath();
    String name = base.getFileName().toString();
    int extension = name.lastIndexOf('.');
    String prefix = extension < 0 ? name : name.substring(0, extension);
    String suffix = extension < 0 ? "" : name.substring(extension);
    return base.resolveSibling(prefix + "-" + label + suffix);
  }

  private static SourceOnlyComparison.Finding finding(Issue issue) {
    String path = issue instanceof com.sonarsource.scanner.integrationtester.dsl.issue.FileIssue fileIssue
      ? normalize(fileIssue.componentPath()) : "<project>";
    Integer line = issue instanceof TextRangeIssue rangeIssue ? rangeIssue.line() : null;
    return new SourceOnlyComparison.Finding(issue.ruleKey(), path, line, issue.message());
  }

  private static String normalize(String path) {
    return path.replace('\\', '/');
  }

  private static ActiveRule activeRule(Path root, String key) {
    Path metadata = root.resolve("sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java").resolve(key + ".json");
    try {
      var json = JsonParser.parseString(Files.readString(metadata)).getAsJsonObject();
      return ActiveRule.builder().withKey(RuleKey.of("java", key)).withLanguageKey("java")
        .withName(json.get("title").getAsString())
        .withSeverity(ActiveRule.Severity.valueOf(json.get("defaultSeverity").getAsString().toUpperCase(Locale.ROOT))).build();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load rule metadata: " + metadata, e);
    }
  }

  private static Path repositoryRoot() {
    Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (directory != null && !Files.isDirectory(directory.resolve("sonar-java-plugin/src/main/resources/profiles/Sonar_way"))) {
      directory = directory.getParent();
    }
    if (directory == null) {
      throw new IllegalStateException("Run this test from the sonar-java checkout.");
    }
    return directory;
  }
}
