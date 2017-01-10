/*
 * SonarQube Java
 * Copyright (C) 2013-2017 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
      .addPlugin(JavaTestSuite.JAVA_PLUGIN_LOCATION.getFile().toURI().toURL())
      .setSonarLintUserHome(temp.newFolder().toPath())
      .setLogOutput((formattedMessage, level) -> { /* Don't pollute logs*/ })
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
    sonarlintEngine.analyze(new StandaloneAnalysisConfiguration(baseDir.toPath(), temp.newFolder().toPath(), Collections.singletonList(inputFile), ImmutableMap.<String, String>of()), issues::add);

    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "severity").containsOnly(
      tuple("squid:S106", 4, inputFile.getPath(), "MAJOR"),
      tuple("squid:S1220", null, inputFile.getPath(), "MINOR"),
      tuple("squid:S1481", 3, inputFile.getPath(), "MINOR"));
  }

  @Test
  public void simplePom() throws Exception {
    ClientInputFile inputFile = prepareInputFile("pom.xml",
      "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
        + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
        + "  <modelVersion>4.0.0</modelVersion>\n"
        + "  <groupId>org.sonarsource.java</groupId>\n"
        + "  <artifactId>simple-project</artifactId>\n"
        + "  <version>1.0-SNAPSHOT</version>\n"
        + "  <packaging>jar</packaging>\n"
        + "  <properties>"
        + "    <deprecated>${pom.artifactId}</deprecated>\n" // S3421 line 7
        + "  </properties>\n"
        + "</project>",
      false);

    final List<Issue> issues = new ArrayList<>();
    sonarlintEngine.analyze(new StandaloneAnalysisConfiguration(baseDir.toPath(), temp.newFolder().toPath(), Collections.singletonList(inputFile), ImmutableMap.<String, String>of()), issues::add);

    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "severity").containsOnly(
      tuple("squid:S3421", 7, inputFile.getPath(), "MINOR"));
  }

  @Test
  public void supportJavaSuppressWarning() throws Exception {
    ClientInputFile inputFile = prepareInputFile("Foo.java",
      "public class Foo {\n"
        + "  @SuppressWarnings(\"squid:S106\")\n"
        + "  public void foo() {\n"
        + "    int x;\n"
        + "    System.out.println(\"Foo\");\n"
        + "    System.out.println(\"Foo\"); //NOSONAR\n"
        + "  }\n"
        + "}",
      false);

    final List<Issue> issues = new ArrayList<>();
    sonarlintEngine.analyze(
      new StandaloneAnalysisConfiguration(baseDir.toPath(), temp.newFolder().toPath(), Collections.singletonList(inputFile), ImmutableMap.<String, String>of()), issues::add);

    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "severity").containsOnly(
      tuple("squid:S1220", null, inputFile.getPath(), "MINOR"),
      tuple("squid:S1481", 4, inputFile.getPath(), "MINOR"));
  }

  @Test
  public void parse_error_should_report_analysis_error() throws Exception {
    ClientInputFile inputFile = prepareInputFile("ParseError.java", "class ParseError {", false);
    final List<Issue> issues = new ArrayList<>();
    AnalysisResults analysisResults = sonarlintEngine.analyze(
      new StandaloneAnalysisConfiguration(baseDir.toPath(), temp.newFolder().toPath(), Collections.singletonList(inputFile), ImmutableMap.<String, String>of()), issues::add);
    assertThat(issues).isEmpty();
    assertThat(analysisResults.failedAnalysisFiles()).hasSize(1);
  }

  private ClientInputFile prepareInputFile(String relativePath, String content, final boolean isTest) throws IOException {
    final File file = new File(baseDir, relativePath);
    FileUtils.write(file, content, StandardCharsets.UTF_8);
    return createInputFile(file.toPath(), isTest);
  }

  private ClientInputFile createInputFile(final Path path, final boolean isTest) {
    return new ClientInputFile() {

      @Override
      public Path getPath() {
        return path;
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
    };
  }

  @AfterClass
  public static void stop() {
    sonarlintEngine.stop();
  }

}
