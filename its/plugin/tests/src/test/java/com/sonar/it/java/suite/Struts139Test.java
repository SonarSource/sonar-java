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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;

import org.fest.assertions.Delta;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class Struts139Test {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  private static final String PROJECT_STRUTS = "org.apache.struts:struts-parent";
  private static final String MODULE_CORE = "org.apache.struts:struts-core";
  private static final String PACKAGE_ACTION = JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/action", "");
  private static final String FILE_ACTION = JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/action/", "Action.java");
  private String currentProject;
  private String currentModule;
  private String currentPackage;
  private String currentFile;

  @BeforeClass
  public static void analyzeProject() {
    orchestrator.resetData();

    MavenBuild build = MavenBuild.create(TestUtils.projectPom("struts-1.3.9-lite"));
    build.setGoals("org.jacoco:jacoco-maven-plugin:prepare-agent clean verify");
    MavenBuild analysis = MavenBuild.create(TestUtils.projectPom("struts-1.3.9-lite"))
      .setProperty("sonar.scm.disabled", "true")
      .setGoals("sonar:sonar");
    orchestrator.executeBuilds(build, analysis);
  }

  private void setCurrentProject() {
    currentProject = PROJECT_STRUTS;
    currentModule = MODULE_CORE;
    currentPackage = PACKAGE_ACTION;
    currentFile = FILE_ACTION;
  }

  @Test
  public void should_return_analysis_date() {
    Date date = orchestrator.getServer().getWsClient().find(new ResourceQuery(PROJECT_STRUTS)).getDate();
    assertThat(date).isNotNull();
    assertThat(date.getYear()).isGreaterThan(111); // 1900 + 110
  }

  @Test
  public void struts_is_analyzed() {
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery(PROJECT_STRUTS)).getName()).isEqualTo("Struts");
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery(PROJECT_STRUTS)).getVersion()).isEqualTo("1.3.9");
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery(MODULE_CORE)).getName()).isEqualTo("Struts Core");
  }

  @Test
  public void size_metrics() {
    setCurrentProject();
    assertThat(getProjectMeasure("files").getIntValue()).isEqualTo(320);
    assertThat(getPackageMeasure("files").getIntValue()).isEqualTo(21);
    assertThat(getFileMeasure("files").getIntValue()).isEqualTo(1);
    assertThat(getProjectMeasure("lines").getIntValue()).isEqualTo(65059);
    assertThat(getProjectMeasure("ncloc").getIntValue()).isEqualTo(27577);
    // 208 getter/setter
    assertThat(getProjectMeasure("functions").getIntValue()).isEqualTo(2730 + 208);

    assertThat(getProjectMeasure("classes").getIntValue()).isEqualTo(337);
    assertThat(getCoreModuleMeasure("files").getIntValue()).isEqualTo(134);
  }

  @Test
  public void unit_test_metrics() {
    setCurrentProject();
    assertThat(getProjectMeasure("lines_to_cover").getValue()).isEqualTo(15474, Delta.delta(10));
    assertThat(getProjectMeasure("coverage").getValue()).isEqualTo(25.1, Delta.delta(0.1));
    assertThat(getCoreModuleMeasure("coverage").getValue()).isEqualTo(36.8, Delta.delta(0.2));
    assertThat(getProjectMeasure("line_coverage").getValue()).isEqualTo(25.5);
    assertThat(getProjectMeasure("branch_coverage").getValue()).isEqualTo(24.1);
    if(JavaTestSuite.sonarqube_version_is_prior_to_6_2()) {
      // overall coverage is the same as UT if no IT.
      assertThat(getCoreModuleMeasure("overall_coverage").getValue()).isEqualTo(36.8, Delta.delta(0.2));
      assertThat(getProjectMeasure("overall_coverage").getValue()).isEqualTo(25.1, Delta.delta(0.1));
      assertThat(getProjectMeasure("overall_line_coverage").getValue()).isEqualTo(25.5);
      assertThat(getProjectMeasure("overall_branch_coverage").getValue()).isEqualTo(24.1);
    }
    assertThat(getProjectMeasure("tests").getIntValue()).isEqualTo(307);
    assertThat(getProjectMeasure("test_execution_time").getIntValue()).isGreaterThan(200);
    assertThat(getProjectMeasure("test_errors").getIntValue()).isEqualTo(0);
    assertThat(getProjectMeasure("test_failures").getIntValue()).isEqualTo(0);
    assertThat(getProjectMeasure("skipped_tests").getIntValue()).isEqualTo(0);
    assertThat(getProjectMeasure("test_success_density").getValue()).isEqualTo(100.0);
  }

  @Test
  public void complexity_metrics() {
    setCurrentProject();
    assertThat(getProjectMeasure("complexity").getIntValue()).isEqualTo(5603);

    assertThat(getProjectMeasure("class_complexity").getValue()).isEqualTo(16.6);
    assertThat(getProjectMeasure("function_complexity").getValue()).isEqualTo(1.9);

    int expected_statements = 12103;
    expected_statements += 3; // empty statements in type declaration or member of classes in struts-1.3.9
    assertThat(getProjectMeasure("statements").getIntValue()).isEqualTo(expected_statements);
  }

  @Test
  public void file_complexity_distribution() {
    setCurrentProject();
    assertThat(orchestrator.getServer().getWsClient().find(
        ResourceQuery.createForMetrics(JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/config", ""), "file_complexity_distribution"))
        .getMeasure("file_complexity_distribution").getData()).isEqualTo("0=3;5=1;10=2;20=1;30=5;60=2;90=1");
    assertThat(getCoreModuleMeasure("file_complexity_distribution").getData()).isEqualTo("0=49;5=24;10=22;20=8;30=17;60=5;90=9");
    assertThat(getProjectMeasure("file_complexity_distribution").getData()).isEqualTo("0=141;5=44;10=55;20=26;30=34;60=7;90=13");
  }

  @Test
  public void function_complexity_distribution() {
    assertThat(orchestrator.getServer().getWsClient().find(
        ResourceQuery.createForMetrics(JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/config", ""), "function_complexity_distribution"))
        .getMeasure("function_complexity_distribution").getData()).isEqualTo("1=134;2=96;4=12;6=9;8=6;10=0;12=5");
  }

  @Test
  public void should_not_persist_complexity_distributions_on_files() {
    setCurrentProject();
    ResourceQuery query = ResourceQuery.createForMetrics(
      JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/config/", "ConfigRuleSet.java"),
      "function_complexity_distribution", "file_complexity_distribution");
    assertThat(orchestrator.getServer().getWsClient().find(query).getMeasures().size()).isEqualTo(0);
  }

  @Test
  public void should_get_details_of_coverage_hits() {
    setCurrentProject();
    Resource resource = orchestrator.getServer().getWsClient().find(
      ResourceQuery.createForMetrics(JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/action/", "ActionForward.java"), "coverage_line_hits_data"));
    Measure coverageData = resource.getMeasure("coverage_line_hits_data");
    assertThat(coverageData).isNotNull();
    assertThat(coverageData.getData().length()).isGreaterThan(10);
    assertThat(coverageData.getData()).matches("(\\d+=\\d+;{0,1})+");
  }

  private Measure getFileMeasure(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(currentFile, metricKey)).getMeasure(metricKey);
  }

  private Measure getCoreModuleMeasure(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(currentModule, metricKey)).getMeasure(metricKey);
  }

  private Measure getProjectMeasure(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(currentProject, metricKey)).getMeasure(metricKey);
  }

  private Measure getPackageMeasure(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(currentPackage, metricKey)).getMeasure(metricKey);
  }
}
