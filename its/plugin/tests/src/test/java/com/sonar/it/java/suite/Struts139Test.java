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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsMeasures.Measure;

import static com.sonar.it.java.suite.JavaTestSuite.getComponent;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasure;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsDouble;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

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
  public void struts_is_analyzed() {
    assertThat(getComponent(PROJECT_STRUTS).getName()).isEqualTo("Struts");
    assertThat(getComponent(MODULE_CORE).getName()).isEqualTo("Struts Core");
  }

  @Test
  public void size_metrics() {
    setCurrentProject();
    assertThat(getProjectMeasureAsInteger("files")).isEqualTo(320);
    assertThat(getPackageMeasureAsInteger("files")).isEqualTo(21);
    assertThat(getFileMeasureAsInteger("files")).isEqualTo(1);
    assertThat(getProjectMeasureAsInteger("lines")).isEqualTo(65059);
    assertThat(getProjectMeasureAsInteger("ncloc")).isEqualTo(27577);
    // 208 getter/setter
    assertThat(getProjectMeasureAsInteger("functions")).isEqualTo(2730 + 208);

    assertThat(getProjectMeasureAsInteger("classes")).isEqualTo(337);
    assertThat(getCoreModuleMeasureAsInteger("files")).isEqualTo(134);
  }

  @Test
  public void unit_test_metrics() {
    setCurrentProject();
    int linesToCover = 15498;
    if (JavaTestSuite.sonarqube_version_is_prior_to_6_2()) {
      linesToCover = 15474;
    }
    assertThat(getProjectMeasureAsDouble("lines_to_cover")).isEqualTo(linesToCover, offset(10.0));
    assertThat(getProjectMeasureAsDouble("coverage")).isEqualTo(25.1, offset(0.1));
    assertThat(getCoreModuleMeasureAsDouble("coverage")).isEqualTo(36.8, offset(0.2));
    assertThat(getProjectMeasureAsDouble("line_coverage")).isEqualTo(25.5);
    assertThat(getProjectMeasureAsDouble("branch_coverage")).isEqualTo(24.1);
    if (JavaTestSuite.sonarqube_version_is_prior_to_6_2()) {
      // overall coverage is the same as UT if no IT.
      assertThat(getCoreModuleMeasureAsDouble("overall_coverage")).isEqualTo(36.8, offset(0.2));
      assertThat(getProjectMeasureAsDouble("overall_coverage")).isEqualTo(25.1, offset(0.1));
      assertThat(getProjectMeasureAsDouble("overall_line_coverage")).isEqualTo(25.5);
      assertThat(getProjectMeasureAsDouble("overall_branch_coverage")).isEqualTo(24.1);
    }
    assertThat(getProjectMeasureAsInteger("tests")).isEqualTo(307);
    assertThat(getProjectMeasureAsInteger("test_execution_time")).isGreaterThan(200);
    assertThat(getProjectMeasureAsInteger("test_errors")).isEqualTo(0);
    assertThat(getProjectMeasureAsInteger("test_failures")).isEqualTo(0);
    assertThat(getProjectMeasureAsInteger("skipped_tests")).isEqualTo(0);
    assertThat(getProjectMeasureAsDouble("test_success_density")).isEqualTo(100.0);
  }

  @Test
  public void complexity_metrics() {
    setCurrentProject();
    assertThat(getProjectMeasureAsInteger("complexity")).isEqualTo(5603);

    assertThat(getProjectMeasureAsDouble("class_complexity")).isEqualTo(16.6);
    assertThat(getProjectMeasureAsDouble("function_complexity")).isEqualTo(1.9);

    int expected_statements = 12103;
    expected_statements += 3; // empty statements in type declaration or member of classes in struts-1.3.9
    assertThat(getProjectMeasureAsInteger("statements")).isEqualTo(expected_statements);
  }

  @Test
  public void file_complexity_distribution() {
    setCurrentProject();
    assertThat(getMeasure(JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/config", ""), "file_complexity_distribution").getValue())
      .isEqualTo("0=3;5=1;10=2;20=1;30=5;60=2;90=1");
    assertThat(getCoreModuleMeasure("file_complexity_distribution").getValue()).isEqualTo("0=49;5=24;10=22;20=8;30=17;60=5;90=9");
    assertThat(getProjectMeasure("file_complexity_distribution").getValue()).isEqualTo("0=141;5=44;10=55;20=26;30=34;60=7;90=13");
  }

  @Test
  public void function_complexity_distribution() {
    assertThat(getMeasure(JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/config", ""), "function_complexity_distribution").getValue())
      .isEqualTo("1=134;2=96;4=12;6=9;8=6;10=0;12=5");
  }

  @Test
  public void should_not_persist_complexity_distributions_on_files() {
    assertThat(getMeasure(JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/config/", "ConfigRuleSet.java"), "function_complexity_distribution")).isNull();
    assertThat(getMeasure(JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/config/", "ConfigRuleSet.java"), "file_complexity_distribution")).isNull();
  }

  @Test
  public void should_get_details_of_coverage_hits() {
    setCurrentProject();
    Measure coverage = getMeasure(JavaTestSuite.keyFor(MODULE_CORE, "org/apache/struts/action/", "ActionForward.java"), "coverage_line_hits_data");
    assertThat(coverage).isNotNull();
    assertThat(coverage.getValue().length()).isGreaterThan(10);
    assertThat(coverage.getValue()).matches("(\\d+=\\d+;{0,1})+");
  }

  private Integer getFileMeasureAsInteger(String metricKey) {
    return getMeasureAsInteger(currentFile, metricKey);
  }

  private Measure getCoreModuleMeasure(String metricKey) {
    return getMeasure(currentModule, metricKey);
  }

  private Integer getCoreModuleMeasureAsInteger(String metricKey) {
    return getMeasureAsInteger(currentModule, metricKey);
  }

  private Double getCoreModuleMeasureAsDouble(String metricKey) {
    return getMeasureAsDouble(currentModule, metricKey);
  }

  private Measure getProjectMeasure(String metricKey) {
    return getMeasure(currentProject, metricKey);
  }

  private Integer getProjectMeasureAsInteger(String metricKey) {
    return getMeasureAsInteger(currentProject, metricKey);
  }

  private Double getProjectMeasureAsDouble(String metricKey) {
    return getMeasureAsDouble(currentProject, metricKey);
  }

  private Integer getPackageMeasureAsInteger(String metricKey) {
    return getMeasureAsInteger(currentPackage, metricKey);
  }
}
