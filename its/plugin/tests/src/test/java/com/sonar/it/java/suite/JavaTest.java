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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.MavenLocator;
import java.io.File;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.ws.WsComponents;

import static com.sonar.it.java.suite.JavaTestSuite.getComponent;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsInteger;
import static org.assertj.core.api.Assertions.assertThat;
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

    WsComponents.Component file = getComponent(JavaTestSuite.keyFor("org.sonar.it.core:dollar-in-names", "dollars/", "FilenameWith$Dollar.java"));
    assertThat(file).isNotNull();
    assertThat(file.getName()).contains("FilenameWith$Dollar");
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

    assertThat(getMeasureAsInteger("com.sonarsource.it.samples:commented-out-java-code", "ncloc")).isEqualTo(7);
    assertThat(getMeasureAsInteger("com.sonarsource.it.samples:commented-out-java-code", "commented_out_code_lines")).isNull();
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
    assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("6.3"));
    
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/profile-pom-xml.xml"));
    orchestrator.getServer().provisionProject("org.sonarsource.java:test-project", "Test Project");
    orchestrator.getServer().associateProjectToQualityProfile("org.sonarsource.java:test-project", "java", "java-pom-xml");

    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("pom-xml"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().components("org.sonarsource.java:test-project:pom.xml")).list();
    assertThat(issues).hasSize(1);
    assertThat(issues.iterator().next().ruleKey()).isEqualTo("squid:S3423");
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
      .setCleanPackageSonarGoals()
      .setProperty("sonar.profile", "filtered-issues");
    orchestrator.executeBuild(build);

    assertThat(getMeasureAsInteger("org.example:example", "violations")).isEqualTo(2);

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().components("org.example:example:src/main/java/EclispeI18NFiltered.java")).list();
    assertThat(issues).hasSize(2);
    for (Issue issue : issues) {
      assertThat(issue.ruleKey()).matches(value -> "squid:S1444".equals(value) || "squid:ClassVariableVisibilityCheck".equals(value));

      assertThat(issue.line()).isEqualTo(17);
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
      .setProperty("sonar.sources", "src");
    orchestrator.executeBuild(scan);

    assertThat(getMeasureAsInteger("jav-file-extension", "files")).isEqualTo(2);
    assertThat(getMeasureAsInteger("jav-file-extension", "ncloc")).isGreaterThan(0);
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
    assertThat(getMeasureAsInteger("com.sonarsource.it.samples:java-inner-classes", "violations")).isEqualTo(1);
  }

  @Test
  public void java_aware_visitor_rely_on_java_version() {
    String sonarJavaSource = "sonar.java.source";

    MavenBuild build = MavenBuild.create(TestUtils.projectPom("java-version-aware-visitor"))
      .setCleanSonarGoals()
      .setProperty("sonar.profile", "java-version-aware-visitor");

    // no java version specified. maven scanner gets maven default version : java 5.
    orchestrator.executeBuild(build);
    assertThat(getMeasureAsInteger("org.example:example", "violations")).isEqualTo(0);

    // invalid java version. got issue on java 7 code
    build.setProperty(sonarJavaSource, "jdk_1.6");
    BuildResult buildResult = orchestrator.executeBuild(build);
    // build should not fail
    assertThat(buildResult.getLastStatus()).isEqualTo(0);
    // build logs should contains warning related to sources
    assertThat(buildResult.getLogs()).contains("Invalid java version");
    assertThat(getMeasureAsInteger("org.example:example", "violations")).isEqualTo(1);

    // upper version. got issue on java 7 code
    build.setProperty(sonarJavaSource, "1.8");
    orchestrator.executeBuild(build);
    assertThat(getMeasureAsInteger("org.example:example", "violations")).isEqualTo(1);

    // lower version. no issue on java 7 code
    build.setProperty(sonarJavaSource, "1.6");
    orchestrator.executeBuild(build);
    assertThat(getMeasureAsInteger("org.example:example", "violations")).isEqualTo(0);

    SonarScanner scan = SonarScanner.create(TestUtils.projectDir("java-version-aware-visitor"))
      .setProperty("sonar.projectKey", "org.example:example-scanner")
      .setProperty("sonar.projectName", "example")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.profile", "java-version-aware-visitor")
      .setProperty("sonar.sources", "src/main/java");
    orchestrator.executeBuild(scan);
    // no java version specified, got issue on java 7 code
    assertThat(getMeasureAsInteger("org.example:example-scanner", "violations")).isEqualTo(1);
  }
}
