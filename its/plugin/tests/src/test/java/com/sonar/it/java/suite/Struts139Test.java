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
    int linesToCover = isGreater76() ? 15452 : 15476;
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "lines_to_cover")).isEqualTo(linesToCover, offset(10.0));
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "coverage")).isEqualTo(25.1, offset(0.2));
    assertThat(getMeasureAsDouble(moduleKey(), "coverage")).isEqualTo(36.8, offset(0.2));
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "line_coverage")).isEqualTo(25.4, offset(0.5));
    assertThat(getMeasureAsDouble(PROJECT_STRUTS, "branch_coverage")).isEqualTo(23.9);

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

    int expected_statements = 12103;
    expected_statements += 3; // empty statements in type declaration or member of classes in struts-1.3.9
    assertThat(getMeasureAsInteger(PROJECT_STRUTS, "statements")).isEqualTo(expected_statements);
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
