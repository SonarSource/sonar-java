/*
 * SonarQube Java
 * Copyright (C) 2013-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

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
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "AnonymousClass.java"), "classes").getIntValue()).isEqualTo(1);
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "ZeroComplexity.java"), "classes").getIntValue()).isEqualTo(1);
    assertThat(getMeasure(PROJECT, "classes").getIntValue()).isEqualTo(6);
  }

  @Test
  public void testNumberMethods() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "ZeroComplexity.java"), "functions").getIntValue()).isEqualTo(0);
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "ContainsInnerClasses.java"), "functions").getIntValue()).isEqualTo(4);
    assertThat(getMeasure(PROJECT, "functions").getIntValue()).isEqualTo(10);
  }

  @Test
  public void methodsInAnonymousClassesShouldBeIgnored() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "AnonymousClass.java"), "functions").getIntValue()).isEqualTo(2);
  }

  @Test
  public void testFileComplexity() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "complexity").getIntValue()).isEqualTo(7);
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "ContainsInnerClasses.java"), "complexity").getIntValue()).isEqualTo(5);
  }

  @Test
  public void testFileComplexityWithAnonymousClasses() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "AnonymousClass.java"), "complexity").getIntValue()).isEqualTo(3 + 1);
  }

  @Test
  public void testPackageComplexity() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "complexity").getIntValue()).isEqualTo(16);
  }

  @Test
  public void testProjectComplexity() {
    assertThat(getMeasure(PROJECT, "complexity").getIntValue()).isEqualTo(16);
  }

  @Test
  public void testAverageMethodComplexity() {
    // complexity 6 / 4 methods
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "function_complexity").getValue()).isEqualTo(1.5);

    // complexity 5 / 4 methods. Real value is 1.25 but round up to 1.3
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "ContainsInnerClasses.java"), "function_complexity").getValue()).isEqualTo(1.3);

    // (1 + 3) / 2 = 2
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "AnonymousClass.java"), "function_complexity").getValue()).isEqualTo(2.0);

    // Helloworld: 6/4
    // ContainsInnerClasses: 5/4
    // AnonymousClass: 4/2
    // => 19/12 Real value is 1.5833 but lowered to 1.5
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "function_complexity").getValue()).isEqualTo(1.5);
    assertThat(getMeasure(PROJECT, "function_complexity").getValue()).isEqualTo(1.5);
  }

  @Test
  public void testAverageClassComplexity() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "class_complexity").getValue()).isEqualTo(7.0);

    // 1 + 1 + 3 => complexity 5/3
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "ContainsInnerClasses.java"), "class_complexity").getValue()).isEqualTo(1.7);

    // 1 + 1 + 3 + 7 + 0 + 4 => 16/6 = 2.666
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "class_complexity").getValue()).isEqualTo(2.7);
  }

  /**
   * SONAR-3289
   */
  @Test
  public void testDistributionOfFileComplexity() throws Exception {
    // 0 + 4 + 5 + 6 => 2 in range [0,5[ and 2 in range [5,10[
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "file_complexity_distribution").getData()).isEqualTo("0=2;5=2;10=0;20=0;30=0;60=0;90=0");
    assertThat(getMeasure(PROJECT, "file_complexity_distribution").getData()).isEqualTo("0=2;5=2;10=0;20=0;30=0;60=0;90=0");
  }

  @Test
  public void testDistributionOfMethodComplexity() {
    // ContainsInnerClasses: 1+ 1 + 2 + 1
    // Helloworld: 1 + 3 (static block is not a method)
    // Anonymous class : 1 + 3
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity", ""), "function_complexity_distribution").getData()).isEqualTo("1=7;2=3;4=0;6=0;8=0;10=0;12=0");
    assertThat(getMeasure(PROJECT, "function_complexity_distribution").getData()).isEqualTo("1=7;2=3;4=0;6=0;8=0;10=0;12=0");
  }

  @Test
  public void shouldNotPersistDistributionOnFiles() {
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "class_complexity_distribution")).isNull();
    assertThat(getMeasure(JavaTestSuite.keyFor(PROJECT, "complexity/", "Helloworld.java"), "function_complexity_distribution")).isNull();
  }

  private Measure getMeasure(String resourceKey, String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

  @Test
  public void complexity_sqale_computation() throws Exception {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();

    List<Issue> issues = issueClient.find(IssueQuery.create().componentRoots(PROJECT)).list();
    assertThat(issues).hasSize(4);
    Set<String> debts = Sets.newHashSet();
    for (Issue issue : issues) {
      debts.add(issue.debt());
    }
    assertThat(debts).hasSize(2).containsOnly("11min", "12min");
  }
}
