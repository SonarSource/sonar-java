/*
 * SonarQube Java
 * Copyright (C) 2013-2020 SonarSource SA
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

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[7.9]"))
      .setEdition(Edition.ENTERPRISE)
      .addPlugin(JavaTestSuite.JAVA_PLUGIN_LOCATION)
      // we need html plugin to have "jsp" language
      .addPlugin(MavenLocation.of("org.sonarsource.html", "sonar-html-plugin", "DEV"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-jsp.xml"))
      .activateLicense();
    orchestratorBuilder.addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")));
    ORCHESTRATOR = orchestratorBuilder.build();
  }

  @Test
  public void should_transpile_jsp() throws Exception {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom(PROJECT))
      .setCleanPackageSonarGoals()
      .setDebugLogs(true)
      .setProperty("sonar.scm.disabled", "true");
    TestUtils.provisionProject(ORCHESTRATOR, "org.sonarsource.it.projects:" + PROJECT, PROJECT, "java", "jsp");
    ORCHESTRATOR.executeBuild(build);

    Path visitTest = TestUtils.projectDir(PROJECT).toPath().resolve("target/sonar/visit.txt");
    List<String> lines = Files.readAllLines(visitTest);
    assertThat(lines).containsExactlyInAnyOrder("GreetingServlet", "greeting_jsp", "index_jsp");

    Path sourceMapTest = TestUtils.projectDir(PROJECT).toPath().resolve("target/sonar/GeneratedCodeCheck.txt");
    assertThat(sourceMapTest).hasContent("index.jsp 1:6");
  }
}
