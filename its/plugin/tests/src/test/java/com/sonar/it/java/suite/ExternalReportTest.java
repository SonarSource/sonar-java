/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.it.java.suite;

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import java.io.File;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Common.Severity;
import org.sonarqube.ws.Issues.Issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ExternalReportTest {

  @ClassRule
  public static OrchestratorRule orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void checkstyle() {
    MavenBuild build = TestUtils.createMavenBuild().setPom(TestUtils.projectPom("checkstyle-external-report"))
      .setProperty("sonar.java.checkstyle.reportPaths", "target" + File.separator + "checkstyle-result.xml")
      .setGoals("org.apache.maven.plugins:maven-checkstyle-plugin:3.0.0:checkstyle", "sonar:sonar");
    orchestrator.executeBuild(build);

    List<Issue> issues = getExternalIssues("org.sonarsource.it.projects:checkstyle-external-report");
    assertThat(issues).hasSize(1);
    Issue issue = issues.get(0);
    assertThat(issue.getComponent()).isEqualTo("org.sonarsource.it.projects:checkstyle-external-report:src/main/java/Main.java");
    assertThat(issue.getRule()).isEqualTo("external_checkstyle:javadoc.JavadocPackageCheck");
    assertThat(issue.getLine()).isZero();
    assertThat(issue.getMessage()).contains("package-info.java");
    assertThat(issue.getSeverity()).isEqualTo(Severity.MAJOR);
    assertThat(issue.getDebt()).isEqualTo("5min");
  }

  @Test
  public void pmd() {
    MavenBuild build = TestUtils.createMavenBuild().setPom(TestUtils.projectPom("pmd-external-report"))
      .setProperty("sonar.java.pmd.reportPaths", "target" + File.separator + "pmd.xml")
      .setGoals("org.apache.maven.plugins:maven-pmd-plugin:3.10.0:pmd", "sonar:sonar");
    orchestrator.executeBuild(build);

    String projectKey = "org.sonarsource.it.projects:pmd-external-report";
    List<Issue> issues = getExternalIssues(projectKey);
    assertThat(issues).hasSize(1);
    Issue issue = issues.get(0);
    assertThat(issue.getComponent()).isEqualTo(projectKey + ":src/main/java/Main.java");
    assertThat(issue.getRule()).isEqualTo("external_pmd:UnusedLocalVariable");
    assertThat(issue.getLine()).isEqualTo(3);
    assertThat(issue.getMessage()).isEqualTo("Avoid unused local variables such as 'unused'.");
    assertThat(issue.getSeverity()).isEqualTo(Severity.MAJOR);
    assertThat(issue.getDebt()).isEqualTo("5min");
  }

  @Test
  public void spotbugs() {
    MavenBuild build = TestUtils.createMavenBuild().setPom(TestUtils.projectPom("spotbugs-external-report"))
      .setProperty("sonar.java.spotbugs.reportPaths", "target" + File.separator + "spotbugsXml.xml")
      .setGoals("clean package com.github.spotbugs:spotbugs-maven-plugin:4.5.3.0:spotbugs", "sonar:sonar");
    orchestrator.executeBuild(build);

    String projectKey = "org.sonarsource.it.projects:spotbugs-external-report";
    List<Issue> issues = getExternalIssues(projectKey);
    assertThat(issues).hasSize(4);
    assertThat(issues).extracting(Issue::getComponent, Issue::getRule, Issue::getLine, Issue::getMessage, Issue::getSeverity, Issue::getDebt)
      .containsExactlyInAnyOrder(
        tuple(projectKey + ":src/main/java/org/myapp/Main.java", "external_spotbugs:HE_EQUALS_USE_HASHCODE", 6, "org.myapp.Main defines equals and uses Object.hashCode()", Severity.MAJOR, "5min"),
        tuple(projectKey + ":src/main/java/org/myapp/App.java", "external_fbcontrib:DLC_DUBIOUS_LIST_COLLECTION", 14, "Class org.myapp.App defines List based fields but uses them like Sets", Severity.MAJOR, "5min"),
        tuple(projectKey + ":src/main/java/org/myapp/App.java", "external_fbcontrib:ABC_ARRAY_BASED_COLLECTIONS", 14, "Method org.myapp.App.getGreeting(int[]) uses array as basis of collection", Severity.MAJOR, "5min"),
        tuple(projectKey + ":src/main/java/org/myapp/App.java", "external_spotbugs:RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", 14, "Return value of java.util.List.contains(Object) ignored, but method has no side effect", Severity.MAJOR, "5min")
      );
  }

  private List<Issue> getExternalIssues(String projectKey) {
    return TestUtils.issuesForComponent(orchestrator, projectKey)
      .stream()
      .filter(issue -> issue.getRule().startsWith("external_"))
      .toList();
  }

}
