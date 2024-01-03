/*
 * SonarQube Java
 * Copyright (C) 2013-2024 SonarSource SA
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class PackageInfoTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void should_detect_package_info_issues() {
    String projectKey = "org.sonarsource.it.projects:package-info";
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("package-info"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.sources", "src/main/java,src/main/other-src")
      .setProperty("sonar.scm.disabled", "true");
    TestUtils.provisionProject(orchestrator, projectKey, "package-info", "java", "package-info");

    orchestrator.executeBuild(build);

    List<Issue> issues = TestUtils.issuesForComponent(orchestrator, projectKey);
    List<String> packageInfoRuleKeys = asList("java:S1228", "java:S4032");

    assertThat(issues).hasSize(3);
    assertThat(issues.stream().map(Issue::getRule)).allMatch(packageInfoRuleKeys::contains);
    assertThat(issues.stream().map(Issue::getLine)).allMatch(line -> line == 0);

    Pattern packagePattern = Pattern.compile("'org\\.package[12]'");
    List<Issue> s1228Issues = issues.stream().filter(issue -> issue.getRule().equals("java:S1228")).collect(Collectors.toList());
    assertThat(s1228Issues).hasSize(2);
    assertThat(s1228Issues).extracting(Issue::getMessage).allMatch(msg -> packagePattern.matcher(msg).find());

    List<Issue> s4032Issues = issues.stream().filter(issue -> issue.getRule().equals("java:S4032")).collect(Collectors.toList());
    assertThat(s4032Issues).hasSize(1);
    assertThat(s4032Issues.get(0).getMessage()).isEqualTo("Remove this package.");
    assertThat(s4032Issues.get(0).getComponent()).isEqualTo(projectKey + ":src/main/other-src/org/package4/package-info.java");

    List<Issue> issuesOnTestPackage = TestUtils.issuesForComponent(orchestrator, projectKey + ":src/test/java/package1");
    assertThat(issuesOnTestPackage).isEmpty();
  }

  @Test
  public void should_take_into_account_package_annotations() {
    String projectKey = "org.sonarsource.it.projects:package-info-annotations";
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("package-info-annotations"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.scm.disabled", "true");
    TestUtils.provisionProject(orchestrator, projectKey, "package-info-annotations", "java", "package-info-annotations");
    orchestrator.executeBuild(build);

    List<Issue> issues = TestUtils.issuesForComponent(orchestrator, projectKey);
    assertThat(issues).hasSize(11);
  }

}
