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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;

import static com.sonar.it.java.suite.JavaTestSuite.getMeasure;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsInteger;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaComplexityTest {

  public static final String PROJECT = "org.sonarsource.it.projects:java-complexity";

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void analyzeProject() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("java-complexity"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.java.binaries", "target");
    TestUtils.provisionProject(orchestrator, PROJECT, "java-complexity", "java", "java-complexity");
    orchestrator.executeBuild(build);
  }

  @Test
  public void testNumberOfClasses() {
    assertThat(getMeasureAsInteger(JavaTestSuite.keyFor(PROJECT, "complexity/", "AnonymousClass.java"), "classes")).isEqualTo(1);
    assertThat(getMeasureAsInteger(JavaTestSuite.keyFor(PROJECT, "complexity/", "ZeroComplexity.java"), "classes")).isEqualTo(1);
    assertThat(getMeasureAsInteger(PROJECT, "classes")).isEqualTo(6);
  }

  @Test
  public void testNumberMethods() {
    assertThat(getMeasureAsInteger(JavaTestSuite.keyFor(PROJECT, "complexity/", "ZeroComplexity.java"), "functions")).isEqualTo(0);
    assertThat(getMeasureAsInteger(JavaTestSuite.keyFor(PROJECT, "complexity/", "ContainsInnerClasses.java"), "functions")).isEqualTo(4);
    assertThat(getMeasureAsInteger(PROJECT, "functions")).isEqualTo(10);
  }

  @Test
  public void methodsInAnonymousClassesShouldBeIgnored() {
    assertThat(getMeasureAsInteger(JavaTestSuite.keyFor(PROJECT, "complexity/", "AnonymousClass.java"), "functions")).isEqualTo(2);
  }

  @Test
  public void testFileComplexity() {
    assertThat(getMeasureAsInteger(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "complexity")).isEqualTo(7);
    assertThat(getMeasureAsInteger(JavaTestSuite.keyFor(PROJECT, "complexity/", "ContainsInnerClasses.java"), "complexity")).isEqualTo(5);
  }

  @Test
  public void testFileComplexityWithAnonymousClasses() {
    assertThat(getMeasureAsInteger(JavaTestSuite.keyFor(PROJECT, "complexity/", "AnonymousClass.java"), "complexity")).isEqualTo(3 + 1);
  }

  @Test
  public void testPackageComplexity() {
    assertThat(getMeasureAsInteger(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "complexity")).isEqualTo(16);
  }

  @Test
  public void testProjectComplexity() {
    assertThat(getMeasureAsInteger(PROJECT, "complexity")).isEqualTo(16);
  }

  @Test
  public void shouldNotPersistDistributionOnFiles() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "function_complexity_distribution")).isNull();
  }

  @Test
  public void complexity_sqale_computation() throws Exception {
    List<Issue> issues = TestUtils.issuesForComponent(orchestrator, PROJECT);

    assertThat(issues).hasSize(3);
    Set<String> debts = new HashSet<>();
    for (Issue issue : issues) {
      debts.add(issue.getDebt());
    }
    assertThat(debts).hasSize(2).containsOnly("11min", "12min");
  }
}
