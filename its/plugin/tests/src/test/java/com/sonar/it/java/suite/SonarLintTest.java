/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonarsource.sonarlint.core.analysis.AnalysisEngine;
import org.sonarsource.sonarlint.core.analysis.api.ActiveRule;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisConfiguration;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisEngineConfiguration;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.analysis.api.ClientModuleFileSystem;
import org.sonarsource.sonarlint.core.analysis.api.ClientModuleInfo;
import org.sonarsource.sonarlint.core.analysis.api.Issue;
import org.sonarsource.sonarlint.core.analysis.command.AnalyzeCommand;
import org.sonarsource.sonarlint.core.analysis.command.RegisterModuleCommand;
import org.sonarsource.sonarlint.core.commons.api.SonarLanguage;
import org.sonarsource.sonarlint.core.commons.log.LogOutput;
import org.sonarsource.sonarlint.core.commons.log.SonarLintLogger;
import org.sonarsource.sonarlint.core.commons.progress.ProgressMonitor;
import org.sonarsource.sonarlint.core.plugin.commons.PluginsLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintTest {
  private static final LogOutput NOOP_LOG_OUTPUT = new LogOutput() {
    @Override
    public void log(@Nullable String formattedMessage, Level level, @Nullable String stacktrace) {
      /*Don't pollute logs*/
    }
  };

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static AnalysisEngine sonarlintEngine;
  private static File baseDir;

  private final ProgressMonitor progressMonitor = new ProgressMonitor(null);

  @BeforeClass
  public static void prepare() throws Exception {
    AnalysisEngineConfiguration config = AnalysisEngineConfiguration.builder()
      .setWorkDir(temp.getRoot().toPath())
      .build();

    SonarLintLogger.setTarget(NOOP_LOG_OUTPUT);
    var pluginJarLocation = Set.of(JavaTestSuite.JAVA_PLUGIN_LOCATION.getFile().toPath());
    var enabledLanguages = Set.of(SonarLanguage.JAVA);
    var pluginConfiguration = new PluginsLoader.Configuration(pluginJarLocation, enabledLanguages, false, Optional.empty());
    var loadedPlugins = new PluginsLoader().load(pluginConfiguration, Set.of()).getLoadedPlugins();

    sonarlintEngine = new AnalysisEngine(config, loadedPlugins, NOOP_LOG_OUTPUT);
    baseDir = temp.newFolder();
  }

  @AfterClass
  public static void stop() {
    SonarLintLogger.setTarget(null);
    sonarlintEngine.stop();
  }

  @Test
  public void simpleJava() throws Exception {
    ClientInputFile inputFile = prepareInputFile("Foo.java", """
        public class Foo {
          public void foo() {
            int x;
            System.out.println("Foo");
            System.out.println("Foo"); //NOSONAR
          }
        }
        """,
      false);

    final List<Issue> issues = new ArrayList<>();
    AnalysisConfiguration configuration = AnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .addActiveRules(
        new ActiveRule("java:S106", SonarLanguage.JAVA.name()),
        new ActiveRule("java:S1220", SonarLanguage.JAVA.name()),
        new ActiveRule("java:S1481", SonarLanguage.JAVA.name())
      ).build();

    ClientModuleFileSystem clientFileSystem = getClientModuleFileSystem(inputFile);
    sonarlintEngine.post(new RegisterModuleCommand(new ClientModuleInfo("myModule", clientFileSystem)), progressMonitor).get();
    var command = new AnalyzeCommand("myModule", configuration, issues::add, NOOP_LOG_OUTPUT);
    sonarlintEngine.post(command, progressMonitor).get();

    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "overriddenImpacts").containsExactlyInAnyOrder(
      tuple("java:S106", 4, inputFile.getPath(), Map.of()),
      tuple("java:S1220", null, inputFile.getPath(), Map.of()),
      tuple("java:S1481", 3, inputFile.getPath(), Map.of())
    );
  }

  @Test
  public void simpleTestFileJava() throws Exception {
    ClientInputFile inputFile = prepareInputFile("FooTest.java",
      "public class FooTest {\n"
        + "  @org.junit.Test\n"
        + "  @org.junit.Ignore\n"
        + "  public void testName() throws Exception {\n" // S1607(ignored test)
        + "    Foo foo = new Foo();\n"
        + "    org.assertj.core.api.Assertions.assertThat(foo.isFooActive());\n" // S2970(incomplete assertions) - requires semantic
        + "    java.lang.Thread.sleep(Long.MAX_VALUE);" // S2925(thread.sleep in test)
        + "  }\n\n"

        + "  private static class Foo {"
        + "    public boolean isFooActive() {"
        + "      return false;"
        + "    }"
        + "  }"
        + "}",
      true);

    final List<Issue> issues = new ArrayList<>();
    AnalysisConfiguration configuration = AnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .addActiveRules(
        new ActiveRule("java:S1220", SonarLanguage.JAVA.name()),
        new ActiveRule("java:S2925", SonarLanguage.JAVA.name())
      ).build();
    ClientModuleFileSystem clientFileSystem = getClientModuleFileSystem(inputFile);

    sonarlintEngine.post(new RegisterModuleCommand(new ClientModuleInfo("myModule", clientFileSystem)), progressMonitor).get();
    var command = new AnalyzeCommand("myModule", configuration, issues::add, NOOP_LOG_OUTPUT);
    sonarlintEngine.post(command, progressMonitor).get();

    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "overriddenImpacts").containsOnly(
      tuple("java:S2925", 7, inputFile.getPath(), Map.of()),
      // expected issue
      tuple("java:S1220", null, inputFile.getPath(), Map.of()));
  }

  @Test
  public void supportJavaSuppressWarning() throws Exception {
    ClientInputFile inputFile = prepareInputFile("Foo.java", """
        public class Foo {
          @SuppressWarnings("java:S106")
          public void foo() {
            int x;
            System.out.println("Foo");
            System.out.println("Foo"); //NOSONAR
          }
        }
        """,
      false);

    final List<Issue> issues = new ArrayList<>();
    AnalysisConfiguration configuration = AnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .addActiveRules(
        new ActiveRule("java:S1220", SonarLanguage.JAVA.name()),
        new ActiveRule("java:S1481", SonarLanguage.JAVA.name())
      )
      .build();


    ClientModuleFileSystem clientFileSystem = getClientModuleFileSystem(inputFile);

    sonarlintEngine.post(new RegisterModuleCommand(new ClientModuleInfo("myModule", clientFileSystem)), progressMonitor).get();
    var command = new AnalyzeCommand("myModule", configuration, issues::add, NOOP_LOG_OUTPUT);
    sonarlintEngine.post(command, progressMonitor).get();

    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "overriddenImpacts").containsOnly(
      tuple("java:S1220", null, inputFile.getPath(), Map.of()),
      tuple("java:S1481", 4, inputFile.getPath(), Map.of()));
  }

  @Test
  public void parse_error_should_report_analysis_error() throws Exception {
    ClientInputFile inputFile = prepareInputFile("ParseError.java", "class ParseError {", false);
    final List<Issue> issues = new ArrayList<>();
    AnalysisConfiguration configuration = AnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .build();

    ClientModuleFileSystem clientFileSystem = getClientModuleFileSystem(inputFile);

    sonarlintEngine.post(new RegisterModuleCommand(new ClientModuleInfo("myModule", clientFileSystem)), progressMonitor).get();
    var command = new AnalyzeCommand("myModule", configuration, issues::add, NOOP_LOG_OUTPUT);
    AnalysisResults analysisResults = sonarlintEngine.post(command, progressMonitor).get();

    assertThat(issues).isEmpty();
    assertThat(analysisResults.failedAnalysisFiles()).hasSize(1);
  }

  @Test
  public void sonarlint_cancelled_analysis_logs_but_does_not_rethrow_exception() throws Exception {
    List<LogOutput.Level> logLevels = new ArrayList<>();
    List<String> errorLogs = new ArrayList<>();

    AnalysisEngineConfiguration engineConfiguration = AnalysisEngineConfiguration.builder()
      .setWorkDir(temp.getRoot().toPath())
      .build();

    LogOutput levelCollector = new LogOutput() {
      @Override
      public void log(String formattedMessage, Level level, @Nullable String stacktrace) {
        logLevels.add(level);
      }
    };
    SonarLintLogger.setTarget(levelCollector);
    var pluginJarLocation = Set.of(JavaTestSuite.JAVA_PLUGIN_LOCATION.getFile().toPath());
    var enabledLanguages = Set.of(SonarLanguage.JAVA);
    var pluginConfiguration = new PluginsLoader.Configuration(pluginJarLocation, enabledLanguages, false, Optional.empty());
    var loadedPlugins = new PluginsLoader().load(pluginConfiguration, Set.of()).getLoadedPlugins();

    AnalysisEngine specificSonarlintEngine = new AnalysisEngine(engineConfiguration, loadedPlugins, NOOP_LOG_OUTPUT);

    ClientInputFile inputFile = prepareInputFile("Foo.java", """
        public class Foo {
          @SuppressWarnings("java:S106")
          public void foo() {
            int x;
            System.out.println("Foo");
            System.out.println("Foo"); //NOSONAR
          }
        }
        """,
      false);

    AnalysisConfiguration analysisConfiguration = AnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addActiveRules(
        new ActiveRule("java:S1220", SonarLanguage.JAVA.name()),
        new ActiveRule("java:S1481", SonarLanguage.JAVA.name())
      )
      .addInputFile(inputFile)
      .build();

    final List<Issue> issues = new ArrayList<>();

    CancellableProgressMonitor cancellableProgressMonitor = new CancellableProgressMonitor();

    ClientModuleFileSystem clientFileSystem = getClientModuleFileSystem(inputFile);

    specificSonarlintEngine.post(new RegisterModuleCommand(new ClientModuleInfo("myModule", clientFileSystem)), progressMonitor).get();
    Consumer<Issue> issueListener = issue -> {
      if (!issues.isEmpty()) {
        cancellableProgressMonitor.isCanceled = true;
        throw new MyCancelException();
      }
      issues.add(issue);
    };
    LogOutput errorCollector = new LogOutput() {
      @Override
      public void log(@Nullable String formattedMessage, Level level, @Nullable String stacktrace) {
        if (level == LogOutput.Level.ERROR) {
          errorLogs.add(formattedMessage);
        }
      }
    };
    var command = new AnalyzeCommand("myModule",
      analysisConfiguration,
      issueListener,
      errorCollector
    );
    specificSonarlintEngine.post(command, cancellableProgressMonitor).get();

    // Check that there were no error logs prior to the analysis, as the log levels are not collected DURING the analysis
    assertThat(logLevels).doesNotContain(LogOutput.Level.ERROR);
    assertThat(errorLogs)
      .containsOnly("Error executing sensor: 'JavaSensor'");
    assertThat(issues).hasSize(1);
  }

  private ClientInputFile prepareInputFile(String relativePath, String content, final boolean isTest) throws IOException {
    final File file = new File(baseDir, relativePath);
    FileUtils.write(file, content, StandardCharsets.UTF_8);
    return createInputFile(file.toPath(), isTest);
  }

  private ClientInputFile createInputFile(final Path path, final boolean isTest) {
    return new ClientInputFile() {

      @Override
      public String getPath() {
        return path.toString();
      }

      @Override
      public String relativePath() {
        return baseDir.toPath().relativize(path).toString();
      }

      @Override
      public URI uri() {
        return path.toUri();
      }

      @Override
      public boolean isTest() {
        return isTest;
      }

      @Override
      public Charset getCharset() {
        return StandardCharsets.UTF_8;
      }

      @Override
      public <G> G getClientObject() {
        return null;
      }

      @Override
      public InputStream inputStream() throws IOException {
        return new FileInputStream(path.toFile());
      }

      @Override
      public String contents() throws IOException {
        return Files.asCharSource(path.toFile(), StandardCharsets.UTF_8).read();
      }
    };
  }

  private static ClientModuleFileSystem getClientModuleFileSystem(ClientInputFile inputFile) {
    return new ClientModuleFileSystem() {
      @Override
      public Stream<ClientInputFile> files(String s, InputFile.Type type) {
        return Stream.of(inputFile);
      }

      @Override
      public Stream<ClientInputFile> files() {
        return Stream.of(inputFile);
      }
    };
  }

  static class MyCancelException extends RuntimeException {
  }

  static class CancellableProgressMonitor extends ProgressMonitor {
    boolean isCanceled = false;

    CancellableProgressMonitor() {
      super(null);
    }

    @Override
    public boolean isCanceled() {
      return isCanceled;
    }
  }
}
