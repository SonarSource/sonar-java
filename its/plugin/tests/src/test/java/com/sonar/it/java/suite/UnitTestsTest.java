/*
 * Java :: IT :: Plugin :: Tests
 * Copyright (C) 2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.it.java.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class UnitTestsTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  @Test
  public void tests_without_main_code() {
    MavenBuild build = MavenBuild.create()
      .setPom(TestUtils.projectPom("tests-without-main-code"))
      .setGoals("clean test-compile surefire:test", "sonar:sonar");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:tests-without-main-code",
      "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density"));

    assertThat(project.getMeasure("tests").getIntValue()).isEqualTo(1);
    assertThat(project.getMeasure("test_errors").getIntValue()).isEqualTo(0);
    assertThat(project.getMeasure("test_failures").getIntValue()).isEqualTo(0);
    assertThat(project.getMeasure("skipped_tests").getIntValue()).isEqualTo(1);
    assertThat(project.getMeasure("test_execution_time").getIntValue()).isGreaterThan(0);
    assertThat(project.getMeasure("test_success_density").getValue()).isEqualTo(100.0);
  }

  @Test
  public void tests_with_report_name_suffix() {
    MavenBuild build = MavenBuild.create()
        .setPom(TestUtils.projectPom("tests-surefire-suffix"))
        .setGoals("clean test-compile surefire:test -Dsurefire.reportNameSuffix=Run1","test-compile surefire:test -Dsurefire.reportNameSuffix=Run2", "sonar:sonar");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:tests-surefire-suffix",
        "tests", "test_errors", "test_failures", "skipped_tests", "test_execution_time", "test_success_density"));

    assertThat(project.getMeasure("tests").getIntValue()).isEqualTo(2);
    assertThat(project.getMeasure("test_errors").getIntValue()).isEqualTo(0);
    assertThat(project.getMeasure("test_failures").getIntValue()).isEqualTo(0);
    assertThat(project.getMeasure("skipped_tests").getIntValue()).isEqualTo(2);
    assertThat(project.getMeasure("test_execution_time").getIntValue()).isGreaterThan(0);
    assertThat(project.getMeasure("test_success_density").getValue()).isEqualTo(100.0);
  }

}
