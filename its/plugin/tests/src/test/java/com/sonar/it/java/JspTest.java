/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.it.java;

import com.sonar.it.java.suite.JavaTestSuite;
import com.sonar.it.java.suite.TestUtils;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.junit4.OrchestratorRuleBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;

import static com.sonar.it.java.suite.TestUtils.extractTelemetryLogs;
import static com.sonar.it.java.suite.TestUtils.patternWithLiteralDot;
import static org.assertj.core.api.Assertions.assertThat;

public class JspTest {

  private static final String PROJECT = "servlet-jsp";

  public static boolean isCommunityEditionTestsOnly() {
    return "true".equals(System.getProperty("communityEditionTestsOnly"));
  }

  @ClassRule
  public static final OrchestratorRule ENTERPRISE_ORCHESTRATOR_OR_NULL = getEnterpriseOrchestratorOrNull();

  private static OrchestratorRule getEnterpriseOrchestratorOrNull() {
    if (isCommunityEditionTestsOnly()) {
      return null;
    }
    OrchestratorRuleBuilder orchestratorBuilder = OrchestratorRule.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
      .setEdition(Edition.ENTERPRISE_LW)
      .activateLicense()
      .addPlugin(JavaTestSuite.JAVA_PLUGIN_LOCATION)
      // we need html plugin to have "jsp" language
      .addPlugin(MavenLocation.of("org.sonarsource.html", "sonar-html-plugin", "DEV"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-jsp.xml"))
      .activateLicense();
    orchestratorBuilder.addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")));
    return orchestratorBuilder.build();
  }

  @Test
  public void should_transpile_jsp() throws Exception {
    if (isCommunityEditionTestsOnly()) {
      return;
    }

    MavenBuild build = TestUtils.createMavenBuild().setPom(TestUtils.projectPom(PROJECT))
      .setCleanPackageSonarGoals()
      .setDebugLogs(true)
      .setProperty("sonar.scm.disabled", "true");
    TestUtils.provisionProject(ENTERPRISE_ORCHESTRATOR_OR_NULL, "org.sonarsource.it.projects:" + PROJECT, PROJECT, "java", "jsp");
    BuildResult buildResult = ENTERPRISE_ORCHESTRATOR_OR_NULL.executeBuild(build);

    Path visitTest = TestUtils.projectDir(PROJECT).toPath().resolve("target/sonar/visit.txt");
    List<String> visitTestLines = Files.readAllLines(visitTest);
    Path sourceMapTest = TestUtils.projectDir(PROJECT).toPath().resolve("target/sonar/JspCodeCheck.txt");
    assertThat(visitTestLines).containsExactlyInAnyOrder("GreetingServlet extends javax.servlet.http.HttpServlet",
      "org.apache.jsp.views.greeting_jsp extends org.apache.jasper.runtime.HttpJspBase",
      "org.apache.jsp.index_jsp extends org.apache.jasper.runtime.HttpJspBase",
      "org.apache.jsp.views.include_jsp extends org.apache.jasper.runtime.HttpJspBase",
      "org.apache.jsp.views.test_005finclude_jsp extends org.apache.jasper.runtime.HttpJspBase"
    );
    List<String> actual = Files.readAllLines(sourceMapTest);
    assertThat(actual).containsExactlyInAnyOrder("index.jsp 1:6",
      "include.jsp 3:3",
      "test_include.jsp 7:7");

    // size of the generated files varies depending on the environment line endings
    assertThat(extractTelemetryLogs(buildResult))
      .matches(patternWithLiteralDot("""
        Telemetry java.analysis.generated.success.size_chars: \\d{5}
        Telemetry java.analysis.generated.success.time_ms: \\d+
        Telemetry java.analysis.generated.success.type_error_count: 0
        Telemetry java.analysis.main.success.size_chars: 969
        Telemetry java.analysis.main.success.time_ms: \\d+
        Telemetry java.analysis.main.success.type_error_count: 0
        Telemetry java.analysis.test.success.size_chars: 20
        Telemetry java.analysis.test.success.time_ms: \\d+
        Telemetry java.analysis.test.success.type_error_count: 0
        Telemetry java.dependency.lombok: absent
        Telemetry java.dependency.spring-boot: absent
        Telemetry java.dependency.spring-web: absent
        Telemetry java.is_autoscan: false
        Telemetry java.language.version: 8
        Telemetry java.module_count: 1
        Telemetry java.scanner_app: ScannerMaven
        """));
  }
}
