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
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class JaCoCoTest {
  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  private static MavenBuild build;

  @BeforeClass
  public static void init() {
    orchestrator.resetData();

    build = MavenBuild.create(TestUtils.projectPom("squid"))
        .setProperty("sonar.scm.disabled", "true")
        .setProperty("sonar.profile", "squid");
  }

  @Test
  public void jacoco_report_should_be_read_when_analyzing_with_sonar_runner_jacoco_v_0_7_4() {
    build.setGoals("clean org.jacoco:jacoco-maven-plugin::0.7.4.201502262128:prepare-agent install -Dmaven.test.failure.ignore=true ");
    analyzeWithSonarRunner();
  }

  @Test
  public void jacoco_report_should_be_read_when_analyzing_with_sonar_runner_jacoco_latest() {
    Assume.assumeTrue(JavaTestSuite.isAtLeastPlugin3_4());
    build.setGoals("clean org.jacoco:jacoco-maven-plugin:prepare-agent install -Dmaven.test.failure.ignore=true ");
    analyzeWithSonarRunner();
  }

  private void analyzeWithSonarRunner() {
    BuildResult buildResult = orchestrator.executeBuild(SonarRunner.create(TestUtils.projectDir("squid"))
        .setProperty("sonar.projectKey", "squid")
        .setProperty("sonar.projectName", "squid")
        .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
        .setProperty("sonar.profile", "squid")
        .setProperty("sonar.sources", "src/main/java")
        .setProperty("sonar.java.binaries", "target/classes"));
    assertThat(buildResult.getLogs()).doesNotContain("No JaCoCo analysis of project coverage can be done since there is no class files.");
  }
}
