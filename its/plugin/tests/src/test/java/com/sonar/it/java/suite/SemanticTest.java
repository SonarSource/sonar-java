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

import com.google.common.base.Throwables;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javaslang.control.Try;

public class SemanticTest {

  private static final int LOGS_NUMBER_LINES = 200;
  private static final Logger LOG = LoggerFactory.getLogger(SemanticTest.class);

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .setOrchestratorProperty("litsVersion", "0.6")
      .addPlugin("lits")
      .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../../sonar-java-plugin/target"), "sonar-java-plugin-*.jar"))
      .addPlugin(FileLocation.of(TestUtils.pluginJar("java-debugging-plugin")))
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/semantic/profile-java-semantic.xml"));
    ORCHESTRATOR = orchestratorBuilder.build();
  }

  @ClassRule
  public static TemporaryFolder TMP_DUMP_OLD_FOLDER = new TemporaryFolder();
  private static Path effectiveDumpOldFolder;

  private static final Path RESULTS_FOLDER = Paths.get("src/test/resources/semantic/");

  /**
   * Contains the key of the rules from debugging plugin used for semantic tests
   */
  private static final List<String> RULE_KEYS = Arrays.asList(
    "UnknownConstructorCall",
    "UnknownMethodInvocations");

  @BeforeClass
  public static void prepare_analysis() {
    effectiveDumpOldFolder = TMP_DUMP_OLD_FOLDER.getRoot().toPath().toAbsolutePath();
    Try.of(() -> Files.list(RESULTS_FOLDER)).getOrElseThrow(Throwables::propagate)
      .filter(p -> p.toFile().isDirectory())
      .forEach(srcProjectDir -> copyDumpSubset(srcProjectDir, effectiveDumpOldFolder.resolve(srcProjectDir.getFileName())));
  }

  private static void copyDumpSubset(Path srcProjectDir, Path dstProjectDir) {
    Try.of(() -> Files.createDirectory(dstProjectDir)).getOrElseThrow(Throwables::propagate);

    RULE_KEYS.stream()
      .map(ruleKey -> srcProjectDir.resolve("debug-" + ruleKey + ".json"))
      .filter(p -> p.toFile().exists())
      .forEach(srcJsonFile -> Try.of(() -> Files.copy(srcJsonFile, dstProjectDir.resolve(srcJsonFile.getFileName()), StandardCopyOption.REPLACE_EXISTING))
        .getOrElseThrow(Throwables::propagate));
  }

  @Test
  public void guava() throws Exception {
    test_project("com.google.guava:guava", "guava");
  }

  @Test
  public void apache_commons_beanutils() throws Exception {
    test_project("commons-beanutils:commons-beanutils", "commons-beanutils");
  }

  @Test
  public void fluent_http() throws Exception {
    test_project("net.code-story:http", "fluent-http");
  }

  @Test
  public void java_squid() throws Exception {
    // sonar-java/java-squid (v3.6)
    test_project("org.sonarsource.java:java-squid", "java-squid");
  }

  @Test
  public void sonarqube_server() throws Exception {
    // sonarqube/server/sonar-server (v.5.1.2)
    test_project("org.codehaus.sonar:sonar-server", "sonarqube/server", "sonar-server");
  }

  private static void test_project(String projectKey, String projectName) throws IOException {
    test_project(projectKey, null, projectName);
  }

  private static void test_project(String projectKey, @Nullable String path, String projectName) throws IOException {
    prepareProject(projectKey, projectName);
    String pomLocation = "../../sources/" + (path != null ? path + "/" : "") + projectName + "/pom.xml";
    MavenBuild mavenBuild = MavenBuild.create().setPom(FileLocation.of(pomLocation).getFile()).setCleanPackageSonarGoals().addArgument("-DskipTests");
    executeBuildWithCommonProperties(mavenBuild, projectName);
  }

  private static void prepareProject(String projectKey, String projectName) {
    ORCHESTRATOR.getServer().provisionProject(projectKey, projectName);
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(projectKey, "java", "java-semantic");
  }

  private static void executeBuildWithCommonProperties(Build<?> build, String projectName) throws IOException {
    build.setProperty("sonar.cpd.skip", "true")
      .setProperty("sonar.skipPackageDesign", "true")
      .setProperty("sonar.analysis.mode", "preview")
      .setProperty("sonar.issuesReport.html.enable", "true")
      .setProperty("sonar.issuesReport.html.location", htmlReportPath(projectName))
      .setProperty("dump.old", effectiveDumpOldFolder.resolve(projectName).toString())
      .setProperty("dump.new", FileLocation.of("target/actual/" + projectName).getFile().getAbsolutePath())
      .setProperty("lits.differences", litsDifferencesPath(projectName));
    BuildResult buildResult = ORCHESTRATOR.executeBuild(build);
    if (buildResult.isSuccess()) {
      assertNoDifferences(projectName);
    } else {
      dumpServerLogs();
    }
  }

  private static void dumpServerLogs() throws IOException {
    Server server = ORCHESTRATOR.getServer();
    LOG.error("::::::::::::::::::::::::::::::::::: DUMPING SERVER LOGS :::::::::::::::::::::::::::::::::::");
    dumpServerLogLastLines(server.getAppLogs());
    dumpServerLogLastLines(server.getCeLogs());
    dumpServerLogLastLines(server.getEsLogs());
    dumpServerLogLastLines(server.getWebLogs());
  }

  private static void dumpServerLogLastLines(File logFile) throws IOException {
    List<String> logs = Files.readAllLines(logFile.toPath());
    int nbLines = logs.size();
    if (nbLines > LOGS_NUMBER_LINES) {
      logs = logs.subList(nbLines - LOGS_NUMBER_LINES, nbLines);
    }
    LOG.error("=================================== START " + logFile.getName() + " ===================================");
    LOG.error(System.lineSeparator() + logs.stream().collect(Collectors.joining(System.lineSeparator())));
    LOG.error("===================================== END " + logFile.getName() + " ===================================");
  }

  private static String litsDifferencesPath(String projectName) {
    return FileLocation.of("target/" + projectName + "_differences").getFile().getAbsolutePath();
  }

  private static String htmlReportPath(String projectName) {
    return FileLocation.of("target/" + projectName + "_issue-report").getFile().getAbsolutePath();
  }

  private static void assertNoDifferences(String projectName) throws IOException {
    String differences = new String(Files.readAllBytes(Paths.get(litsDifferencesPath(projectName))), StandardCharsets.UTF_8);
    Assertions.assertThat(differences).overridingErrorMessage(differences + " -> file://" + htmlReportPath(projectName) + "/issues-report.html").isEmpty();
  }

}
