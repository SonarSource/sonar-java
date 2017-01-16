/*
 * SonarQube Java
 * Copyright (C) 2013-2017 SonarSource SA
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

import com.google.common.collect.Sets;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static com.sonar.it.java.suite.JavaTestSuite.getMeasure;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsDouble;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsInteger;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaComplexityTest {

  public static final String PROJECT = "org.sonar.it.core:java-complexity";

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void analyzeProject() {
    orchestrator.resetData();
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("java-complexity"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.profile", "java-complexity");
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
  public void testAverageMethodComplexity() {
    assertThat(getMeasureAsDouble(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "function_complexity")).isEqualTo(1.5);

    assertThat(getMeasureAsDouble(JavaTestSuite.keyFor(PROJECT, "complexity/", "ContainsInnerClasses.java"), "function_complexity")).isEqualTo(1.3);

    assertThat(getMeasureAsDouble(JavaTestSuite.keyFor(PROJECT, "complexity/", "AnonymousClass.java"), "function_complexity")).isEqualTo(1.0);

    assertThat(getMeasureAsDouble(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "function_complexity")).isEqualTo(1.3);
    assertThat(getMeasureAsDouble(PROJECT, "function_complexity")).isEqualTo(1.3);
  }

  @Test
  public void testAverageClassComplexity() {
    assertThat(getMeasureAsDouble(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "class_complexity")).isEqualTo(7.0);

    assertThat(getMeasureAsDouble(JavaTestSuite.keyFor(PROJECT, "complexity/", "ContainsInnerClasses.java"), "class_complexity")).isEqualTo(1.7);

    assertThat(getMeasureAsDouble(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "class_complexity")).isEqualTo(2.7);
  }

  /**
   * SONAR-3289
   */
  @Test
  public void testDistributionOfFileComplexity() throws Exception {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "file_complexity_distribution").getValue()).isEqualTo("0=2;5=2;10=0;20=0;30=0;60=0;90=0");
    assertThat(getMeasure(PROJECT, "file_complexity_distribution").getValue()).isEqualTo("0=2;5=2;10=0;20=0;30=0;60=0;90=0");
  }

  @Test
  public void testDistributionOfMethodComplexity() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "function_complexity_distribution").getValue()).isEqualTo("1=8;2=2;4=0;6=0;8=0;10=0;12=0");
    assertThat(getMeasure(PROJECT, "function_complexity_distribution").getValue()).isEqualTo("1=8;2=2;4=0;6=0;8=0;10=0;12=0");
  }

  @Test
  public void shouldNotPersistDistributionOnFiles() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "class_complexity_distribution")).isNull();
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "function_complexity_distribution")).isNull();
  }

  @Test
  public void complexity_sqale_computation() throws Exception {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();

    List<Issue> issues = issueClient.find(IssueQuery.create().componentRoots(PROJECT)).list();
    assertThat(issues).hasSize(3);
    Set<String> debts = Sets.newHashSet();
    for (Issue issue : issues) {
      debts.add(issue.debt());
    }
    assertThat(debts).hasSize(2).containsOnly("11min", "12min");
  }
}
