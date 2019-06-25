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
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaExtensionsTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void test() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("java-extension"))
      .setCleanSonarGoals();
    TestUtils.provisionProject(orchestrator, "org.sonarsource.it.projects:java-extension","java-extension","java","java-extension");
    orchestrator.executeBuild(build);

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();

    List<Issue> issues = issueClient.find(IssueQuery.create().rules("java-extension:example").componentRoots("org.sonarsource.it.projects:java-extension")).list();
    //We found issues so the extension rule was properly set.
    assertThat(issues).hasSize(4);
    issues = issueClient.find(IssueQuery.create().rules("java-extension:subscriptionexamplecheck").componentRoots("org.sonarsource.it.projects:java-extension")).list();
    assertThat(issues).hasSize(3);
    issues = issueClient.find(IssueQuery.create().rules("java-extension:subscriptionexampletestcheck").componentRoots("org.sonarsource.it.projects:java-extension")).list();
    assertThat(issues).hasSize(1);
  }

}
