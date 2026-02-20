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

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.java.telemetry.TelemetryKey;

import static org.assertj.core.api.Assertions.assertThat;

public class Java25FeaturesTelemetryTest {

  @ClassRule
  public static OrchestratorRule orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void test() {
    MavenBuild build = TestUtils.createMavenBuild()
      .setPom(TestUtils.projectPom("java-25-new-features"))
      .setCleanPackageSonarGoals()
      .setDebugLogs(true);

    String projectKey = "org.sonarsource.it.projects:parent-project";
    TestUtils.provisionProject(orchestrator, projectKey, "multi-module", "java", "multi-module");

    BuildResult buildResult = orchestrator.executeBuild(build);

    assertThat(buildResult.getLogs())
      .containsOnlyOnce("Telemetry %s: %d".formatted(TelemetryKey.JAVA_FEATURE_FLEXIBLE_CONSTRUCTOR_BODY, 1))
      .containsOnlyOnce("Telemetry %s: %d".formatted(TelemetryKey.JAVA_FEATURE_MODULE_IMPORT, 1))
      .containsOnlyOnce("Telemetry %s: %d".formatted(TelemetryKey.JAVA_FEATURE_COMPACT_SOURCE_FILES, 1));
  }
}
