/*
 * SonarQube Java
 * Copyright (C) 2013-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.commons.log.ClientLogOutput;
import org.sonarsource.sonarlint.core.commons.progress.ClientProgressMonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static StandaloneSonarLintEngine sonarlintEngine;
  private static File baseDir;

  @BeforeClass
  public static void prepare() throws Exception {
    StandaloneGlobalConfiguration config = StandaloneGlobalConfiguration.builder()
      .addPlugin(JavaTestSuite.JAVA_PLUGIN_LOCATION.getFile().toPath())
      .setSonarLintUserHome(temp.newFolder().toPath())
      .setLogOutput((formattedMessage, level) -> { /* Don't pollute logs*/ })
      .addEnabledLanguage(Language.JAVA)
      .build();
    sonarlintEngine = new StandaloneSonarLintEngineImpl(config);
    baseDir = temp.newFolder();
  }

  @Test
  public void simpleJava() throws Exception {
    ClientInputFile inputFile = prepareInputFile("Foo.java",
      "public class Foo {\n"
        + "  public void foo() {\n"
        + "    int x;\n"
        + "    System.out.println(\"Foo\");\n"
        + "    System.out.println(\"Foo\"); //NOSONAR\n"
        + "  }\n"
        + "}",
      false);

    final List<Issue> issues = new ArrayList<>();
    StandaloneAnalysisConfiguration standaloneAnalysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .build();
    sonarlintEngine.analyze(standaloneAnalysisConfiguration, issues::add, null, null);

    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "severity").containsOnly(
      tuple("java:S106", 4, inputFile.getPath(), IssueSeverity.MAJOR),
      tuple("java:S1220", null, inputFile.getPath(), IssueSeverity.MINOR),
      tuple("java:S1481", 3, inputFile.getPath(), IssueSeverity.MINOR));
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
    StandaloneAnalysisConfiguration standaloneAnalysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .build();
    sonarlintEngine.analyze(standaloneAnalysisConfiguration, issues::add, null, null);

    // Issues reported by S1607 are no longer expected here as the check requires complete semantic to run properly.
    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "severity").containsOnly(
      // tuple("squid:S2970", 6, inputFile.getPath(), "BLOCKER"),
      tuple("java:S2925", 7, inputFile.getPath(), IssueSeverity.MAJOR));
  }

  @Test
  public void supportJavaSuppressWarning() throws Exception {
    ClientInputFile inputFile = prepareInputFile("Foo.java",
      "public class Foo {\n"
        + "  @SuppressWarnings(\"java:S106\")\n"
        + "  public void foo() {\n"
        + "    int x;\n"
        + "    System.out.println(\"Foo\");\n"
        + "    System.out.println(\"Foo\"); //NOSONAR\n"
        + "  }\n"
        + "}",
      false);

    final List<Issue> issues = new ArrayList<>();
    StandaloneAnalysisConfiguration standaloneAnalysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .build();
    sonarlintEngine.analyze(standaloneAnalysisConfiguration, issues::add, null, null);

    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "severity").containsOnly(
      tuple("java:S1220", null, inputFile.getPath(), IssueSeverity.MINOR),
      tuple("java:S1481", 4, inputFile.getPath(), IssueSeverity.MINOR));
  }

  @Test
  public void parse_error_should_report_analysis_error() throws Exception {
    ClientInputFile inputFile = prepareInputFile("ParseError.java", "class ParseError {", false);
    final List<Issue> issues = new ArrayList<>();
    StandaloneAnalysisConfiguration standaloneAnalysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .build();
    AnalysisResults analysisResults = sonarlintEngine.analyze(standaloneAnalysisConfiguration, issues::add, null, null);
    assertThat(issues).isEmpty();
    assertThat(analysisResults.failedAnalysisFiles()).hasSize(1);
  }

  @Test
  public void sonarlint_cancelled_analysis_logs_but_does_not_rethrow_exception() throws Exception {
    List<ClientLogOutput.Level> logLevels = new ArrayList<>();
    List<String> errorLogs = new ArrayList<>();
    StandaloneGlobalConfiguration config = StandaloneGlobalConfiguration.builder()
      .addPlugin(JavaTestSuite.JAVA_PLUGIN_LOCATION.getFile().toPath())
      .setSonarLintUserHome(temp.newFolder().toPath())
      .setLogOutput((formattedMessage, level) -> logLevels.add(level))
      .addEnabledLanguage(Language.JAVA)
      .build();
    StandaloneSonarLintEngine sonarlintEngine = new StandaloneSonarLintEngineImpl(config);

    ClientInputFile inputFile = prepareInputFile("Foo.java",
      "public class Foo {\n"
        + "  @SuppressWarnings(\"java:S106\")\n"
        + "  public void foo() {\n"
        + "    int x;\n"
        + "    System.out.println(\"Foo\");\n"
        + "    System.out.println(\"Foo\"); //NOSONAR\n"
        + "  }\n"
        + "}",
      false);

    StandaloneAnalysisConfiguration standaloneAnalysisConfiguration = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir.toPath())
      .addInputFile(inputFile)
      .build();
    final List<Issue> issues = new ArrayList<>();
    CancellableProgressMonitor progressMonitor = new CancellableProgressMonitor();
    sonarlintEngine.analyze(
      standaloneAnalysisConfiguration,
      issue -> {
        if (!issues.isEmpty()) {
          progressMonitor.isCanceled = true;
          throw new MyCancelException();
        }
        issues.add(issue);
      },
      (formattedMessage, level) -> {
        if (level == ClientLogOutput.Level.ERROR) {
          errorLogs.add(formattedMessage);
        }
      },
      progressMonitor
    );

    // Check that there were no error logs prior to the analysis, as the log levels are not collected DURING the analysis
    assertThat(logLevels).doesNotContain(ClientLogOutput.Level.ERROR);
    assertThat(errorLogs)
      .hasSize(2)
      .allMatch(msg -> msg.equals("Error executing sensor: 'JavaSensor'") ||
        msg.startsWith("org.sonar.java.AnalysisException: Analysis cancelled")
      );
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

  @AfterClass
  public static void stop() {
    sonarlintEngine.stop();
  }

  static class MyCancelException extends RuntimeException {
  }

  static class CancellableProgressMonitor implements ClientProgressMonitor {
    boolean isCanceled = false;
    String message;
    float fraction;

    boolean intermediate;

    @Override
    public boolean isCanceled() {
      return isCanceled;
    }

    @Override
    public void setMessage(String msg) {
      message = msg;
    }

    @Override
    public void setFraction(float fraction) {
      this.fraction = fraction;
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
      intermediate = intermediate;
    }
  }

}
