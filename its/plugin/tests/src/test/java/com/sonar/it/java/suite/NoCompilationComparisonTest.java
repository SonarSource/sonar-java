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

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonarqube.ws.client.components.TreeRequest;
import org.sonarqube.ws.client.issues.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "comparison.project", matches = ".+")
class NoCompilationComparisonTest {

  private static final Map<String, String> CANDIDATE_PROPERTIES = Map.of();
  private static final String SOURCE_ROOT = "sonar-xml-plugin/src/main/java";
  private static final String SEMANTIC_REPORT_PROPERTY = "sonar.java.internal.semantic.report";
  private static final Pattern TELEMETRY = Pattern.compile("Telemetry (java\\.analysis\\.main\\.[\\w.]+): (\\S+)");
  private static OrchestratorRule orchestrator;

  @TempDir
  Path workspace;

  @BeforeAll
  static void startServer() {
    orchestrator = OrchestratorRule.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
      .setEdition(Edition.COMMUNITY)
      .addPlugin(FileLocation.of(TestClasspathUtils.findModuleJarPath(repositoryRoot().resolve("sonar-java-plugin").toString()).toFile()))
      .build();
    orchestrator.start();
  }

  @AfterAll
  static void stopServer() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  void scanner_accepts_sources_without_binaries_or_libraries() throws IOException {
    Path project = workspace.resolve("smoke");
    Files.createDirectories(project);
    Files.writeString(project.resolve("Example.java"), "class Example { void example() { int value = 1; ; } }");
    Files.writeString(project.resolve("Other.java"), "class Other {}");
    var result = scan(project, ".", "smoke", Map.of(), List.of("Example.java", "Other.java"));
    assertThat(result.success()).as("Source-only smoke scan: %s", result.error()).isTrue();
    assertThat(result.findings()).anySatisfy(finding -> assertThat(finding.rule()).isEqualTo("java:S1116"));
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
    List<String> rules;
    try (var files = Files.list(root.resolve("sonar-java-plugin/src/main/resources/profiles/Sonar_way"))) {
      rules = files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
        .filter(key -> key.matches("S\\d+")).sorted().map(key -> "java:" + key).toList();
    }
    Path reportDirectory = SourceOnlyComparison.createRunDirectory(root
      .resolve("its/plugin/tests/src/test/java/com/sonar/it/java/suite/results"));
    System.out.println("Source-only analysis results: " + reportDirectory);
    var current = scan(currentProject, SOURCE_ROOT, "current", Map.of(), expectedFiles);
    var candidate = scan(candidateProject, SOURCE_ROOT, "candidate", CANDIDATE_PROPERTIES, expectedFiles);
    var comparison = SourceOnlyComparison.compare(current, candidate, CANDIDATE_PROPERTIES.isEmpty(), rules);
    SourceOnlyComparison.write(reportDirectory, comparison);
    System.out.println("Source-only comparison report: " + reportDirectory.resolve("report.md"));
    assertThat(current.success()).as("Current scan: %s", current.error()).isTrue();
    assertThat(candidate.success()).as("Candidate scan: %s", candidate.error()).isTrue();
    assertThat(comparison.valid()).as("Comparison: %s", comparison.error()).isTrue();
    if (CANDIDATE_PROPERTIES.isEmpty()) {
      assertThat(comparison.currentOnly()).isEmpty();
      assertThat(comparison.candidateOnly()).isEmpty();
      assertThat(candidate.semantics()).isEqualTo(current.semantics());
    }
  }

