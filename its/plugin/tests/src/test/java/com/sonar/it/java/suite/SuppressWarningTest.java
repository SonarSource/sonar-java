/*
 * SonarQube Java
 * Copyright (C) 2013-2017 SonarSource SA
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
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.client.measure.ComponentWsRequest;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class SuppressWarningTest {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../../sonar-java-plugin/target"), "sonar-java-plugin-*.jar"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings.xml"));
    orchestratorBuilder.addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")));
    orchestratorBuilder.addPlugin(FileLocation.ofClasspath("/sonar-checkstyle-plugin-2.4.jar"));
    ORCHESTRATOR = orchestratorBuilder.build();
  }

  /**
   * SONARJAVA-19
   */
  @Test
  public void suppressWarnings_nosonar() throws Exception {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("suppress-warnings"))
      .setCleanSonarGoals()
      .setProperty("sonar.profile", "suppress-warnings");
    ORCHESTRATOR.executeBuild(build);

    assertThat(parseInt(getMeasure("org.example:example", "violations").getValue())).isEqualTo(3);
  }

  @CheckForNull
  static WsMeasures.Measure getMeasure(String componentKey, String metricKey) {
    WsMeasures.ComponentWsResponse response = TestUtils.newWsClient(ORCHESTRATOR).measures().component(new ComponentWsRequest()
      .setComponentKey(componentKey)
      .setMetricKeys(singletonList(metricKey)));
    List<WsMeasures.Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }

}
