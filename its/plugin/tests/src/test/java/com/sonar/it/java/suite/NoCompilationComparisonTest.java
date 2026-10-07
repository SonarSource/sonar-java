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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.jar.JarFile;
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

  private static final String SOURCE_ROOT = "sonar-xml-plugin/src/main/java";
  private static final String SEMANTIC_REPORT_PROPERTY = "sonar.java.internal.semantic.report";
  private static final Pattern TELEMETRY = Pattern.compile("Telemetry (java\\.analysis\\.main\\.[\\w.]+): (\\S+)");
  private static final Pattern ANALYZER_TIME = Pattern.compile("Sensor JavaSensor \\[java\\] \\(done\\) \\| time=(\\d+)ms");
  private static final Pattern BINDING_COMPLETION = Pattern.compile("Semantic binding probes: (\\d+) checked, (\\d+) failed\\.");
  private static OrchestratorRule orchestrator;
  private static ComparisonSettings settings;
  private static Path serverWorkspace;
  private static Path pluginJar;

  @TempDir
  Path workspace;

  @BeforeAll
  static void startServer() throws IOException {
    settings = ComparisonSettings.load();
    Path target = repositoryRoot().resolve("its/plugin/tests/target");
    Files.createDirectories(target);
    serverWorkspace = Files.createTempDirectory(target, "comparison-orchestrator-");
    String configuredPlugin = System.getProperty("comparison.pluginJar");
    pluginJar = configuredPlugin == null ? TestClasspathUtils.findModuleJarPath(repositoryRoot().resolve("sonar-java-plugin").toString())
      : Path.of(configuredPlugin).toAbsolutePath();
    assertThat(pluginJar).as("Analyzer plugin containing semantic reporting and PR #6308 source-path support").isRegularFile();
    orchestrator = OrchestratorRule.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(settings.serverVersion())
      .setOrchestratorProperty("orchestrator.workspaceDir", serverWorkspace.toString())
      .setEdition(Edition.COMMUNITY)
      .addPlugin(FileLocation.of(pluginJar.toFile()))
      .addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-semantic-bindings.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-semantic-bindings-unresolved.xml"))
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
    MessageDigest snapshot = sha256();
    for (Path source : javaFiles) {
      byte[] content = Files.readAllBytes(source);
      snapshot.update(normalize(checkout.relativize(source).toString()).getBytes(StandardCharsets.UTF_8));
      snapshot.update((byte) 0);
      snapshot.update(content);
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
    var metadata = runMetadata(checkout, HexFormat.of().formatHex(snapshot.digest()));
    var datasetCandidate = candidateProperties(List.of(SOURCE_ROOT));
    var bindingsCurrent = checkBindings(workspace.resolve("bindings-current"), "bindings-current", Map.of());
    var bindingsCandidate = checkBindings(workspace.resolve("bindings-candidate"), "bindings-candidate", candidateProperties(List.of(".")));
    var sourcepathCurrent = checkDependencyBindings(workspace.resolve("sourcepath-current"), "sourcepath-current", false);
    var sourcepathCandidate = checkDependencyBindings(workspace.resolve("sourcepath-candidate"), "sourcepath-candidate", true);
    metadata.put("Binding correctness/current", bindingSummary(bindingsCurrent));
    metadata.put("Binding correctness/candidate", bindingSummary(bindingsCandidate));
    metadata.put("Source-path fixture/current (project bindings expected unknown)", bindingSummary(sourcepathCurrent));
    metadata.put("Source-path fixture/candidate (project bindings expected known)", bindingSummary(sourcepathCandidate));
    metadata.put("Source-path fixture/analyzed files", "consumer/src/main/java/bindings/BindingFixture.java only; dependency sources excluded");
    recordFixtureCounts(metadata, "current", sourcepathCurrent);
    recordFixtureCounts(metadata, "candidate", sourcepathCandidate);
    metadata.put("Current source paths", "none");
    metadata.put("Candidate source paths", SOURCE_ROOT);
    if (settings.repetitions() > 1) {
      var warmupCurrent = scan(currentProject, SOURCE_ROOT, "warmup-current", Map.of(), expectedFiles);
      var warmupCandidate = scan(candidateProject, SOURCE_ROOT, "warmup-candidate", datasetCandidate, expectedFiles);
      assertThat(warmupCurrent.success()).as("Current warm-up: %s", warmupCurrent.error()).isTrue();
      assertThat(warmupCandidate.success()).as("Candidate warm-up: %s", warmupCandidate.error()).isTrue();
    }
    var currentSamples = new ArrayList<SourceOnlyComparison.Run>();
    var candidateSamples = new ArrayList<SourceOnlyComparison.Run>();
    for (int i = 0; i < settings.repetitions(); i++) {
      if (i % 2 == 0) {
        currentSamples.add(scan(currentProject, SOURCE_ROOT, "current", Map.of(), expectedFiles));
        candidateSamples.add(scan(candidateProject, SOURCE_ROOT, "candidate", datasetCandidate, expectedFiles));
      } else {
        candidateSamples.add(scan(candidateProject, SOURCE_ROOT, "candidate", datasetCandidate, expectedFiles));
        currentSamples.add(scan(currentProject, SOURCE_ROOT, "current", Map.of(), expectedFiles));
      }
    }
    var current = representative(currentSamples);
    var candidate = representative(candidateSamples);
    var comparison = SourceOnlyComparison.compare(current, candidate, false, rules);
    boolean stable = stable(currentSamples, rules) && stable(candidateSamples, rules);
    metadata.put("Repeated result stability", stable ? "stable" : "unstable");
    if (!stable) {
      comparison = new SourceOnlyComparison.Comparison(false, false,
        "Measured repetitions produced inconsistent findings or semantics.", current, candidate, List.of(), List.of(), List.of());
    }
    SourceOnlyComparison.write(reportDirectory, comparison, metadata, currentSamples, candidateSamples);
    System.out.println("Source-only comparison report: " + reportDirectory.resolve("report.md"));
    assertThat(current.success()).as("Current scan: %s", current.error()).isTrue();
    assertThat(candidate.success()).as("Candidate scan: %s", candidate.error()).isTrue();
    assertThat(comparison.valid()).as("Comparison: %s", comparison.error()).isTrue();
    assertBindingProbe(bindingsCurrent);
    assertBindingProbe(bindingsCandidate);
    assertBindingProbe(sourcepathCurrent);
    assertBindingProbe(sourcepathCandidate);
    assertThat(sourcepathCandidate.semantics().totals().unknown()).as("Source paths must reduce unresolved project references in the targeted fixture")
      .isLessThan(sourcepathCurrent.semantics().totals().unknown());
  }

  static Map<String, String> sourcePathProperties(Map<String, String> extraProperties, List<String> roots) {
    var properties = new TreeMap<>(extraProperties);
    properties.put("sonar.java.sourcepath", String.join(",", roots));
    return Map.copyOf(properties);
  }

  private static Map<String, String> candidateProperties(List<String> roots) {
    return sourcePathProperties(settings.candidateProperties(), roots);
  }

  private static SourceOnlyComparison.Run scan(Path project, String sourceRoot, String label,
                                               Map<String, String> candidateProperties, List<String> expectedFiles) {
    return scan(project, sourceRoot, label, candidateProperties, expectedFiles, "Sonar way");
  }

  private static SourceOnlyComparison.Run scan(Path project, String sourceRoot, String label,
                                               Map<String, String> candidateProperties, List<String> expectedFiles, String profile) {
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
      TestUtils.provisionProject(orchestrator, projectKey, projectKey, "java", profile);
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
          if (issue.getRule().startsWith("java:") || issue.getRule().equals("java-extension:semanticbindings")) {
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
      var analyzer = ANALYZER_TIME.matcher(result.getLogs());
      long analyzerMillis = 0;
      boolean hasAnalyzerTime = false;
      while (analyzer.find()) {
        analyzerMillis += Long.parseLong(analyzer.group(1));
        hasAnalyzerTime = true;
      }
      if (hasAnalyzerTime) {
        telemetry.put("comparison.analyzer.time_ms", Long.toString(analyzerMillis));
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
              <version>%s</version>
            </plugin>
          </plugins>
        </build>
      </project>
      """.formatted(sourceRoot, settings.scannerVersion()));
  }

  private static SourceOnlyComparison.Run representative(List<SourceOnlyComparison.Run> samples) {
    return samples.stream().filter(run -> !run.success()).findFirst().orElse(samples.getFirst());
  }

  private static boolean stable(List<SourceOnlyComparison.Run> samples, List<String> rules) {
    var reference = samples.getFirst();
    return samples.stream().allMatch(run -> {
      var comparison = SourceOnlyComparison.compare(reference, run, true, rules);
      return comparison.valid() && comparison.currentOnly().isEmpty() && comparison.candidateOnly().isEmpty()
        && reference.semantics().equals(run.semantics());
    });
  }

  private static SourceOnlyComparison.Run checkBindings(Path project, String label, Map<String, String> candidateProperties) throws IOException {
    Path sources = project.resolve("bindings");
    Files.createDirectories(sources);
    Files.writeString(sources.resolve("BindingFixture.java"), """
      package bindings;
      class BindingFixture extends BindingParent {
        void check() {
          String text = "x";
          BindingHelper helper = new BindingHelper();
          int fieldValue = inheritedValue;
          String result = helper.select(text);
          MissingType missing = null;
        }
      }
      """);
    Files.writeString(sources.resolve("BindingHelper.java"), """
      package bindings;
      class BindingHelper {
        String select(String value) { return value; }
        int select(int value) { return value; }
      }
      class BindingParent { protected int inheritedValue; }
      """);
    return scan(project, ".", label, candidateProperties, List.of("bindings/BindingFixture.java", "bindings/BindingHelper.java"), "semantic-bindings");
  }

  private static SourceOnlyComparison.Run checkDependencyBindings(Path project, String label, boolean sourcePaths) throws IOException {
    Path consumer = project.resolve("consumer/src/main/java/bindings");
    Path dependency = project.resolve("dependency/src/main/java/bindings");
    Files.createDirectories(consumer);
    Files.createDirectories(dependency);
    Files.writeString(consumer.resolve("BindingFixture.java"), """
      package bindings;
      class BindingFixture extends BindingParent {
        void check() {
          String text = "x";
          BindingHelper helper = new BindingHelper();
          int fieldValue = inheritedValue;
          String result = helper.select(text);
          MissingType missing = null;
        }
      }
      """);
    Files.writeString(dependency.resolve("BindingHelper.java"), """
      package bindings;
      public class BindingHelper {
        public String select(String value) { return value; }
        public int select(int value) { return value; }
      }
      """);
    Files.writeString(dependency.resolve("BindingParent.java"), "package bindings; public class BindingParent { protected int inheritedValue; }");
    var properties = sourcePaths ? candidateProperties(List.of("consumer/src/main/java", "dependency/src/main/java")) : Map.<String, String>of();
    return scan(project, "consumer/src/main/java", label, properties, List.of("consumer/src/main/java/bindings/BindingFixture.java"),
      sourcePaths ? "semantic-bindings" : "semantic-bindings-unresolved");
  }

  private static String bindingSummary(SourceOnlyComparison.Run run) {
    if (!run.success()) {
      return "unavailable: " + run.error();
    }
    return run.findings().stream().map(SourceOnlyComparison.Finding::message)
      .filter(message -> message.startsWith("Semantic binding "))
      .sorted().collect(java.util.stream.Collectors.joining("; "));
  }

  private static void recordFixtureCounts(Map<String, String> metadata, String mode, SourceOnlyComparison.Run run) {
    metadata.put("Source-path fixture/" + mode + " files", Integer.toString(run.files().size()));
    metadata.put("Source-path fixture/" + mode + " identifiers", run.semantics() == null ? "N/A" : Integer.toString(run.semantics().totals().total()));
    metadata.put("Source-path fixture/" + mode + " known identifiers", run.semantics() == null ? "N/A" : Integer.toString(run.semantics().totals().known()));
    metadata.put("Source-path fixture/" + mode + " unknown identifiers", run.semantics() == null ? "N/A" : Integer.toString(run.semantics().totals().unknown()));
  }

  private static void assertBindingProbe(SourceOnlyComparison.Run run) {
    assertThat(run.success()).as("%s binding scan: %s", run.label(), run.error()).isTrue();
    var completions = run.findings().stream().map(SourceOnlyComparison.Finding::message)
      .filter(message -> BINDING_COMPLETION.matcher(message).matches()).toList();
    assertThat(completions).as("Binding probe must execute and emit exactly one completion").hasSize(1);
    var completion = BINDING_COMPLETION.matcher(completions.getFirst());
    assertThat(completion.matches()).isTrue();
    assertThat(Integer.parseInt(completion.group(1))).as("All five expected binding probes must execute").isEqualTo(5);
    assertThat(Integer.parseInt(completion.group(2))).as("Exact semantic bindings must match the golden fixture: %s", bindingSummary(run)).isZero();
  }

  private static Map<String, String> runMetadata(Path checkout, String snapshot) throws IOException {
    var metadata = new TreeMap<String, String>();
    metadata.put("Recorded at (UTC)", Instant.now().toString());
    metadata.put("Test harness revision", git(repositoryRoot(), "rev-parse", "HEAD"));
    metadata.put("Test harness dirty", Boolean.toString(!git(repositoryRoot(), "status", "--porcelain").isBlank()));
    String analyzerCheckout = System.getProperty("comparison.analyzerCheckout");
    metadata.put("Analyzer checkout revision", analyzerCheckout == null ? "not supplied; identify artifact by SHA-256"
      : git(Path.of(analyzerCheckout), "rev-parse", "HEAD"));
    metadata.put("Analyzer checkout dirty", analyzerCheckout == null ? "not supplied"
      : Boolean.toString(!git(Path.of(analyzerCheckout), "status", "--porcelain").isBlank()));
    metadata.put("Target checkout revision", git(checkout, "rev-parse", "HEAD"));
    metadata.put("Target checkout dirty", Boolean.toString(!git(checkout, "status", "--porcelain").isBlank()));
    metadata.put("Target source snapshot SHA-256", snapshot);
    metadata.put("Analyzer plugin SHA-256", HexFormat.of().formatHex(sha256().digest(Files.readAllBytes(pluginJar))));
    metadata.put("Binding probe plugin SHA-256", HexFormat.of().formatHex(sha256().digest(Files.readAllBytes(TestUtils.pluginJar("java-extension-plugin").toPath()))));
    try (var jar = new JarFile(pluginJar.toFile())) {
      metadata.put("Analyzer plugin version", jar.getManifest().getMainAttributes().getValue("Plugin-Version"));
      metadata.put("Analyzer build manifest revision", java.util.Objects.toString(jar.getManifest().getMainAttributes().getValue("Implementation-Build"), "not recorded"));
    }
    metadata.put("Analyzer plugin path", pluginJar.toString());
    metadata.put("Server version", settings.serverVersion());
    metadata.put("Maven scanner version", settings.scannerVersion());
    metadata.put("JDK", System.getProperty("java.version") + " / " + System.getProperty("java.vendor"));
    metadata.put("Java language level", "21");
    metadata.put("Profile", "Sonar way");
    metadata.put("Candidate properties", new TreeMap<>(candidateProperties(List.of(SOURCE_ROOT))).toString());
    metadata.put("Candidate feature", "PR #6308 source-path resolution enabled; same plugin used for both runs");
    metadata.put("Measured repetitions per mode", Integer.toString(settings.repetitions()));
    metadata.put("Timing protocol", settings.repetitions() > 1 ? "one warm-up per mode excluded; measured order alternates current/candidate" : "single measured pair; no warm-up");
    metadata.put("Server workspace", serverWorkspace.toString());
    return metadata;
  }

  private static String git(Path checkout, String... arguments) {
    var command = new ArrayList<>(List.of("git", "-C", checkout.toString()));
    command.addAll(List.of(arguments));
    try {
      var process = new ProcessBuilder(command).redirectErrorStream(true).start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
      return process.waitFor() == 0 ? output : "unavailable";
    } catch (IOException e) {
      return "unavailable";
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while recording Git metadata", e);
    }
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
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