  private static SourceOnlyComparison.Run scan(Path project, String sourceRoot, String label,
                                               Map<String, String> candidateProperties, List<String> expectedFiles) {
    Path semanticReportPath = semanticReportPath(project, label);
    String projectKey = "source-only-" + label + "-" + UUID.randomUUID();
    var properties = new TreeMap<>(Map.of(
      "sonar.projectKey", projectKey,
      "sonar.sources", sourceRoot,
      "sonar.tests", "",
      "sonar.java.source", "21",
      "sonar.java.jdkHome", System.getProperty("java.home"),
      "sonar.java.binaries", "",
      "sonar.java.libraries", "",
      "sonar.java.skipUnchanged", "false",
      "sonar.internal.analysis.autoscan", "false",
      "sonar.internal.analysis.autoscan.filtering", "false"));
    candidateProperties.forEach((key, value) -> {
      if (properties.containsKey(key) || key.endsWith("binaries") || key.endsWith("libraries") || key.equals(SEMANTIC_REPORT_PROPERTY)) {
        throw new IllegalArgumentException("Candidate properties must preserve source-only comparison settings: " + key);
      }
      properties.put(key, value);
    });
    long start = System.nanoTime();
    try {
      Files.createDirectories(semanticReportPath.getParent());
      Files.deleteIfExists(semanticReportPath);
      writePom(project, sourceRoot);
      TestUtils.provisionProject(orchestrator, projectKey, projectKey, "java", "Sonar way");
      MavenBuild build = TestUtils.createMavenBuild()
        .setPom(project.resolve("pom.xml").toFile())
        .setProperties(properties)
        .setProperty(SEMANTIC_REPORT_PROPERTY, semanticReportPath.toString())
        .setProperty("sonar.verbose", "true")
        .setProperty("org.slf4j.simpleLogger.log.org.sonarsource", "debug")
        .setProperty("sonar.scm.disabled", "true")
        .setProperty("style.color", "never")
        .setGoals("sonar:sonar");
      System.out.println(label + " semantic report: " + semanticReportPath);
      start = System.nanoTime();
      var result = orchestrator.executeBuild(build);
      long scanMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
      if (!result.isSuccess()) {
        return failed(label, scanMillis, result.getLogs());
      }
      SemanticReport semantics = SemanticReport.read(semanticReportPath, expectedFiles);
      var client = TestUtils.newAdminWsClient(orchestrator);
      var files = new ArrayList<String>();
      var pathsByComponent = new HashMap<String, String>();
      int page = 1;
      int components = 0;
      int total;
      do {
        var response = client.components().tree(new TreeRequest().setComponent(projectKey).setStrategy("all").setQualifiers(List.of("FIL"))
          .setPs("500").setP(Integer.toString(page++)));
        for (var component : response.getComponentsList()) {
          String path = normalize(component.getPath());
          pathsByComponent.put(component.getKey(), path);
          if (component.getLanguage().equals("java")) {
            files.add(path);
          }
        }
        components += response.getComponentsCount();
        total = response.getPaging().getTotal();
      } while (components < total);
      var findings = new ArrayList<SourceOnlyComparison.Finding>();
      page = 1;
      int issues = 0;
      do {
        var response = client.issues().search(new SearchRequest().setComponentKeys(List.of(projectKey))
          .setPs("500").setP(Integer.toString(page++)));
        for (var issue : response.getIssuesList()) {
          if (issue.getRule().startsWith("java:")) {
            String path = issue.getComponent().equals(projectKey) ? "<project>" : pathsByComponent.get(issue.getComponent());
            if (path == null) {
              throw new IllegalStateException("Issue component is missing from indexed files: " + issue.getComponent());
            }
            findings.add(new SourceOnlyComparison.Finding(issue.getRule(), path, issue.hasLine() ? issue.getLine() : null, issue.getMessage()));
          }
        }
        issues += response.getIssuesCount();
        total = response.getPaging().getTotal();
      } while (issues < total);
      var telemetry = new TreeMap<String, String>();
      var matcher = TELEMETRY.matcher(result.getLogs());
      while (matcher.find()) {
        telemetry.put(matcher.group(1), matcher.group(2));
      }
      files.sort(String::compareTo);
      boolean complete = files.equals(expectedFiles);
      return new SourceOnlyComparison.Run(label, complete, scanMillis, files, findings, telemetry, semantics,
        complete ? null : "Indexed files differ from the intended Java files. Expected: " + expectedFiles + "; actual: " + files);
    } catch (IOException | RuntimeException e) {
      var stacktrace = new StringWriter();
      e.printStackTrace(new PrintWriter(stacktrace));
      return failed(label, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), stacktrace.toString());
    }
  }

  private static void writePom(Path project, String sourceRoot) throws IOException {
    Files.writeString(project.resolve("pom.xml"), """
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>org.sonarsource.it</groupId>
        <artifactId>source-only-comparison</artifactId>
        <version>1.0</version>
        <properties>
          <maven.compiler.release>21</maven.compiler.release>
          <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        </properties>
        <build>
          <sourceDirectory>%s</sourceDirectory>
          <plugins>
            <plugin>
              <groupId>org.sonarsource.scanner.maven</groupId>
              <artifactId>sonar-maven-plugin</artifactId>
            </plugin>
          </plugins>
        </build>
      </project>
      """.formatted(sourceRoot));
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

  private static String normalize(String path) {
    return path.replace('\\', '/');
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
