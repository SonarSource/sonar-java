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
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Measures;

import static com.sonar.it.java.suite.JavaTestSuite.getComponent;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasure;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsInteger;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  /**
   * See SONAR-1865
   */
  @Test
  public void shouldAcceptFilenamesWithDollar() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("dollar-in-names"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Components.Component file = getComponent(JavaTestSuite.keyFor("org.sonarsource.it.projects:dollar-in-names", "dollars/", "FilenameWith$Dollar.java"));
    assertThat(file).isNotNull();
    assertThat(file.getName()).contains("FilenameWith$Dollar");
  }

  /**
   * SONARJAVA-444
   */
  @Test
  public void shouldFailIfInvalidJavaPackage() {
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("invalid-java-package"))
      .setCleanSonarGoals();

    BuildResult buildResult = orchestrator.executeBuildQuietly(build);
    assertThat(buildResult.getLastStatus()).isEqualTo(0);
  }

  @Test
  public void should_create_issue_pom_xml() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/profile-pom-xml.xml"));
    orchestrator.getServer().provisionProject("org.sonarsource.java:test-project", "Test Project");
    orchestrator.getServer().associateProjectToQualityProfile("org.sonarsource.java:test-project", "java", "java-pom-xml");

    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("pom-xml"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    List<Issue> issues = TestUtils.issuesForComponent(orchestrator, "org.sonarsource.java:test-project:pom.xml");

    assertThat(issues).hasSize(1);
    assertThat(issues.iterator().next().getRule()).isEqualTo("squid:S3423");
  }

  @Test
  public void measures_on_directory() {
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("measures-on-directory"))
      .setCleanPackageSonarGoals();
    BuildResult result = orchestrator.executeBuildQuietly(build);
    // since sonar-java 2.1 does not fail if multiple package in same directory.
    assertThat(result.getLastStatus()).isEqualTo(0);
  }

  @Test
  public void multiple_package_in_directory_should_not_fail() throws Exception {
    MavenBuild inspection = MavenBuild.create()
      .setPom(TestUtils.projectPom("multiple-packages-in-directory"))
      .setCleanPackageSonarGoals();
    BuildResult result = orchestrator.executeBuildQuietly(inspection);
    assertThat(result.getLastStatus()).isEqualTo(0);
    inspection = MavenBuild.create()
      .setPom(TestUtils.projectPom("multiple-packages-in-directory"))
      .setProperty("sonar.skipPackageDesign", "true")
      .setGoals("sonar:sonar");
    result = orchestrator.executeBuildQuietly(inspection);
    assertThat(result.getLastStatus()).isEqualTo(0);
  }

  /**
   * SONARJAVA-1615
   */
  @Test
  public void filtered_issues() throws Exception {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("filtered-issues"))
      .setCleanPackageSonarGoals();

    TestUtils.provisionProject(orchestrator, "org.example:example", "filtered-issues", "java", "filtered-issues");
    orchestrator.executeBuild(build);

    assertThat(getMeasureAsInteger("org.example:example", "violations")).isEqualTo(2);

    List<Issue> issues = TestUtils.issuesForComponent(orchestrator, "org.example:example:src/main/java/EclispeI18NFiltered.java");

    assertThat(issues).hasSize(2);
    for (Issue issue : issues) {
      assertThat(issue.getRule()).matches(value -> "squid:S1444".equals(value) || "squid:ClassVariableVisibilityCheck".equals(value));
      assertThat(issue.getLine()).isEqualTo(17);
    }
  }

  /**
   * SONAR-4768
   */
  @Test
  public void support_jav_file_extension() {
    SonarScanner scan = SonarScanner.create(TestUtils.projectDir("jav-file-extension"))
      .setProperty("sonar.projectKey", "jav-file-extension")
      .setProperty("sonar.projectName", "jav-file-extension")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.sources", "src");
    orchestrator.executeBuild(scan);

    assertThat(getMeasureAsInteger("jav-file-extension", "files")).isEqualTo(1);
    assertThat(getMeasureAsInteger("jav-file-extension", "ncloc")).isGreaterThan(0);
  }

  @Test
  public void support_change_of_extension_property() {
    SonarScanner scan = SonarScanner.create(TestUtils.projectDir("jav-file-extension"))
      .setProperty("sonar.projectKey", "jav-file-extension")
      .setProperty("sonar.projectName", "jav-file-extension")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.java.file.suffixes", ".txt,.foo")
      .setProperty("sonar.sources", "src")
      .setProperty("sonar.java.binaries", "src");
    orchestrator.executeBuild(scan);

    assertThat(getMeasureAsInteger("jav-file-extension", "files")).isEqualTo(2);
    assertThat(getMeasureAsInteger("jav-file-extension", "ncloc")).isGreaterThan(0);
  }

  @Test
  public void should_execute_rule_on_test() throws Exception {
    MavenLocation junit_4_11 = MavenLocation.of("junit", "junit", "4.11");
    orchestrator.getConfiguration().locators().copyToDirectory(junit_4_11, tmp.getRoot());
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("java-inner-classes"))
      .setProperty("sonar.java.test.binaries", "target/test-classes")
      .setProperty("sonar.java.test.libraries", new File(tmp.getRoot(), junit_4_11.getFilename()).getAbsolutePath())
      .setCleanPackageSonarGoals();
    TestUtils.provisionProject(orchestrator, "org.sonarsource.it.projects:java-inner-classes", "java-inner-classes", "java", "ignored-test-check");
    orchestrator.executeBuild(build);
    assertThat(getMeasureAsInteger("org.sonarsource.it.projects:java-inner-classes", "violations")).isEqualTo(1);
  }

  @Test
  public void java_aware_visitor_rely_on_java_version() {
    String sonarJavaSource = "sonar.java.source";

    MavenBuild build = MavenBuild.create(TestUtils.projectPom("java-version-aware-visitor"))
      .setCleanSonarGoals();
    String projectKey = "java-version-aware-visitor";
    build.setProperties("sonar.projectKey", projectKey);

    TestUtils.provisionProject(orchestrator, projectKey, "java-version-aware-visitor", "java", "java-version-aware-visitor");

    // no java version specified. maven scanner gets maven default version : java 5.
    orchestrator.executeBuild(build);
    assertThat(getMeasureAsInteger(projectKey, "violations")).isEqualTo(0);

    // invalid java version. got issue on java 7 code
    build.setProperty(sonarJavaSource, "jdk_1.6");
    BuildResult buildResult = orchestrator.executeBuild(build);
    // build should not fail
    assertThat(buildResult.getLastStatus()).isEqualTo(0);
    // build logs should contains warning related to sources
    assertThat(buildResult.getLogs()).contains("Invalid java version");
    assertThat(getMeasureAsInteger(projectKey, "violations")).isEqualTo(1);

    // upper version. got issue on java 7 code
    build.setProperty(sonarJavaSource, "1.8");
    orchestrator.executeBuild(build);
    assertThat(getMeasureAsInteger(projectKey, "violations")).isEqualTo(1);

    // lower version. no issue on java 7 code
    build.setProperty(sonarJavaSource, "1.6");
    orchestrator.executeBuild(build);
    assertThat(getMeasureAsInteger(projectKey, "violations")).isEqualTo(0);

    SonarScanner scan = SonarScanner.create(TestUtils.projectDir("java-version-aware-visitor"))
      .setProperty("sonar.projectKey", "org.example:example-scanner")
      .setProperty("sonar.projectName", "example")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.sources", "src/main/java");
    TestUtils.provisionProject(orchestrator, "org.example:example-scanner", "java-version-aware-visitor", "java", "java-version-aware-visitor");
    orchestrator.executeBuild(scan);
    // no java version specified, got issue on java 7 code
    assertThat(getMeasureAsInteger("org.example:example-scanner", "violations")).isEqualTo(1);
  }

  @Test
  public void collect_feedback_on_server() {
    SonarScanner scan = SonarScanner.create(TestUtils.projectDir("java-parse-error"))
      .setProperty("sonar.projectKey", "java-parse-error")
      .setProperty("sonar.projectName", "java-parse-error")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.java.collectAnalysisErrors", "true")
      .setProperty("sonar.sources", "src");
    orchestrator.executeBuild(scan);

    Measures.Measure sonarjava_feedback = getMeasure("java-parse-error", "sonarjava_feedback");
    assertThat(sonarjava_feedback).isNotNull();
    assertThat(sonarjava_feedback.getValue()).startsWith("[{\"message\":\"Parse error at line 4 column 0");
  }
}
