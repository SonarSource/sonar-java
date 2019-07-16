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
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.util.List;
import javax.annotation.CheckForNull;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.client.measures.ComponentRequest;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class SuppressWarningTest {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;
  public static final String PROJECT_KEY = "org.sonarsource.it.projects:example";

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[7.9]"))
      .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../../sonar-java-plugin/target"), "sonar-java-plugin-*.jar"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings.xml"));
    orchestratorBuilder.addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")));
    ORCHESTRATOR = orchestratorBuilder.build();
  }

  /**
   * SONARJAVA-19
   */
  @Test
  public void suppressWarnings_nosonar() throws Exception {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("suppress-warnings"))
      .setCleanSonarGoals()
      .setProperty("sonar.java.binaries", "target");
    TestUtils.provisionProject(ORCHESTRATOR, SuppressWarningTest.PROJECT_KEY,"suppress-warnings","java","suppress-warnings");

    ORCHESTRATOR.executeBuild(build);

    assertThat(parseInt(getMeasure(PROJECT_KEY, "violations").getValue())).isEqualTo(4);
  }

  @CheckForNull
  static Measures.Measure getMeasure(String componentKey, String metricKey) {
    Measures.ComponentWsResponse response = TestUtils.newWsClient(ORCHESTRATOR).measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(singletonList(metricKey)));
    List<Measures.Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }

}
