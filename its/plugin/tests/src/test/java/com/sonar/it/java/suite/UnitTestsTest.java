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
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Measures.Measure;

import static com.sonar.it.java.suite.JavaTestSuite.getMeasures;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

public class UnitTestsTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void tests_without_main_code() {
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("tests-without-main-code"))
      .setGoals("clean test-compile surefire:test", "sonar:sonar");
    orchestrator.executeBuild(build);

    Map<String, Measure> measures = getMeasures("org.sonarsource.it.projects:tests-without-main-code",
      "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density");

    assertThat(parseInt(measures.get("tests").getValue())).isEqualTo(1);
    assertThat(parseInt(measures.get("test_errors").getValue())).isEqualTo(0);
    assertThat(parseInt(measures.get("test_failures").getValue())).isEqualTo(0);
    assertThat(parseInt(measures.get("skipped_tests").getValue())).isEqualTo(1);
    assertThat(parseInt(measures.get("test_execution_time").getValue())).isGreaterThan(0);
    assertThat(parseDouble(measures.get("test_success_density").getValue())).isEqualTo(100.0);
  }

  @Test
  public void tests_with_report_name_suffix() {
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("tests-surefire-suffix"))
      .setGoals("clean test-compile surefire:test -Dsurefire.reportNameSuffix=Run1", "test-compile surefire:test -Dsurefire.reportNameSuffix=Run2", "sonar:sonar");
    orchestrator.executeBuild(build);

    Map<String, Measure> measures = getMeasures("org.sonarsource.it.projects:tests-surefire-suffix",
      "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density");

    assertThat(parseInt(measures.get("tests").getValue())).isEqualTo(2);
    assertThat(parseInt(measures.get("test_errors").getValue())).isEqualTo(0);
    assertThat(parseInt(measures.get("test_failures").getValue())).isEqualTo(0);
    assertThat(parseInt(measures.get("skipped_tests").getValue())).isEqualTo(2);
    assertThat(parseInt(measures.get("test_execution_time").getValue())).isGreaterThan(0);
    assertThat(parseDouble(measures.get("test_success_density").getValue())).isEqualTo(100.0);
  }

}
