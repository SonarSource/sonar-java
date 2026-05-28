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
package org.sonar.java.checks.sit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sonarsource.scanner.integrationtester.dsl.ActiveRule;
import com.sonarsource.scanner.integrationtester.dsl.EngineVersion;
import com.sonarsource.scanner.integrationtester.dsl.RuleKey;
import com.sonarsource.scanner.integrationtester.dsl.ScannerInput;
import com.sonarsource.scanner.integrationtester.dsl.Log;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.test.classpath.TestClasspathUtils;

public abstract class ScannerIntegrationAbstractTest {

  private static final Path JAVA_PLUGIN_PATH = TestClasspathUtils.findModuleJarPath("../sonar-java-plugin");
  private static final String RULES_METADATA_DIR = "sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java";
  private static final Path METADATA_ROOT = resolveMetadataRoot();

  private static final ScannerRunnerConfig RUNNER_CONFIG = ScannerRunnerConfig.builder()
    .withLogsPrintedToStdOut(false)
    .build();

  @TempDir
  protected Path projectBaseDir;

  protected static Module module(String name) {
    return new Module(name);
  }

  protected static Path[] defaultClasspath() {
    return TestClasspathUtils.DEFAULT_MODULE.getClassPath().stream()
      .map(File::toPath)
      .toArray(Path[]::new);
  }

  protected List<FileIssue> analyze(List<String> ruleKeys, Module... modules) {
    Objects.requireNonNull(ruleKeys, "ruleKeys must not be null");
    if (modules == null || modules.length == 0) {
      throw new IllegalArgumentException("At least one module must be provided");
    }

    var modulesByName = indexModules(modules);
    var activeRules = ruleKeys.stream().map(ScannerIntegrationAbstractTest::buildActiveRule).toList();
    var scannerProperties = buildScannerProperties(modulesByName, projectBaseDir);

    createModuleDirectories(modulesByName);

    var serverContext = SonarServerContext.builder()
      .withProduct(SonarServerContext.Product.SERVER)
      .withEngineVersion(EngineVersion.latestRelease())
      .withLanguage("java", "Java", ".java")
      .withPlugin(JAVA_PLUGIN_PATH)
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

  private static Map<String, Module> indexModules(Module[] modules) {
    var map = new LinkedHashMap<String, Module>();
    for (Module module : modules) {
      if (map.containsKey(module.name)) {
        throw new IllegalArgumentException("Duplicate module name: '" + module.name + "'");
      }
      map.put(module.name, module);
    }
    // Validate dependencies
    for (Module module : modules) {
      for (String dep : module.dependencies) {
        if (!map.containsKey(dep)) {
          throw new IllegalStateException(
            "Module '" + module.name + "' depends on unknown module '" + dep + "'");
        }
      }
    }
    return map;
  }

  private static Map<String, String> buildScannerProperties(Map<String, Module> modulesByName, Path baseDir) {
    var properties = new LinkedHashMap<String, String>();
    properties.put("sonar.modules", String.join(",", modulesByName.keySet()));

    for (Module module : modulesByName.values()) {
      String prefix = module.name + ".";

      if (!module.inputFiles.isEmpty()) {
        properties.put(prefix + "sonar.sources", "src");
      }

      if (!module.binaries.isEmpty()) {
        properties.put(prefix + "sonar.java.binaries",
          module.binaries.stream().map(Path::toString).collect(Collectors.joining(",")));
      } else {
        Path emptyBinDir = baseDir.resolve(module.name + "-binaries");
        try {
          Files.createDirectories(emptyBinDir);
        } catch (IOException e) {
          throw new UncheckedIOException("Failed to create empty binaries directory for module: " + module.name, e);
        }
        properties.put(prefix + "sonar.java.binaries", emptyBinDir.toString());
      }

      List<String> allLibraries = new ArrayList<>();
      module.libraries.stream().map(Path::toString).forEach(allLibraries::add);
      for (String dep : module.dependencies) {
        modulesByName.get(dep).binaries.stream().map(Path::toString).forEach(allLibraries::add);
      }
      if (!allLibraries.isEmpty()) {
        properties.put(prefix + "sonar.java.libraries", String.join(",", allLibraries));
      }
    }

    return properties;
  }

  private void createModuleDirectories(Map<String, Module> modulesByName) {
    for (Module module : modulesByName.values()) {
      Path srcDir = projectBaseDir.resolve(module.name).resolve("src");
      try {
        Files.createDirectories(srcDir);
        for (Path inputFile : module.inputFiles) {
          Files.copy(inputFile, srcDir.resolve(inputFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to set up module directory: " + module.name, e);
      }
    }
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

  public static final class Module {
    private final String name;
    private final List<Path> inputFiles = new ArrayList<>();
    private final List<Path> binaries = new ArrayList<>();
    private final List<Path> libraries = new ArrayList<>();
    private final List<String> dependencies = new ArrayList<>();

    private Module(String name) {
      Objects.requireNonNull(name, "Module name must not be null");
      this.name = name;
    }

    public Module withInputFiles(Path... files) {
      Collections.addAll(inputFiles, files);
      return this;
    }

    public Module withBinaries(Path... paths) {
      Collections.addAll(binaries, paths);
      return this;
    }

    public Module withLibraries(Path... libs) {
      Collections.addAll(libraries, libs);
      return this;
    }

    public Module withDependencies(String... moduleNames) {
      Collections.addAll(dependencies, moduleNames);
      return this;
    }
  }
}
