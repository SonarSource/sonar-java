/*
 * SonarQube Java
 * Copyright (C) 2013-2024 SonarSource SA
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
import java.util.List;
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaTutorialTest {

  @ClassRule
  public static OrchestratorRule orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void test() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("java-tutorial")).setCleanPackageSonarGoals();
    String projectKey = "org.sonarsource.it.projects:java-tutorial";
    TestUtils.provisionProject(orchestrator, projectKey, "java-tutorial", "java", "java-tutorial");
    executeAndAssertBuild(build, projectKey);
  }

  @Test
  public void test_as_batch_mode() {
    String projectKey = "org.sonarsource.it.projects:java-tutorial-batch";
    String projectName = "java-tutorial-batch";
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("java-tutorial"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.projectKey", projectKey)
      .setProperty("sonar.projectName", projectName)
      .setProperty("sonar.java.experimental.batchModeSizeInKB", "8000");
    TestUtils.provisionProject(orchestrator, projectKey, projectName, "java", "java-tutorial");
    executeAndAssertBuild(build, projectKey);
  }

  private void executeAndAssertBuild(MavenBuild build, String projectKey) {
    orchestrator.executeBuild(build);

    List<Issue> issues = TestUtils.issuesForComponent(orchestrator, projectKey);
    assertThat(issues).hasSize(31);

    assertThat(issuesForRule(issues, "mycompany-java:AvoidTreeList")).hasSize(2);
    assertThat(issuesForRule(issues, "mycompany-java:AvoidMethodDeclaration")).hasSize(24);
    assertThat(issuesForRule(issues, "mycompany-java:AvoidBrandInMethodNames")).hasSize(2);
    assertThat(issuesForRule(issues, "mycompany-java:SecurityAnnotationMandatory")).hasSize(2);
    assertThat(issuesForRule(issues, "mycompany-java:SpringControllerRequestMappingEntity")).hasSize(1);
  }

  private static Stream<String> issuesForRule(List<Issue> issues, String key) {
    return issues.stream().map(Issue::getRule).filter(key::equals);
  }

}
