/*
 * SonarQube Java
 * Copyright (C) 2013-2022 SonarSource SA
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
import com.sonar.orchestrator.build.MavenBuild;
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
  public static final Orchestrator ORCHESTRATOR = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void suppressWarnings_nosonar() throws Exception {
    String projectKey = "org.sonarsource.it.projects:suppress-warnings";
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("suppress-warnings"))
      .setProperty("sonar.scm.disabled", "true")
      .setCleanPackageSonarGoals();
    TestUtils.provisionProject(ORCHESTRATOR, projectKey, "suppress-warnings", "java", "suppress-warnings");

    ORCHESTRATOR.executeBuild(build);

    assertThat(parseInt(getMeasure(projectKey, "violations").getValue())).isEqualTo(5);
  }

  @Test
  public void suppressWarnings_also_supress_issues_of_other_analyzers() throws Exception {
    String projectKey = "org.sonarsource.it.projects:suppress-warnings-pmd";
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("suppress-warnings-pmd"))
      .setProperty("sonar.scm.disabled", "true")
      .setCleanPackageSonarGoals();
    TestUtils.provisionProject(ORCHESTRATOR, projectKey, "suppress-warnings-pmd", "java", "suppress-warnings-pmd");

    ORCHESTRATOR.executeBuild(build);

    assertThat(parseInt(getMeasure(projectKey, "violations").getValue())).isEqualTo(3);
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
