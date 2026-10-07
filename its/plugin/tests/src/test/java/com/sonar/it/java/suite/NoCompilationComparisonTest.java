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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
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
    assertThat(pluginJar).as("Analyzer plugin containing semantic reporting and PRs #6308/#6309").isRegularFile();
    orchestrator = OrchestratorRule.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(settings.serverVersion())
      .setOrchestratorProperty("orchestrator.workspaceDir", serverWorkspace.toString())
      .setEdition(Edition.COMMUNITY)
      .addPlugin(FileLocation.of(pluginJar.toFile()))
      .addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-semantic-bindings.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-semantic-bindings-unresolved.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-semantic-bindings-clean-unresolved.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-semantic-bindings-clean-sourcepaths.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-semantic-bindings-clean-bytecode.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-semantic-bindings-fallback.xml"))
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
    var result = scan(project, ".", "smoke", AnalysisMode.BASELINE.properties(List.of("."), Map.of()), List.of("Example.java", "Other.java"));
    assertThat(result.success()).as("Source-only smoke scan: %s", result.error()).isTrue();
    assertThat(result.findings()).anySatisfy(finding -> assertThat(finding.rule()).isEqualTo("java:S1116"));
    assertThat(result.semantics().totals().total()).isPositive();
  }

  @Test
  void compare_analysis_modes_without_project_build() throws IOException {
    Path checkout = Path.of(System.getProperty("comparison.project")).toAbsolutePath();
    var scope = RepositoryScope.discover(checkout);
    var expectedFiles = scope.expectedFiles();
    assertThat(expectedFiles).as("production Java files").isNotEmpty();
    var sourceContents = new LinkedHashMap<String, byte[]>();
    MessageDigest snapshot = sha256();
    for (String path : expectedFiles) {
      byte[] content = Files.readAllBytes(checkout.resolve(path));
      sourceContents.put(path, content);
      snapshot.update(path.getBytes(StandardCharsets.UTF_8));
      snapshot.update((byte) 0);
      snapshot.update(content);
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
    long resolutionStarted = System.nanoTime();
    var libraries = AnalysisDataset.freezeLibraries(AnalysisDataset.resolveProjectLibraries(checkout, workspace.resolve("dependency-resolution"),
      System.getProperty("maven.binary", "mvn"), scope), workspace.resolve("libraries"));
    metadata.put("Dependency resolution time (ms), excluded from scans", Long.toString(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - resolutionStarted)));
    metadata.put("Dependency resolution", "Pinned dependency:build-classpath on copied POMs; compile scope only; no Maven compilation");
    metadata.put("Analyzed project", scope.name());
    metadata.put("Module scope", scope.modulePaths().toString());
    metadata.put("Analyzed Java files", Integer.toString(expectedFiles.size()));
    metadata.put("Scan topology", "Native synthetic Maven reactor; real production module boundaries; no project dependencies or supplied project bytecode");
    metadata.put("Classpath policy", "External compile-scope JARs only, preserving each module's versions and order; cached project module artifacts excluded");
    recordClasspath(metadata, libraries.union());
    var fixtures = scanFixtures();
    for (FixtureScan fixture : fixtures) {
      metadata.put(fixture.fixture().name() + "/" + fixture.mode().label() + " bindings", bindingSummary(fixture.run()));
    }
    var datasets = new ArrayList<AnalysisComparisonReport.DatasetResult>();
    for (AnalysisDataset dataset : settings.selectDatasets(AnalysisDataset.forProject(scope, libraries.union()))) {
      datasets.add(measureDataset(dataset, sourceContents, expectedFiles, rules, scope, libraries.byModule()));
    }
    var scenarios = fixtureScenarios(fixtures);
    AnalysisComparisonReport.write(reportDirectory, datasets, rules, metadata, scenarios);
    System.out.println("Analysis comparison report: " + reportDirectory.resolve("report.md"));
    for (var dataset : datasets) {
      assertThat(dataset.metadata()).as("%s repetitions", dataset.dataset().label()).containsEntry("Repeated result stability", "stable");
      var baseline = representative(dataset.samples().get(AnalysisMode.BASELINE));
      for (AnalysisMode mode : AnalysisMode.values()) {
        var result = representative(dataset.samples().get(mode));
        assertThat(result.success()).as("%s/%s scan: %s", dataset.dataset().label(), mode.label(), result.error()).isTrue();
        var comparison = SourceOnlyComparison.compare(baseline, result, false, rules);
        assertThat(comparison.valid()).as("%s/%s comparison: %s", dataset.dataset().label(), mode.label(), comparison.error()).isTrue();
        assertCompilationMeasurement(result, mode);
        if (mode.compilation() && dataset.dataset().expectCompilationSuccess()) {
          assertThat(result.telemetry()).as("Compilation must match the dataset's expected behavior")
            .containsEntry("comparison.compilation.status", "SUCCESS");
        }
      }
    }
    fixtures.forEach(NoCompilationComparisonTest::assertFixture);
  }

  private AnalysisComparisonReport.DatasetResult measureDataset(AnalysisDataset dataset, Map<String, byte[]> sources,
                                                                 List<String> expectedFiles, List<String> rules, RepositoryScope scope,
                                                                 Map<String, List<Path>> librariesByModule) throws IOException {
    var projects = new EnumMap<AnalysisMode, Path>(AnalysisMode.class);
    var samples = new EnumMap<AnalysisMode, List<SourceOnlyComparison.Run>>(AnalysisMode.class);
    var metadata = new TreeMap<String, String>();
    metadata.put("Scenario", dataset.description());
    metadata.put("External library count", Integer.toString(dataset.libraries().size()));
    metadata.put("Shared scenario properties", new TreeMap<>(dataset.sharedProperties()).toString());
    var moduleLibraries = dataset.libraries().isEmpty() ? Map.<String, List<Path>>of() : librariesByModule;
    for (var module : scope.modules()) {
      metadata.put("Module " + module.path() + " external classpath", moduleLibraries.getOrDefault(module.path(), List.of()).toString());
    }
    for (AnalysisMode mode : AnalysisMode.values()) {
      Path project = workspace.resolve(dataset.id()).resolve(mode.id());
      projects.put(mode, project);
      samples.put(mode, new ArrayList<>());
      for (var source : sources.entrySet()) {
        Path path = project.resolve(source.getKey());
        Files.createDirectories(path.getParent());
        Files.write(path, source.getValue());
      }
      metadata.put(mode.label() + " properties", new TreeMap<>(datasetProperties(dataset, mode, project, scope)).toString());
    }
    if (settings.repetitions() > 1) {
      for (AnalysisMode mode : AnalysisMode.values()) {
        var warmup = scan(projects.get(mode), "", "warmup-" + dataset.id() + "-" + mode.id(),
          datasetProperties(dataset, mode, projects.get(mode), scope), expectedFiles, "Sonar way", List.of(), scope, moduleLibraries);
        metadata.put(mode.label() + " warm-up status", warmup.success() ? "SUCCESS" : "FAILED: " + warmup.error());
      }
    }
    for (int i = 0; i < settings.repetitions(); i++) {
      for (AnalysisMode mode : measuredOrder(i)) {
        samples.get(mode).add(scan(projects.get(mode), "", dataset.id() + "-" + mode.id(),
          datasetProperties(dataset, mode, projects.get(mode), scope), expectedFiles, "Sonar way", List.of(), scope, moduleLibraries));
      }
    }
    metadata.put("Repeated result stability", samples.values().stream().allMatch(runs -> stable(runs, rules)) ? "stable" : "unstable");
    metadata.put("Graph diagnostic stability", samples.values().stream().allMatch(NoCompilationComparisonTest::stableGraph) ? "stable" : "variable");
    return new AnalysisComparisonReport.DatasetResult(dataset, samples, metadata);
  }

  private static Map<String, String> datasetProperties(AnalysisDataset dataset, AnalysisMode mode, Path project, RepositoryScope scope) {
    var roots = scope.sourceRoots().stream().map(root -> project.resolve(root).toAbsolutePath().toString()).toList();
    var properties = new TreeMap<>(mode.properties(roots, settings.candidateProperties()));
    properties.putAll(dataset.sharedProperties());
    return Map.copyOf(properties);
  }

  private static void recordClasspath(Map<String, String> metadata, List<Path> libraries) throws IOException {
    var digest = sha256();
    for (int i = 0; i < libraries.size(); i++) {
      Path library = libraries.get(i);
      byte[] content = Files.readAllBytes(library);
      digest.update(library.getFileName().toString().getBytes(StandardCharsets.UTF_8));
      digest.update((byte) 0);
      digest.update(content);
      metadata.put("Compile dependency " + i, library.getFileName() + " / SHA-256 " + HexFormat.of().formatHex(sha256().digest(content)));
    }
    metadata.put("Compile classpath SHA-256 (ordered)", HexFormat.of().formatHex(digest.digest()));
  }

  static List<AnalysisMode> measuredOrder(int repetition) {
    var order = new ArrayList<>(List.of(AnalysisMode.values()));
    if (repetition % 2 != 0) {
      Collections.reverse(order);
    }
    Collections.rotate(order, -(repetition / 2 % order.size()));
    return List.copyOf(order);
  }

  private static SourceOnlyComparison.Run scan(Path project, String sourceRoot, String label,
                                               Map<String, String> candidateProperties, List<String> expectedFiles) {
    return scan(project, sourceRoot, label, candidateProperties, expectedFiles, "Sonar way");
  }

  private static SourceOnlyComparison.Run scan(Path project, String sourceRoot, String label,
                                               Map<String, String> candidateProperties, List<String> expectedFiles, String profile) {
    return scan(project, sourceRoot, label, candidateProperties, expectedFiles, profile, List.of());
  }

  private static SourceOnlyComparison.Run scan(Path project, String sourceRoot, String label,
                                               Map<String, String> candidateProperties, List<String> expectedFiles, String profile, List<Path> libraries) {
    return scan(project, sourceRoot, label, candidateProperties, expectedFiles, profile, libraries, null, Map.of());
  }

  private static SourceOnlyComparison.Run scan(Path project, String sourceRoot, String label,
                                               Map<String, String> candidateProperties, List<String> expectedFiles, String profile, List<Path> libraries,
                                               RepositoryScope scope, Map<String, List<Path>> moduleLibraries) {
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
    properties.put("sonar.java.test.binaries", "");
    properties.put("sonar.java.test.libraries", "");
    properties.put("sonar.java.libraries", libraries.stream().map(Path::toString).collect(java.util.stream.Collectors.joining(",")));
    properties.put("sonar.working.directory", project.resolve("scanner-work").toAbsolutePath().toString());
    properties.put("sonar.java.internal.semantic.report.graph.maxExpansions", Integer.toString(settings.graphLimit()));
    if (scope != null) {
      properties.remove("sonar.sources");
      properties.remove("sonar.java.libraries");
    }
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
      if (scope == null) {
        writePom(project, sourceRoot);
      } else {
        ReactorProject.write(project, scope, moduleLibraries, settings.scannerVersion());
      }
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
      telemetry.putAll(CompilationMeasurement.read(result.getLogs(), Boolean.parseBoolean(properties.get("sonar.java.compileToByteCode")),
        scope == null ? 1 : scope.modules().size() + 1));
      boolean cleaned;
      try (var paths = Files.walk(project)) {
        cleaned = paths.noneMatch(path -> Files.isDirectory(path) && path.getFileName().toString().equals("java-bytecode"));
      }
      telemetry.put("comparison.bytecode.cleaned", Boolean.toString(cleaned));
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
        && reference.semantics().totals().equals(run.semantics().totals())
        && reference.semantics().moduleIdentifierCounts().equals(run.semantics().moduleIdentifierCounts())
        && reference.semantics().hasFileObservations() == run.semantics().hasFileObservations()
        && reference.semantics().files().equals(run.semantics().files())
        && reference.semantics().unknownIdentifiers().equals(run.semantics().unknownIdentifiers())
        && List.of("comparison.compilation.status", "comparison.compilation.classes", "comparison.compilation.sources", "comparison.bytecode.cleaned")
          .stream().allMatch(key -> java.util.Objects.equals(reference.telemetry().get(key), run.telemetry().get(key)));
    });
  }

  private static boolean stableGraph(List<SourceOnlyComparison.Run> samples) {
    var reference = samples.getFirst().semantics();
    return reference != null && samples.stream().allMatch(run -> run.semantics() != null
      && java.util.Objects.equals(reference.graphTotals(), run.semantics().graphTotals())
      && java.util.Objects.equals(reference.graphTraversal(), run.semantics().graphTraversal())
      && reference.unknownSymbols().equals(run.semantics().unknownSymbols()) && reference.unknownTypes().equals(run.semantics().unknownTypes()));
  }

  private record FixtureScan(AnalysisMode mode, AnalysisFixtures.Fixture fixture, SourceOnlyComparison.Run run) {
  }

  private List<FixtureScan> scanFixtures() throws IOException {
    var scans = new ArrayList<FixtureScan>();
    for (AnalysisMode mode : AnalysisMode.values()) {
      var fixtures = List.of(
        AnalysisFixtures.clean(workspace.resolve("clean-" + mode.id()), mode != AnalysisMode.BASELINE, mode.compilation()),
        AnalysisFixtures.encoding(workspace.resolve("encoding-" + mode.id()), mode != AnalysisMode.BASELINE, mode.compilation()),
        AnalysisFixtures.dependency(workspace.resolve("dependency-" + mode.id()), mode.sourcePaths()),
        AnalysisFixtures.fallback(workspace.resolve("fallback-" + mode.id())));
      for (AnalysisFixtures.Fixture fixture : fixtures) {
        var properties = new TreeMap<>(mode.properties(fixture.sourcePaths(), settings.candidateProperties()));
        properties.putAll(fixture.properties());
        String label = fixture.project().getFileName().toString();
        scans.add(new FixtureScan(mode, fixture, scan(fixture.project(), fixture.sourceRoot(), label, properties,
          fixture.expectedFiles(), fixture.profile())));
      }
    }
    return scans;
  }

  private static List<AnalysisMatrix.Scenario> fixtureScenarios(List<FixtureScan> fixtures) {
    var grouped = new LinkedHashMap<String, Map<AnalysisMode, SourceOnlyComparison.Run>>();
    var descriptions = new LinkedHashMap<String, String>();
    for (FixtureScan fixture : fixtures) {
      grouped.computeIfAbsent(fixture.fixture().name(), name -> new EnumMap<>(AnalysisMode.class)).put(fixture.mode(), fixture.run());
      descriptions.put(fixture.fixture().name(), fixture.fixture().description());
    }
    return grouped.entrySet().stream().map(entry -> new AnalysisMatrix.Scenario(entry.getKey(), descriptions.get(entry.getKey()), entry.getValue())).toList();
  }

  private static void assertFixture(FixtureScan scan) {
    var fixture = scan.fixture();
    var run = scan.run();
    assertBindingProbe(run, fixture.expectedProbes());
    assertCompilationMeasurement(run, scan.mode());
    if (scan.mode().compilation()) {
      assertThat(run.telemetry()).as("%s compilation outcome", run.label()).containsEntry("comparison.compilation.status",
        fixture.expectCompilerSuccess() ? "SUCCESS" : "FAILED");
      if (fixture.expectCompilerSuccess()) {
        assertThat(Long.parseLong(run.telemetry().get("comparison.compilation.classes"))).as("Clean fixture must produce all three classes").isGreaterThanOrEqualTo(3);
      }
    }
    if (fixture.profile().equals("semantic-bindings-fallback")) {
      assertThat(run.findings()).as("Syntax checks must still execute after compiler errors").anyMatch(finding -> finding.rule().equals("java:S1116"));
    }
    if (fixture.name().equals("Clean compilation") || fixture.name().equals("Encoding compatibility")) {
      var deprecated = run.findings().stream().filter(finding -> finding.rule().equals("java:S1874")).toList();
      if (scan.mode() == AnalysisMode.BASELINE) {
        assertThat(deprecated).as("The baseline cannot identify the cross-file deprecated method").isEmpty();
      } else {
        assertThat(deprecated).as("Only the deprecated String overload should report, not the int overload or ordinary method")
          .singleElement().satisfies(finding -> assertThat(finding.path()).isEqualTo("src/main/java/bindings/BindingFixture.java"));
        assertThat(deprecated.getFirst().line()).isEqualTo(8);
      }
      var syntax = run.findings().stream().filter(finding -> finding.rule().equals("java:S1116")).toList();
      assertThat(syntax).as("Syntax control must report identically in every mode").singleElement().satisfies(finding -> {
        assertThat(finding.path()).isEqualTo("src/main/java/bindings/BindingFixture.java");
        assertThat(finding.line()).isEqualTo(11);
      });
    }
  }

  private static void assertCompilationMeasurement(SourceOnlyComparison.Run run, AnalysisMode mode) {
    assertThat(run.telemetry().get("comparison.compilation.status")).as("%s must expose compilation outcome", run.label())
      .isNotNull().isNotEqualTo("UNAVAILABLE");
    if (!mode.compilation()) {
      assertThat(run.telemetry()).containsEntry("comparison.compilation.status", "DISABLED").containsEntry("comparison.compilation.classes", "0");
    }
    assertThat(run.telemetry()).as("Generated bytecode must be removed after analysis").containsEntry("comparison.bytecode.cleaned", "true");
  }

  private static String bindingSummary(SourceOnlyComparison.Run run) {
    if (!run.success()) {
      return "unavailable: " + run.error();
    }
    return run.findings().stream().map(SourceOnlyComparison.Finding::message)
      .filter(message -> message.startsWith("Semantic binding "))
      .sorted().collect(java.util.stream.Collectors.joining("; "));
  }

  private static void assertBindingProbe(SourceOnlyComparison.Run run, int expectedProbes) {
    assertThat(run.success()).as("%s binding scan: %s", run.label(), run.error()).isTrue();
    var completions = run.findings().stream().map(SourceOnlyComparison.Finding::message)
      .filter(message -> BINDING_COMPLETION.matcher(message).matches()).toList();
    assertThat(completions).as("Binding probe must execute and emit exactly one completion").hasSize(1);
    var completion = BINDING_COMPLETION.matcher(completions.getFirst());
    assertThat(completion.matches()).isTrue();
    assertThat(Integer.parseInt(completion.group(1))).as("All expected binding probes must execute").isEqualTo(expectedProbes);
    assertThat(Integer.parseInt(completion.group(2))).as("Exact semantic bindings must match the golden fixture: %s", bindingSummary(run)).isZero();
  }

  private static Map<String, String> runMetadata(Path checkout, String snapshot) throws IOException {
    var metadata = new TreeMap<String, String>();
    metadata.put("Recorded at (UTC)", Instant.now().toString());
    metadata.put("Test harness revision", git(repositoryRoot(), "rev-parse", "HEAD"));
    metadata.put("Test harness dirty", Boolean.toString(!git(repositoryRoot(), "status", "--porcelain").isBlank()));
    String analyzerCheckout = System.getProperty("comparison.analyzerCheckout");
    Path provenance = pluginJar.getParent().resolve("comparison-analyzer-checkout.txt");
    if (analyzerCheckout == null && Files.isRegularFile(provenance)) {
      analyzerCheckout = Files.readString(provenance).strip();
    }
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
    metadata.put("Parser scenario selection", settings.fileByFile() == null ? "Normal batches and file-by-file" : "sonar.java.fileByFile=" + settings.fileByFile());
    metadata.put("Extra properties shared by all modes", new TreeMap<>(settings.candidateProperties()).toString());
    metadata.put("Features", "PR #6308 source paths and PR #6309 internal compilation; same plugin used for all four modes");
    metadata.put("PR #6308 revision", git(repositoryRoot(), "rev-parse", "origin/ac/hackathon"));
    metadata.put("PR #6309 revision", git(repositoryRoot(), "rev-parse", "origin/db/hackathon/optional-compilation"));
    metadata.put("Semantic reporter revision", git(repositoryRoot(), "rev-parse", "origin/alban/SemanticReport"));
    metadata.put("Compilation measurements", "Evaluation-only structured log hook records outcome, source/class counts, and elapsed time before cleanup");
    metadata.put("Per-file measurements", "Additive module.files observations preserve latest canonical identifier and graph fields");
    metadata.put("Graph observation budget", Integer.toString(settings.graphLimit()));
    metadata.put("Graph timing mode", settings.graphLimit() == 0 ? "Recursive diagnostics disabled; full AST identifiers, findings, and per-file observations remain enabled"
      : "Optional bounded diagnostic traversal enabled; partial results labelled and included in instrumented timings");
    metadata.put("Excluded observer run", "Unbounded refreshed graph traversal stalled on the two-file smoke scan and was stopped; excluded from all recorded comparison timings");
    for (String patch : List.of("compilation-measurements.patch", "semantic-file-measurements.patch", "semantic-graph-bounds.patch")) {
      metadata.put(patch + " SHA-256", HexFormat.of().formatHex(sha256().digest(Files.readAllBytes(repositoryRoot().resolve("its/plugin/tests/src/test/resources").resolve(patch)))));
    }
    metadata.put("Measured repetitions per mode", Integer.toString(settings.repetitions()));
    metadata.put("Timing protocol", settings.repetitions() > 1 ? "one warm-up per mode excluded; measured order rotates and reverses across repetitions" : "one measured scan per mode; no warm-up");
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

  static SourceOnlyComparison.Run failed(String label, long scanMillis, String error) {
    String diagnostic = error.replaceAll("(?i)(sonar\\.(?:token|login|password)=)\\S+", "$1[redacted]");
    return new SourceOnlyComparison.Run(label, false, scanMillis, List.of(), List.of(), Map.of(), null, diagnostic);
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
