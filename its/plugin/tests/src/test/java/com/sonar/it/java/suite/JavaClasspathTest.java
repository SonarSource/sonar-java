/*
 * SonarQube Java
 * Copyright (C) 2013-2019 SonarSource SA
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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsInteger;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * SONARJAVA-160
 */
public class JavaClasspathTest {

  private static final String PROJECT_KEY_DIT = "org.example:dit-check";
  private static final String PROJECT_KEY_AAR = "org.example:using-aar-dep";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = JavaTestSuite.ORCHESTRATOR;

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();
  private String guavaJarPath;
  private String aarPath;
  private String fakeGuavaJarPath;

  @BeforeClass
  public static void setUp() {
    buildDitProject();
  }

  @Before
  public void copyGuavaJar() {
    MavenLocation guava = MavenLocation.of("com.google.guava", "guava", "10.0.1");
    File subFolder = new File(tmp.getRoot(), "subFolder");
    File subSubFolder = new File(subFolder, "subSubFolder");
    ORCHESTRATOR.getConfiguration().locators().copyToDirectory(guava, subSubFolder);

    aarPath = new File(new File(TestUtils.projectDir("using-aar-dep"), "lib"), "cache-1.3.0.aar").getAbsolutePath();
    guavaJarPath = new File(subSubFolder.getAbsolutePath(), guava.getFilename()).getAbsolutePath();
    fakeGuavaJarPath = new File(new File(TestUtils.projectDir("dit-check"), "lib"), "fake-guava-1.0.jar").getAbsolutePath();
  }

  @Test
  public void should_use_new_java_binaries_property() {
    String projectKey = "should_use_new_java_binaries_property";
    SonarScanner scanner = ditProjectSonarScanner();
    scanner.setProperty("sonar.java.binaries", "target/classes");
    scanner.setProjectKey(projectKey);

    TestUtils.provisionProject(ORCHESTRATOR, projectKey, projectKey, "java", "dit-check");
    ORCHESTRATOR.executeBuild(scanner);
    assertThat(getNumberOfViolations(projectKey)).isEqualTo(1);
  }

  @Test
  public void invalid_binaries_dir_should_fail_analysis() {
    SonarScanner scanner = ditProjectSonarScanner();
    scanner.setProperty("sonar.java.binaries", "target/dummy__Dir");
    BuildResult buildResult = ORCHESTRATOR.executeBuildQuietly(scanner);
    assertThat(buildResult.getStatus()).isNotEqualTo(0);
    assertThat(buildResult.getLogs()).contains("No files nor directories matching 'target/dummy__Dir'");
  }

  @Test
  public void relative_path_and_wildcard_for_binaries_should_be_supported() {
    String projectKey = "relative_path_and_wildcard_for_binaries_should_be_supported";
    SonarScanner scanner = ditProjectSonarScanner();
    scanner.setProperty("sonar.java.binaries", "target/../target/clas**, ");
    scanner.setProjectKey(projectKey);
    TestUtils.provisionProject(ORCHESTRATOR, projectKey, projectKey, "java", "dit-check");
    ORCHESTRATOR.executeBuild(scanner);
    assertThat(getNumberOfViolations(projectKey)).isEqualTo(1);
  }

  @Test
  public void should_use_aar_library() {
    SonarScanner scanner = aarProjectSonarScanner();
    scanner.setProperty("sonar.java.libraries", aarPath);

    TestUtils.provisionProject(ORCHESTRATOR, PROJECT_KEY_AAR, "should_use_aar_library", "java", "using-aar-dep");
    ORCHESTRATOR.executeBuild(scanner);
    assertThat(getNumberOfViolations(PROJECT_KEY_AAR)).isEqualTo(1);
  }

  @Test
  public void should_use_new_java_libraries_property() {
    SonarScanner scanner = ditProjectSonarScanner();
    scanner.setProperty("sonar.java.binaries", "target/classes");
    scanner.setProperty("sonar.java.libraries", guavaJarPath);
    TestUtils.provisionProject(ORCHESTRATOR, PROJECT_KEY_DIT, PROJECT_KEY_DIT, "java", "dit-check");
    ORCHESTRATOR.executeBuild(scanner);
    assertThat(getNumberOfViolations(PROJECT_KEY_DIT)).isEqualTo(2);
  }

