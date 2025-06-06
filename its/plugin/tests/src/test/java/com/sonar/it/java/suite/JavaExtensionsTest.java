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
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaExtensionsTest {

  @ClassRule
  public static OrchestratorRule orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void test() {
    MavenBuild build = TestUtils.createMavenBuild().setPom(TestUtils.projectPom("java-extension"))
      .setCleanSonarGoals();
    TestUtils.provisionProject(orchestrator, "org.sonarsource.it.projects:java-extension","java-extension","java","java-extension");
    orchestrator.executeBuild(build);

    List<Issue> issues = TestUtils.issuesForComponent(orchestrator, "org.sonarsource.it.projects:java-extension");

    // We found issues so the extension rule was properly set.
    assertThat(issues.stream().map(Issue::getRule).filter("java-extension:example"::equals)).hasSize(4);
    assertThat(issues.stream().map(Issue::getRule).filter("java-extension:subscriptionexamplecheck"::equals)).hasSize(3);
    assertThat(issues.stream().map(Issue::getRule).filter("java-extension:subscriptionexampletestcheck"::equals)).hasSize(1);
  }

}
