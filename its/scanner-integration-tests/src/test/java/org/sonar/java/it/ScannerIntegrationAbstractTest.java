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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonarsource.scanner.integrationtester.dsl.ActiveRule;
import com.sonarsource.scanner.integrationtester.dsl.EngineVersion;
import com.sonarsource.scanner.integrationtester.dsl.Log;
import com.sonarsource.scanner.integrationtester.dsl.RuleKey;
import com.sonarsource.scanner.integrationtester.dsl.ScannerInput;
import com.sonarsource.scanner.integrationtester.dsl.ScannerResult;
import com.sonarsource.scanner.integrationtester.dsl.ScannerResultSuccess;
import com.sonarsource.scanner.integrationtester.dsl.SonarProjectContext;
import com.sonarsource.scanner.integrationtester.dsl.SonarServerContext;
import com.sonarsource.scanner.integrationtester.dsl.issue.FileIssue;
import com.sonarsource.scanner.integrationtester.runner.ScannerRunner;
import com.sonarsource.scanner.integrationtester.runner.ScannerRunnerConfig;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.sonar.java.test.classpath.TestClasspathUtils;

@Execution(ExecutionMode.CONCURRENT)
public abstract class ScannerIntegrationAbstractTest {

  private static FileLocation javaPluginLocation;
  private static final String RULES_METADATA_DIR = "sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java";
  private static final Path METADATA_ROOT = resolveMetadataRoot();

  private static final ScannerRunnerConfig RUNNER_CONFIG = ScannerRunnerConfig.builder()
    .withLogsPrintedToStdOut(false)
    .build();

  @TempDir
  protected Path projectBaseDir;

  @BeforeAll
  static void beforeAll() {
    javaPluginLocation = FileLocation.of(TestClasspathUtils.findModuleJarPath("../../sonar-java-plugin").toFile());
  }

  protected List<FileIssue> analyze(Path projectDir, String... ruleKeys) {
    Path resourceDir = resolveResourceDir(projectDir);

    copyProjectTree(resourceDir);

    MavenBuildHelper mavenHelper = new MavenBuildHelper(projectBaseDir);
    mavenHelper.build();

    Map<String, List<Path>> moduleFiles = discoverModules(projectBaseDir);

    var activeRules = Arrays.stream(ruleKeys)
      .map(ScannerIntegrationAbstractTest::buildActiveRule)
      .toList();

    var scannerProperties = buildScannerProperties(moduleFiles, mavenHelper);

    var serverContext = SonarServerContext.builder()
      .withProduct(SonarServerContext.Product.SERVER)
      .withEngineVersion(EngineVersion.latestRelease())
      .withLanguage("java", "Java", ".java")
      .withPlugin(javaPluginLocation)
      .withProjectContext(SonarProjectContext.builder().withActiveRules(activeRules).build())
      .build();

    var scannerInput = ScannerInput.create("scanner-integration-test", projectBaseDir)
      .withScannerProperties(scannerProperties)
      .build();

    var result = ScannerRunner.run(serverContext, scannerInput, RUNNER_CONFIG);
    if (result.exitCode() != 0) {
      throw new AssertionError(buildFailureMessage(result, scannerProperties));
    }

    return ((ScannerResultSuccess) result).scannerOutputReader().getFiles().stream()
      .flatMap(file -> file.getIssues().stream())
      .toList();
  }

  private static Path resolveResourceDir(Path relativePath) {
    var resource = ScannerIntegrationAbstractTest.class.getClassLoader()
      .getResource(relativePath.toString());
    if (resource == null) {
      throw new IllegalArgumentException("Cannot find test resource directory: " + relativePath);
    }
    try {
      return Path.of(resource.toURI());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid resource URI", e);
    }
  }

