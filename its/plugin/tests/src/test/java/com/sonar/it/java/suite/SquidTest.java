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
import java.util.Objects;
import java.util.regex.Pattern;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;

public class SquidTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("squid"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.scm.disabled", "true");
    TestUtils.provisionProject(orchestrator, "com.sonarsource.it.samples:squid", "squid", "java", "squid");
    orchestrator.executeBuild(build);
  }

  @Test
  public void should_detect_missing_package_info() throws Exception {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().components("com.sonarsource.it.samples:squid")).list();
    assertThat(issues).hasSize(2);
    assertThat(issues.stream().map(Issue::ruleKey)).allMatch("squid:S1228"::equals);
    assertThat(issues.stream().map(Issue::line)).allMatch(Objects::isNull);
    String sep = "[/\\\\]";
    Pattern packagePattern = Pattern.compile("'src" + sep + "main" + sep + "java" + sep + "package[12]'");
    assertThat(issues.stream().map(Issue::message)).allMatch(msg -> packagePattern.matcher(msg).find());
    List<Issue> issuesOnTestPackage = issueClient.find(IssueQuery.create().components("com.sonarsource.it.samples:squid:src/test/java/package1")).list();
    assertThat(issuesOnTestPackage).isEmpty();
  }

}
