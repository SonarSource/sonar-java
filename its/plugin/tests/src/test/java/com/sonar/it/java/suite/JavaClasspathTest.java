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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

/**
 * SONARJAVA-160
 */
public class JavaClasspathTest {

  private static final String PROJECT_KEY = "org.example:dit-check";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = JavaTestSuite.ORCHESTRATOR;

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();
  private String guavaJarPath;

  @BeforeClass
  public static void setUp() {
    buildDitProject();
  }

  @Before
  public void copyGuavaJar() {
    MavenLocation guava = MavenLocation.of("com.google.guava", "guava", "10.0.1");
    File subFolder = new File(tmp.getRoot(), "subFolder");
    File subSubFolder = new File(subFolder, "subSubFolder");
    new MavenLocator(ORCHESTRATOR.getConfiguration()).copyToDirectory(guava, subSubFolder);

    guavaJarPath = new File(subSubFolder.getAbsolutePath(), guava.getFilename()).getAbsolutePath();
  }

  @Before
  public void deleteData() {
    ORCHESTRATOR.resetData();
  }

  @Test
  public void should_use_new_java_binaries_property() {
    SonarRunner runner = ditProjectSonarRunner();
    runner.setProperty("sonar.java.binaries", "target/classes");
    ORCHESTRATOR.executeBuild(runner);
    assertThat(getNumberOfViolations()).isEqualTo(1);
  }

  @Test
  public void invalid_binaries_dir_should_fail_analysis() {
    SonarRunner runner = ditProjectSonarRunner();
    runner.setProperty("sonar.java.binaries", "target/dummy__Dir");
    BuildResult buildResult = ORCHESTRATOR.executeBuildQuietly(runner);
    assertThat(buildResult.getStatus()).isNotEqualTo(0);
    assertThat(buildResult.getLogs()).contains("No files nor directories matching 'target/dummy__Dir'");
  }

  @Test
  public void relative_path_and_wildcard_for_binaries_should_be_supported() {
    SonarRunner runner = ditProjectSonarRunner();
    runner.setProperty("sonar.java.binaries", "target/../target/clas**, ");
    ORCHESTRATOR.executeBuild(runner);
    assertThat(getNumberOfViolations()).isEqualTo(1);
  }

  @Test
  public void should_use_new_java_libraries_property() {
    SonarRunner runner = ditProjectSonarRunner();
    runner.setProperty("sonar.java.binaries", "target/classes");
    runner.setProperty("sonar.java.libraries", guavaJarPath);
    ORCHESTRATOR.executeBuild(runner);
    assertThat(getNumberOfViolations()).isEqualTo(2);
  }


  @Test
  public void should_support_the_old_binaries_and_libraries_properties() {
    SonarRunner runner = ditProjectSonarRunner();
    runner.setProperty("sonar.binaries", "target/classes");
    runner.setProperty("sonar.libraries", guavaJarPath);
    String logs = ORCHESTRATOR.executeBuild(runner).getLogs();

    assertThat(logs).contains("sonar.binaries and sonar.libraries are deprecated since version 2.5 of sonar-java-plugin," +
      " please use sonar.java.binaries and sonar.java.libraries instead");
    assertThat(getNumberOfViolations()).isEqualTo(2);
  }

  @Test
  public void should_not_log_warnings_if_properties_not_set() {
    SonarRunner runner = ditProjectSonarRunner();
    String logs = ORCHESTRATOR.executeBuild(runner).getLogs();

    assertThat(logs).doesNotContain("sonar.binaries and sonar.libraries are deprecated since version 2.5 of sonar-java-plugin," +
      " please use sonar.java.binaries and sonar.java.libraries instead");
    assertThat(getNumberOfViolations()).isEqualTo(0);
  }

  @Test
  public void directory_of_classes_in_library_should_be_supported() throws Exception {
    SonarRunner runner = ditProjectSonarRunner();
    runner.setProperty("sonar.java.libraries", "target/classes");
    ORCHESTRATOR.executeBuild(runner);
    assertThat(getNumberOfViolations()).isEqualTo(1);
  }

  @Test
  public void no_source_files_should_not_validate_binaries_for_backward_compatibility_with_sonar_maven_plugin_2_2() throws Exception {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("multi-module-project"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.profile", "dit-check")
      .setProperty("sonar.dynamicAnalysis", "false");
    int status = ORCHESTRATOR.executeBuildQuietly(build).getStatus();
    if (JavaTestSuite.sonarqube_version_is_prior_to_5_0()) {
      assertThat(status).isEqualTo(0);
    } else {
      assertThat(status).isGreaterThan(0);
    }
  }

  private static void buildDitProject() {
    mavenOnDitProject("clean package");
  }

  private static void mavenOnDitProject(String goal) {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("dit-check"))
      .setGoals(goal)
      .setProperty("sonar.profile", "dit-check")
      .setProperty("sonar.dynamicAnalysis", "false");
    ORCHESTRATOR.executeBuild(build);
  }

  private static SonarRunner ditProjectSonarRunner() {
    return SonarRunner.create(TestUtils.projectDir("dit-check"))
      .setProperty("sonar.projectKey", PROJECT_KEY)
      .setProperty("sonar.projectName", "dit-check")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.profile", "dit-check")
      .setProperty("sonar.sources", "src/main/java");
  }

  private int getNumberOfViolations() {
    Resource resource = ORCHESTRATOR.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, "violations"));
    return resource != null ? resource.getMeasure("violations").getValue().intValue() : -1;
  }

}