  private static Map<String, List<Path>> discoverModules(Path projectDir) {
    var modules = new LinkedHashMap<String, List<Path>>();
    try (Stream<Path> dirs = Files.list(projectDir)) {
      dirs.filter(Files::isDirectory)
        .sorted()
        .forEach(moduleDir -> {
          String moduleName = moduleDir.getFileName().toString();
          try (Stream<Path> files = Files.walk(moduleDir)) {
            List<Path> javaFiles = files
              .filter(p -> p.toString().endsWith(".java"))
              .sorted()
              .toList();
            if (!javaFiles.isEmpty()) {
              modules.put(moduleName, javaFiles);
            }
          } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan module directory: " + moduleName, e);
          }
        });
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to list project directory: " + projectDir, e);
    }
    if (modules.isEmpty()) {
      throw new IllegalArgumentException(
        "No modules found (no subdirectories with .java files) in " + projectDir);
    }
    return modules;
  }

  private void copyProjectTree(Path resourceDir) {
    try (Stream<Path> walk = Files.walk(resourceDir)) {
      walk.forEach(source -> {
        Path target = projectBaseDir.resolve(resourceDir.relativize(source));
        try {
          if (Files.isDirectory(source)) {
            Files.createDirectories(target);
          } else {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
          }
        } catch (IOException e) {
          throw new UncheckedIOException("Failed to copy: " + source, e);
        }
      });
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to copy project tree", e);
    }
  }

  private Map<String, String> buildScannerProperties(Map<String, List<Path>> moduleFiles,
                                                     @javax.annotation.Nullable MavenBuildHelper mavenHelper) {
    var properties = new LinkedHashMap<String, String>();
    properties.put("sonar.modules", String.join(",", moduleFiles.keySet()));

    for (String moduleName : moduleFiles.keySet()) {
      String prefix = moduleName + ".";

      if (mavenHelper != null) {
        properties.put(prefix + "sonar.sources", MavenBuildHelper.SOURCES_DIR);
        properties.put(prefix + "sonar.java.binaries", MavenBuildHelper.BINARIES_DIR);
        properties.put(prefix + "sonar.java.libraries",
          mavenHelper.resolveLibraries(moduleName, moduleFiles.keySet()));
      } else {
        properties.put(prefix + "sonar.sources", ".");
        Path emptyBinDir = projectBaseDir.resolve(moduleName + "-binaries");
        try {
          Files.createDirectories(emptyBinDir);
        } catch (IOException e) {
          throw new UncheckedIOException("Failed to create empty binaries directory for module: " + moduleName, e);
        }
        properties.put(prefix + "sonar.java.binaries", emptyBinDir.toString());
        properties.put(prefix + "sonar.java.libraries",
          Arrays.stream(defaultClasspath()).map(Path::toString).collect(Collectors.joining(",")));
      }
    }

    return properties;
  }

  private static Path[] defaultClasspath() {
    return TestClasspathUtils.DEFAULT_MODULE.getClassPath().stream()
      .map(File::toPath)
      .toArray(Path[]::new);
  }

  private static String buildFailureMessage(ScannerResult result, Map<String, String> scannerProperties) {
    var sb = new StringBuilder();
    sb.append("Scanner execution failed with exit code ").append(result.exitCode()).append("\n\n");

    sb.append("--- Scanner properties ---\n");
    scannerProperties.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));

    List<Log> logs = result.logOutput();
    if (logs != null && !logs.isEmpty()) {
      sb.append("\n--- Scanner logs ---\n");
      for (Log log : logs) {
        sb.append("[").append(log.level()).append("] ").append(log.message()).append("\n");
        if (log.stacktrace() != null && !log.stacktrace().isEmpty()) {
          sb.append(log.stacktrace()).append("\n");
        }
      }
    }

    return sb.toString();
  }

  private static ActiveRule buildActiveRule(String ruleKey) {
    Path jsonFile = METADATA_ROOT.resolve(ruleKey + ".json");
    if (!Files.exists(jsonFile)) {
      throw new IllegalArgumentException("Rule metadata not found for key '" + ruleKey + "': " + jsonFile);
    }

    try {
      JsonObject json = JsonParser.parseString(Files.readString(jsonFile)).getAsJsonObject();
      String title = json.get("title").getAsString();
      String severityStr = json.get("defaultSeverity").getAsString();
      ActiveRule.Severity severity = ActiveRule.Severity.valueOf(severityStr.toUpperCase(Locale.ROOT));

      return ActiveRule.builder()
        .withKey(RuleKey.of("java", ruleKey))
        .withName(title)
        .withLanguageKey("java")
        .withSeverity(severity)
        .build();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read rule metadata: " + jsonFile, e);
    }
  }

  private static Path resolveMetadataRoot() {
    Path lookUpPath = Path.of(System.getProperty("user.dir"));
    while (lookUpPath != null) {
      Path candidate = lookUpPath.resolve(RULES_METADATA_DIR);
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      lookUpPath = lookUpPath.getParent();
    }
    throw new IllegalStateException(
      "Cannot find rule metadata directory '" + RULES_METADATA_DIR + "' from working directory");
  }

}
