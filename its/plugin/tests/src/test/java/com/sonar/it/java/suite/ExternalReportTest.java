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

    List<Issue> issues = getExternalIssues("com.sonarsource.it.projects:checkstyle-external-report");
    if (externalIssuesSupported()) {
      assertThat(issues).hasSize(1);
      Issue issue = issues.get(0);
      assertThat(issue.componentKey()).isEqualTo("com.sonarsource.it.projects:checkstyle-external-report:src/main/java/Main.java");
      assertThat(issue.ruleKey()).isEqualTo("external_checkstyle:javadoc.JavadocPackageCheck");
      assertThat(issue.line()).isNull();
      assertThat(issue.message()).isEqualTo("Missing package-info.java file.");
      assertThat(issue.severity()).isEqualTo("MAJOR");
      assertThat(issue.debt()).isEqualTo("5min");
    } else {
      assertThat(issues).isEmpty();
    }
  }

  @Test
  public void pmd() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("pmd-external-report"))
      .setProperty("sonar.java.pmd.reportPaths", "target" + File.separator + "pmd.xml")
      .setGoals("org.apache.maven.plugins:maven-pmd-plugin:3.10.0:pmd", "sonar:sonar");
    orchestrator.executeBuild(build);

    String projectKey = "com.sonarsource.it.projects:pmd-external-report";
    List<Issue> issues = getExternalIssues(projectKey);
    if (externalIssuesSupported()) {
      assertThat(issues).hasSize(1);
      Issue issue = issues.get(0);
      assertThat(issue.componentKey()).isEqualTo(projectKey + ":src/main/java/Main.java");
      assertThat(issue.ruleKey()).isEqualTo("external_pmd:UnusedLocalVariable");
      assertThat(issue.line()).isEqualTo(3);
      assertThat(issue.message()).isEqualTo("Avoid unused local variables such as 'unused'.");
      assertThat(issue.severity()).isEqualTo("MAJOR");
      assertThat(issue.debt()).isEqualTo("5min");
    } else {
      assertThat(issues).isEmpty();
    }
  }

  @Test
  public void spotbugs() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("spotbugs-external-report"))
      .setProperty("sonar.java.spotbugs.reportPaths", "target" + File.separator + "spotbugsXml.xml")
      .setGoals("clean package com.github.spotbugs:spotbugs-maven-plugin:3.1.5:spotbugs", "sonar:sonar");
    orchestrator.executeBuild(build);

    String projectKey = "com.sonarsource.it.projects:spotbugs-external-report";
    List<Issue> issues = getExternalIssues(projectKey);
    boolean externalIssuesSupported = orchestrator.getServer().version().isGreaterThanOrEquals(7, 2);
    if (externalIssuesSupported) {
      assertThat(issues).hasSize(1);
      Issue issue = issues.get(0);
      assertThat(issue.componentKey()).isEqualTo(projectKey + ":src/main/java/org/myapp/Main.java");
      assertThat(issue.ruleKey()).isEqualTo("external_spotbugs:HE_EQUALS_USE_HASHCODE");
      assertThat(issue.line()).isEqualTo(6);
      assertThat(issue.message()).isEqualTo("org.myapp.Main defines equals and uses Object.hashCode()");
      assertThat(issue.severity()).isEqualTo("MAJOR");
      assertThat(issue.debt()).isEqualTo("5min");
    } else {
      assertThat(issues).isEmpty();
    }
  }

  private boolean externalIssuesSupported() {
    return orchestrator.getServer().version().isGreaterThanOrEquals(7, 2);
  }

  private List<Issue> getExternalIssues(String projectKey) {
    IssueClient issueClient = SonarClient.create(orchestrator.getServer().getUrl()).issueClient();
    return issueClient.find(IssueQuery.create().componentRoots(projectKey)).list().stream()
      .filter(issue -> issue.ruleKey().startsWith("external_"))
      .collect(Collectors.toList());
  }

}
