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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.sonar.java.test.classpath.TestClasspathUtils;

@Execution(ExecutionMode.CONCURRENT)
public abstract class ScannerIntegrationAbstractTest {

  private static final Path JAVA_PLUGIN_PATH = TestClasspathUtils.findModuleJarPath("../../sonar-java-plugin");
  private static final String RULES_METADATA_DIR = "sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java";
  private static final Path METADATA_ROOT = resolveMetadataRoot();

  private static final ScannerRunnerConfig RUNNER_CONFIG = ScannerRunnerConfig.builder()
    .withLogsPrintedToStdOut(false)
    .build();

  @TempDir
  protected Path projectBaseDir;

  protected List<FileIssue> analyze(Path projectDir) {
    Path resourceDir = resolveResourceDir(projectDir);
    ProjectConfig config = loadProjectConfig(resourceDir);
    Map<String, List<Path>> moduleFiles = discoverModules(resourceDir);

    var activeRules = config.ruleKeys.stream()
      .map(ScannerIntegrationAbstractTest::buildActiveRule)
      .toList();

    copySourceFiles(moduleFiles);
    var scannerProperties = buildScannerProperties(config, moduleFiles);

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

  private static ProjectConfig loadProjectConfig(Path projectDir) {
    Path propsFile = projectDir.resolve("project.properties");
    if (!Files.exists(propsFile)) {
      throw new IllegalArgumentException("Missing project.properties in " + projectDir);
    }
    var props = new Properties();
    try (var reader = Files.newBufferedReader(propsFile)) {
      props.load(reader);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read project.properties", e);
    }

    String ruleKeysStr = props.getProperty("ruleKeys");
    if (ruleKeysStr == null || ruleKeysStr.isBlank()) {
      throw new IllegalArgumentException("'ruleKeys' is required in project.properties");
    }
    List<String> ruleKeys = Arrays.stream(ruleKeysStr.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toList();

    String globalLibraries = props.getProperty("libraries", "default");
    String javaVersion = props.getProperty("javaVersion");

    Map<String, List<String>> moduleDependencies = new LinkedHashMap<>();
    Map<String, String> moduleLibraries = new LinkedHashMap<>();
    Map<String, String> moduleBinaries = new LinkedHashMap<>();

    for (String key : props.stringPropertyNames()) {
      if (key.endsWith(".dependencies")) {
        String moduleName = key.substring(0, key.length() - ".dependencies".length());
        moduleDependencies.put(moduleName,
          Arrays.stream(props.getProperty(key).split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList());
      } else if (key.endsWith(".libraries")) {
        String moduleName = key.substring(0, key.length() - ".libraries".length());
        moduleLibraries.put(moduleName, props.getProperty(key).trim());
      } else if (key.endsWith(".binaries")) {
        String moduleName = key.substring(0, key.length() - ".binaries".length());
        moduleBinaries.put(moduleName, props.getProperty(key).trim());
      }
    }

    return new ProjectConfig(ruleKeys, globalLibraries, javaVersion,
      moduleDependencies, moduleLibraries, moduleBinaries);
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

  private void copySourceFiles(Map<String, List<Path>> moduleFiles) {
    for (var entry : moduleFiles.entrySet()) {
      Path srcDir = projectBaseDir.resolve(entry.getKey()).resolve("src");
      try {
        Files.createDirectories(srcDir);
        for (Path javaFile : entry.getValue()) {
          Files.copy(javaFile, srcDir.resolve(javaFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to set up module directory: " + entry.getKey(), e);
      }
    }
  }

  private Map<String, String> buildScannerProperties(ProjectConfig config, Map<String, List<Path>> moduleFiles) {
    var properties = new LinkedHashMap<String, String>();
    properties.put("sonar.modules", String.join(",", moduleFiles.keySet()));

    if (config.javaVersion != null) {
      properties.put("sonar.java.source", config.javaVersion);
    }

    for (String moduleName : moduleFiles.keySet()) {
      String prefix = moduleName + ".";

      properties.put(prefix + "sonar.sources", "src");

      // Binaries
      String binariesOverride = config.moduleBinaries.get(moduleName);
      if (binariesOverride != null) {
        properties.put(prefix + "sonar.java.binaries", binariesOverride);
      } else {
        Path emptyBinDir = projectBaseDir.resolve(moduleName + "-binaries");
        try {
          Files.createDirectories(emptyBinDir);
        } catch (IOException e) {
          throw new UncheckedIOException("Failed to create empty binaries directory for module: " + moduleName, e);
        }
        properties.put(prefix + "sonar.java.binaries", emptyBinDir.toString());
      }

      // Libraries
      List<String> allLibraries = new ArrayList<>();
      String libSpec = config.moduleLibraries.getOrDefault(moduleName, config.globalLibraries);
      if ("default".equals(libSpec)) {
        for (Path p : defaultClasspath()) {
          allLibraries.add(p.toString());
        }
      } else if (libSpec != null && !libSpec.isBlank()) {
        Arrays.stream(libSpec.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .forEach(allLibraries::add);
      }

      // Dependencies: add dependent module's binaries to this module's libraries
      for (String dep : config.moduleDependencies.getOrDefault(moduleName, List.of())) {
        String depBinaries = properties.get(dep + ".sonar.java.binaries");
        if (depBinaries != null) {
          allLibraries.add(depBinaries);
        }
      }

      if (!allLibraries.isEmpty()) {
        properties.put(prefix + "sonar.java.libraries", String.join(",", allLibraries));
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

  private record ProjectConfig(
    List<String> ruleKeys,
    String globalLibraries,
    String javaVersion,
    Map<String, List<String>> moduleDependencies,
    Map<String, String> moduleLibraries,
    Map<String, String> moduleBinaries
  ) {}
}
