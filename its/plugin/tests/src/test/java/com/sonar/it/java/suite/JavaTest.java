/*
 * Java :: IT :: Plugin :: Tests
 * Copyright (C) 2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.it.java.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.MavenLocator;
import com.sonar.orchestrator.version.Version;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class JavaTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  /**
   * See SONAR-1865
   */
  @Test
  public void shouldAcceptFilenamesWithDollar() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("dollar-in-names"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Resource file = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(JavaTestSuite.keyFor("org.sonar.it.core:dollar-in-names", "dollars/", "FilenameWith$Dollar.java"), "files"));
    assertThat(file).isNotNull();
    assertThat(file.getLongName()).contains("FilenameWith$Dollar");
  }

  /**
   * Since 2.13 commented-out code lines not saved as measure for Java - see SONAR-3093
   */
  @Test
  public void shouldDetectCommentedOutCode() {
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("commented-out-java-code"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:commented-out-java-code",
      "commented_out_code_lines", "ncloc"));
    assertThat(project.getMeasureIntValue("ncloc")).isEqualTo(7);
    assertThat(project.getMeasureIntValue("commented_out_code_lines")).isNull();
  }

  /**
   * SONARJAVA-444
   */
  @Test
  public void shouldFailIfInvalidJavaPackage() {
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("invalid-java-package"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");

    BuildResult buildResult = orchestrator.executeBuildQuietly(build);
    assertThat(buildResult.getStatus()).isEqualTo(0);
  }

  @Test
  public void measures_on_directory() {
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("measures-on-directory"))
      .setCleanPackageSonarGoals();
    BuildResult result = orchestrator.executeBuildQuietly(build);
    Version version = orchestrator.getConfiguration().getPluginVersion("java");
    if (version.isGreaterThan("2.1") && !version.isGreaterThanOrEquals("2.4")) {
      assertThat(result.getStatus()).overridingErrorMessage("build of project measures-on-directory should have failed and have a status different than 0")
        .isGreaterThan(0);
      assertThat(result.getLogs()).contains("Directory contains files belonging to different packages - some metrics could be reported incorrectly: "
        + new File(TestUtils.projectPom("measures-on-directory").getParentFile(), "src/main/java/org"));
    } else {
      // sonar-java 2.1 does not fail if multiple package in same directory.
      assertThat(result.getStatus()).isEqualTo(0);
    }
  }

  @Test
  public void multiple_package_in_directory_should_not_fail() throws Exception {
    MavenBuild inspection = MavenBuild.create()
      .setPom(TestUtils.projectPom("multiple-packages-in-directory"))
      .setCleanPackageSonarGoals();
    BuildResult result = orchestrator.executeBuildQuietly(inspection);
    assertThat(result.getStatus()).isEqualTo(0);
    inspection = MavenBuild.create()
      .setPom(TestUtils.projectPom("multiple-packages-in-directory"))
      .setProperty(CoreProperties.DESIGN_SKIP_PACKAGE_DESIGN_PROPERTY, "true")
      .setGoals("sonar:sonar");
    result = orchestrator.executeBuildQuietly(inspection);
    assertThat(result.getStatus()).isEqualTo(0);

  }

  /**
   * SONAR-3228
   */
  @Test
  public void shouldPersistMetricsEvenIfZero() {
    assumeTrue(JavaTestSuite.sonarqube_version_is_prior_to_5_2());
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("zero-value-metric-project"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    Sonar wsClient = orchestrator.getServer().getWsClient();

    Resource project = wsClient.find(ResourceQuery.createForMetrics("com.sonarsource.it.projects:zero-value-metric-project",
      "package_cycles", "package_feedback_edges", "package_tangles"));

    assertThat(project.getMeasureIntValue("package_cycles")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("package_feedback_edges")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("package_tangles")).isEqualTo(0);
  }

  /**
   * SONARJAVA-19
   */
  @Test
  public void suppressWarnings_nosonar() throws Exception {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("suppress-warnings"))
      .setCleanSonarGoals()
      .setProperty("sonar.profile", "suppress-warnings")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    assertThat(getMeasure("org.example:example", "violations").getValue()).isEqualTo(2);
  }

  private static Measure getMeasure(String resourceKey, String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

  /**
   * SONAR-4768
   */
  @Test
  public void support_jav_file_extension() {
    SonarRunner scan = SonarRunner.create(TestUtils.projectDir("jav-file-extension"))
      .setProperty("sonar.projectKey", "jav-file-extension")
      .setProperty("sonar.projectName", "jav-file-extension")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.sources", "src");
    orchestrator.executeBuild(scan);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("jav-file-extension",
      "files", "ncloc"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("ncloc")).isGreaterThan(0);
  }

  @Test
  public void support_change_of_extension_property() {
    SonarRunner scan = SonarRunner.create(TestUtils.projectDir("jav-file-extension"))
      .setProperty("sonar.projectKey", "jav-file-extension")
      .setProperty("sonar.projectName", "jav-file-extension")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.java.file.suffixes", ".txt,.foo")
      .setProperty("sonar.sources", "src");
    orchestrator.executeBuild(scan);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("jav-file-extension",
      "files", "ncloc"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("ncloc")).isGreaterThan(0);
  }

  @Test
  public void should_execute_rule_on_test() throws Exception {
    MavenLocation junit_4_11 = MavenLocation.of("junit", "junit", "4.11");
    new MavenLocator(orchestrator.getConfiguration()).copyToDirectory(junit_4_11, tmp.getRoot());
    MavenBuild build = MavenBuild.create()
        .setPom(TestUtils.projectPom("java-inner-classes"))
        .setProperty("sonar.profile", "ignored-test-check")
        .setProperty("sonar.java.test.binaries", "target/test-classes")
        .setProperty("sonar.java.test.libraries", new File(tmp.getRoot(), junit_4_11.getFilename()).getAbsolutePath())
        .setCleanPackageSonarGoals();

    orchestrator.executeBuild(build);
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:java-inner-classes", "violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(1);

  }
}
