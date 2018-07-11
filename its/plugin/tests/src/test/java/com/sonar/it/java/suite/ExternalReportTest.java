/*
 * SonarQube Java
 * Copyright (C) 2013-2018 SonarSource SA
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
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;

public class ExternalReportTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void checkstyle() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("checkstyle-external-report"))
      .setProperty("sonar.java.checkstyle.reportPaths", "target" + File.separator + "checkstyle-result.xml")
      .setGoals("org.apache.maven.plugins:maven-checkstyle-plugin:3.0.0:checkstyle", "sonar:sonar");
    orchestrator.executeBuild(build);

    List<Issue> issues = getExternalIssues();
    boolean externalIssuesSupported = orchestrator.getServer().version().isGreaterThanOrEquals(7, 2);
    if (externalIssuesSupported) {
      assertThat(issues).hasSize(1);
      Issue issue = issues.get(0);
      assertThat(issue.componentKey()).isEqualTo("com.sonarsource.it.projects:checkstyle-external-report:src/main/java/Main.java");
      assertThat(issue.ruleKey()).isEqualTo("external_checkstyle:javadoc.JavadocPackageCheck");
      assertThat(issue.line()).isNull();
      assertThat(issue.message()).isEqualTo("Missing package-info.java file.");
      assertThat(issue.severity()).isEqualTo("MINOR");
      assertThat(issue.debt()).isEqualTo("30min");
    } else {
      assertThat(issues).isEmpty();
    }
  }

  private List<Issue> getExternalIssues() {
    IssueClient issueClient = SonarClient.create(orchestrator.getServer().getUrl()).issueClient();
    return issueClient.find(IssueQuery.create().componentRoots("com.sonarsource.it.projects:checkstyle-external-report")).list().stream()
      .filter(issue -> issue.ruleKey().startsWith("external_"))
      .collect(Collectors.toList());
  }

}
