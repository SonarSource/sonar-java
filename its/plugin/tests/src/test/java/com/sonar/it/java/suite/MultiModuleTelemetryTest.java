/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource SA
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

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiModuleTelemetryTest {

  @ClassRule
  public static OrchestratorRule orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void test() {
    MavenBuild build = TestUtils.createMavenBuild()
      .setPom(TestUtils.projectPom("multi-module"))
      .setCleanPackageSonarGoals()
      .setDebugLogs(true);

    String projectKey = "org.sonarsource.it.projects:parent-project";
    TestUtils.provisionProject(orchestrator, projectKey, "multi-module", "java", "multi-module");

    BuildResult buildResult = orchestrator.executeBuild(build);

    assertThat(buildResult.getLogs())
      .containsOnlyOnce("Telemetry java.language.version: 8")
      .containsOnlyOnce("Telemetry java.module_count: 2")
      .containsOnlyOnce("Telemetry java.scanner_app: ScannerMaven")
      .containsOnlyOnce("Telemetry java.dependency.lombok: 1.18.30,1.18.38");
  }

}
