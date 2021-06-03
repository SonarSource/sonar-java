/*
 * SonarQube Java
 * Copyright (C) 2013-2021 SonarSource SA
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
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaTutorialTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void test() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("java-tutorial")).setCleanPackageSonarGoals();
    TestUtils.provisionProject(orchestrator, "org.sonarsource.it.projects:java-tutorial", "java-tutorial", "java", "java-tutorial");
    executeAndAssertBuild(build);
  }

  @Test
  public void test_as_batch_mode() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("java-tutorial"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.java.internal.batchMode", "true")
      .setProperty("sonar.java.source", "1.5");
    TestUtils.provisionProject(orchestrator, "org.sonarsource.it.projects:java-tutorial", "java-tutorial", "java", "java-tutorial");
    executeAndAssertBuild(build);
  }

  private void executeAndAssertBuild(MavenBuild build) {
    orchestrator.executeBuild(build);

    List<Issue> issues = TestUtils.issuesForComponent(orchestrator, "org.sonarsource.it.projects:java-tutorial");
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
