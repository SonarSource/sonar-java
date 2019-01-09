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
import com.sonar.orchestrator.build.MavenBuild;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;

public class TestsAsFirstCitizensTest {

  private static final String PROJECT = "tests-as-first-citizens";
  private static final String PROJECT_KEY_TESTS_AS_FIRST_CITIZENS = getProjectKey(true);
  private static final String PROJECT_KEY_NORMAL = getProjectKey(false);

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void test_and_main_sources_separated() throws Exception {
    analyzeProject(false);

    assertThat(getProjectMeasureAsInteger(PROJECT_KEY_NORMAL, "ncloc")).isEqualTo(10);
    assertThat(getProjectMeasureAsDouble(PROJECT_KEY_NORMAL, "sqale_debt_ratio")).isEqualTo(3.3);

    List<Issue> issues = getIssues(PROJECT_KEY_NORMAL);
    assertThat(issues).hasSize(3);

    String testFileKey = keyForTestFile(PROJECT_KEY_NORMAL);
    List<Issue> testFileIssues = issues.stream().filter(issue -> testFileKey.equals(issue.componentKey())).collect(Collectors.toList());
    assertThat(testFileIssues).hasSize(1);
    assertThat(testFileIssues.stream().map(Issue::ruleKey)).containsOnly("squid:S2970");
  }

  @Test
  public void tests_as_first_citizens() throws Exception {
    analyzeProject(true);

    assertThat(getProjectMeasureAsInteger(PROJECT_KEY_TESTS_AS_FIRST_CITIZENS, "ncloc")).isEqualTo(27);
    assertThat(getProjectMeasureAsDouble(PROJECT_KEY_TESTS_AS_FIRST_CITIZENS, "sqale_debt_ratio")).isEqualTo(1.9);

    List<Issue> issues = getIssues(PROJECT_KEY_TESTS_AS_FIRST_CITIZENS);
    assertThat(issues).hasSize(5);

    String testFileKey = keyForTestFile(PROJECT_KEY_TESTS_AS_FIRST_CITIZENS);
    List<Issue> testFileIssues = issues.stream().filter(issue -> testFileKey.equals(issue.componentKey())).collect(Collectors.toList());
    assertThat(testFileIssues).hasSize(3);
    assertThat(testFileIssues.stream().map(Issue::ruleKey)).containsExactlyInAnyOrder("squid:S2970", "squid:S1172", "squid:S2259");
  }

  private static void analyzeProject(boolean testsAsFirstCitizen) {
    String status = testsAsFirstCitizen ? "enabled" : "disabled";
    String projectKey = getProjectKey(testsAsFirstCitizen);
    String projectName = String.format("IT:: Tests as First Citizens (%s)", status);

    MavenBuild analysis = MavenBuild.create(TestUtils.projectPom(PROJECT))
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.java.tests.firstcitizen", Boolean.toString(testsAsFirstCitizen))
      .setProperty("sonar.projectKey", projectKey)
      .setProperty("sonar.projectName", projectName)
      .setCleanPackageSonarGoals();
    orchestrator.getServer().provisionProject(projectKey, projectName);
    orchestrator.executeBuilds(analysis);
  }

  private static String getProjectKey(boolean testsAsFirstCitizen) {
    String status = testsAsFirstCitizen ? "enabled" : "disabled";
    return String.format("it:%s-%s", PROJECT, status);
  }

  private static List<Issue> getIssues(String projectKey) {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    return issueClient.find(IssueQuery.create().componentRoots(projectKey)).list();
  }

  private static Integer getProjectMeasureAsInteger(String projectKey, String metricKey) {
    return JavaTestSuite.getMeasureAsInteger(projectKey, metricKey);
  }

  private static Double getProjectMeasureAsDouble(String projectKey, String metricKey) {
    return JavaTestSuite.getMeasureAsDouble(projectKey, metricKey);
  }

  private static String keyForTestFile(String projectKey) {
    return projectKey + ":" + "src/test/java/org/foo/bar/MyBarTest.java";
  }

}
