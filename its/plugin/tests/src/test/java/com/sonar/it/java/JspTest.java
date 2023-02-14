/*
 * SonarQube Java
 * Copyright (C) 2013-2023 SonarSource SA
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
package com.sonar.it.java;

import com.sonar.it.java.suite.JavaTestSuite;
import com.sonar.it.java.suite.TestUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JspTest {

  private static final String PROJECT = "servlet-jsp";

  public static boolean isCommunityEditionTestsOnly() {
    return "true".equals(System.getProperty("communityEditionTestsOnly"));
  }

  @ClassRule
  public static final Orchestrator ENTERPRISE_ORCHESTRATOR_OR_NULL = getEnterpriseOrchestratorOrNull();

  private static Orchestrator getEnterpriseOrchestratorOrNull() {
    if (isCommunityEditionTestsOnly()) {
      return null;
    }
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
      .setEdition(Edition.ENTERPRISE)
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

    MavenBuild build = MavenBuild.create(TestUtils.projectPom(PROJECT))
      .setCleanPackageSonarGoals()
      .setDebugLogs(true)
      .setProperty("sonar.scm.disabled", "true");
    TestUtils.provisionProject(ENTERPRISE_ORCHESTRATOR_OR_NULL, "org.sonarsource.it.projects:" + PROJECT, PROJECT, "java", "jsp");
    ENTERPRISE_ORCHESTRATOR_OR_NULL.executeBuild(build);

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
  }
}
