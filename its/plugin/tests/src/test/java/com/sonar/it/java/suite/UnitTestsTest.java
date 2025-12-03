/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.it.java.suite;

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit4.OrchestratorRule;
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
  public static OrchestratorRule orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void tests_without_main_code() {
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("tests-without-main-code"))
      .setGoals("clean test-compile surefire:test", "sonar:sonar");
    orchestrator.executeBuild(build);

    Map<String, Measure> measures = getMeasures("org.sonarsource.it.projects:tests-without-main-code",
      "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density");

    assertThat(parseInt(measures.get("tests").getValue())).isEqualTo(1);
    assertThat(parseInt(measures.get("test_errors").getValue())).isZero();
    assertThat(parseInt(measures.get("test_failures").getValue())).isZero();
    assertThat(parseInt(measures.get("skipped_tests").getValue())).isEqualTo(1);
    assertThat(parseInt(measures.get("test_execution_time").getValue())).isPositive();
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
    assertThat(parseInt(measures.get("test_errors").getValue())).isZero();
    assertThat(parseInt(measures.get("test_failures").getValue())).isZero();
    assertThat(parseInt(measures.get("skipped_tests").getValue())).isEqualTo(2);
    assertThat(parseInt(measures.get("test_execution_time").getValue())).isPositive();
    assertThat(parseDouble(measures.get("test_success_density").getValue())).isEqualTo(100.0);
  }

}