  @Test
  public void should_keep_order_libs() {
    String projectKey = "should_keep_order_libs";

    SonarScanner scanner = ditProjectSonarScanner();
    scanner.setProperty("sonar.java.binaries", "target/classes");
    scanner.setProperty("sonar.java.libraries", guavaJarPath + "," + fakeGuavaJarPath);
    scanner.setProperty("sonar.verbose", "true");
    scanner.setProjectKey(projectKey);

    TestUtils.provisionProject(ORCHESTRATOR, projectKey, projectKey, "java", "dit-check");
    ORCHESTRATOR.executeBuild(scanner);
    assertThat(getNumberOfViolations(projectKey)).isEqualTo(2);

    projectKey = "should_keep_order_libs_2";

    scanner = ditProjectSonarScanner();
    scanner.setProperty("sonar.java.binaries", "target/classes");
    scanner.setProperty("sonar.java.libraries", fakeGuavaJarPath + "," + guavaJarPath);
    scanner.setProperty("sonar.verbose", "true");
    scanner.setProjectKey(projectKey);
    TestUtils.provisionProject(ORCHESTRATOR, projectKey, projectKey, "java", "dit-check");
    ORCHESTRATOR.executeBuild(scanner);
    assertThat(getNumberOfViolations(projectKey)).isEqualTo(1);
  }

  @Test
  public void should_support_the_old_binaries_and_libraries_properties() {
    SonarScanner scanner = ditProjectSonarScanner();
    scanner.setProperty("sonar.binaries", "target/classes");
    scanner.setProperty("sonar.libraries", guavaJarPath);
    BuildResult buildResult = ORCHESTRATOR.executeBuildQuietly(scanner);

    assertThat(buildResult.getLogs()).contains("sonar.binaries and sonar.libraries are not supported since version 4.0 of sonar-java-plugin," +
      " please use sonar.java.binaries and sonar.java.libraries instead");
    assertThat(buildResult.isSuccess()).isFalse();
  }

  @Test
  public void should_log_warnings_if_binaries_missing() {
    SonarScanner scanner = ditProjectSonarScanner();
    BuildResult buildResult = ORCHESTRATOR.executeBuildQuietly(scanner);
    String logs = buildResult.getLogs();
    assertThat(logs).contains("Please provide compiled classes of your project with sonar.java.binaries property");
    assertThat(buildResult.isSuccess()).isFalse();
  }

  @Test
  public void directory_of_classes_in_library_should_be_supported() throws Exception {
    String projectKey = "directory_of_classes_in_library_should_be_supported";
    SonarScanner scanner = ditProjectSonarScanner();
    scanner.setProperty("sonar.java.binaries", "target");
    scanner.setProperty("sonar.java.libraries", "target/classes");
    scanner.setProjectKey(projectKey);
    TestUtils.provisionProject(ORCHESTRATOR, projectKey, projectKey, "java", "dit-check");
    ORCHESTRATOR.executeBuild(scanner);
    assertThat(getNumberOfViolations(projectKey)).isEqualTo(1);
  }

  private static void buildDitProject() {
    mavenOnDitProject("clean package");
  }

  private static void mavenOnDitProject(String goal) {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("dit-check"))
      .setGoals(goal)
      .setProperty("sonar.dynamicAnalysis", "false");

    ORCHESTRATOR.executeBuild(build);
  }

  private static SonarScanner aarProjectSonarScanner() {
    return SonarScanner.create(TestUtils.projectDir("using-aar-dep"))
      .setProperty("sonar.projectKey", PROJECT_KEY_AAR)
      .setProperty("sonar.projectName", "using-aar-dep")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.sources", "src/main/java");
  }

  private static SonarScanner ditProjectSonarScanner() {
    return SonarScanner.create(TestUtils.projectDir("dit-check"))
      .setProperty("sonar.projectKey", PROJECT_KEY_DIT)
      .setProperty("sonar.projectName", "dit-check")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.sources", "src/main/java");
  }

  private int getNumberOfViolations(String projectKey) {
    return getMeasureAsInteger(projectKey, "violations");
  }

}
