/*
 * SonarQube Java
 * Copyright (C) 2013-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;

import static org.fest.assertions.Assertions.assertThat;

public class JaCoCoControllerTest {


  @ClassRule
  public static final Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;


  @BeforeClass
  public static void analyzeProject() {
    orchestrator.resetData();
  }

  @Test
  public void test_error_thrown() {
    SonarClient sonarClient = orchestrator.getServer().adminWsClient();
    String json = sonarClient.get("api/updatecenter/installed_plugins");
    Plugin[] plugins = new Gson().fromJson(json, Plugin[].class);
    String javaVersion = "";
    for (Plugin plugin : plugins) {
      if(plugin.key.equals("java")) {
        javaVersion = plugin.version;
        break;
      }
    }
    assertThat(javaVersion).isNotEmpty();
    // We build the project with JunitListeners to get coverage but with no jacoco agent to let JaCoCoController throw NoClassDefFoundError
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("coverage_error"))
      .setProperty("skipTests", "false")
      .setProperty("javaPluginVersion", javaVersion)
      .setGoals("clean", "package");
    BuildResult buildResult = orchestrator.executeBuildQuietly(build);
    assertThat(buildResult.isSuccess()).isFalse();
    assertThat(buildResult.getLogs()).contains("JacocoControllerError");
  }

  private static class Plugin {
    String key;
    String name;
    String version;
  }
}
