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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

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
  private static final String MODULE_CORE_PHYSICAL_NAME = "core";

  @BeforeClass
  public static void analyzeProject() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("struts-1.3.9-lite"))
      .setGoals("org.jacoco:jacoco-maven-plugin:prepare-agent clean verify");
    MavenBuild analysis = MavenBuild.create(TestUtils.projectPom("struts-1.3.9-lite"))
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.exclusions", "**/pom.xml")
      .setGoals("sonar:sonar");
    orchestrator.executeBuilds(build, analysis);
  }

  @Test
  public void struts_is_analyzed() throws Exception {
    assertThat(getComponent(PROJECT_STRUTS).getName()).isEqualTo("Struts");
    assertThat(getComponent(moduleKey()).getName()).isEqualTo(isGreater75() ? "core/src" : "Struts Core");
  }

  @Test
  public void size_metrics() {
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "files")).isEqualTo(320);
    String directoryKey = componentKey("org/apache/struts/action", "");
    assertThat(getMeasureAsInteger(directoryKey, "files")).isEqualTo(21);
    String fileKey = componentKey("org/apache/struts/action/", "Action.java");
    assertThat(getMeasureAsInteger(fileKey, "files")).isEqualTo(1);
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "lines")).isEqualTo(65059);
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "ncloc")).isEqualTo(27577);
    // 208 getter/setter
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "functions")).isEqualTo(2730 + 208);

    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "classes")).isEqualTo(337);
    assertThat(getMeasureAsInteger(moduleKey(), "files")).isEqualTo(134);
  }

  @Test
  public void unit_test_metrics() {
    int linesToCover = isGreater76() ? 15470 : 15494;
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "lines_to_cover")).isEqualTo(linesToCover, offset(10.0));
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "coverage")).isEqualTo(25.1, offset(0.2));
    assertThat(getMeasureAsDouble(moduleKey(), "coverage")).isEqualTo(36.8, offset(0.2));
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "line_coverage")).isEqualTo(25.5);
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "branch_coverage")).isEqualTo(24.2);

    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "tests")).isEqualTo(307);
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "test_execution_time")).isGreaterThan(200);
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "test_errors")).isEqualTo(0);
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "test_failures")).isEqualTo(0);
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "skipped_tests")).isEqualTo(0);
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "test_success_density")).isEqualTo(100.0);
  }

  @Test
  public void complexity_metrics() {
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "complexity")).isEqualTo(5589);

    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "class_complexity")).isEqualTo(16.6);
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "function_complexity")).isEqualTo(1.9);

    int expected_statements = 12103;
    expected_statements += 3; // empty statements in type declaration or member of classes in struts-1.3.9
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "statements")).isEqualTo(expected_statements);
  }

  @Test
  public void file_complexity_distribution() {
    assertThat(getMeasureValue(componentKey("org/apache/struts/config", ""), "file_complexity_distribution"))
      .isEqualTo(isGreater75() ? "0=4;5=1;10=2;20=1;30=5;60=3;90=1" : "0=3;5=1;10=2;20=1;30=5;60=2;90=1");
    assertThat(getMeasureValue(moduleKey(), "file_complexity_distribution")).isEqualTo("0=49;5=24;10=22;20=8;30=17;60=5;90=9");
    assertThat(getMeasureValue(PROJECT_STRUTS, "file_complexity_distribution")).isEqualTo("0=141;5=44;10=55;20=26;30=34;60=7;90=13");
  }

  @Test
  public void function_complexity_distribution() {
    String componentKey = componentKey("org/apache/struts/config", "");
    assertThat(getMeasureValue(componentKey, "function_complexity_distribution"))
      .isEqualTo(isGreater75() ? "1=163;2=103;4=13;6=11;8=6;10=0;12=5" : "1=134;2=96;4=12;6=9;8=6;10=0;12=5");
  }

  @Test
  public void should_not_persist_complexity_distributions_on_files() {
    String componentKey = componentKey("org/apache/struts/config/", "ConfigRuleSet.java");
    assertThat(getMeasure(componentKey, "function_complexity_distribution")).isNull();
    assertThat(getMeasure(componentKey, "file_complexity_distribution")).isNull();
  }

  private static String componentKey(String path, String file) {
    if (isGreater75()) {
      return String.format("%s:%s/src/main/java/%s%s", PROJECT_STRUTS, MODULE_CORE_PHYSICAL_NAME, path, file);
    }
    return String.format("%s:src/main/java/%s%s", MODULE_CORE, path, file);
  }

  private static String moduleKey() {
    if (isGreater75()) {
      return String.format("%s:%s/src", PROJECT_STRUTS, MODULE_CORE_PHYSICAL_NAME);
    }
    return MODULE_CORE;
  }

  private static boolean isGreater75() {
    return orchestrator.getServer().version().isGreaterThanOrEquals(7, 6);
  }

  private static boolean isGreater76() {
    return orchestrator.getServer().version().isGreaterThanOrEquals(7, 7);
  }

  private static String getMeasureValue(String componentKey, String metricKey) {
    return getMeasure(componentKey, metricKey).getValue();
  }
}
