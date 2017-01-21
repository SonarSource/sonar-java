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
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.Measure;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.component.ShowWsRequest;
import org.sonarqube.ws.client.measure.ComponentWsRequest;

import static com.sonar.orchestrator.container.Server.ADMIN_LOGIN;
import static com.sonar.orchestrator.container.Server.ADMIN_PASSWORD;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  JavaExtensionsTest.class,
  UnitTestsTest.class,
  JavaTest.class,
  JavaComplexityTest.class,
  SquidTest.class,
  Struts139Test.class,
  JavaClasspathTest.class,
  JaCoCoControllerTest.class,
  SuppressWarningTest.class,
  SonarLintTest.class
})
public class JavaTestSuite {

  static final FileLocation JAVA_PLUGIN_LOCATION = FileLocation.byWildcardMavenFilename(new File("../../../sonar-java-plugin/target"), "sonar-java-plugin-*.jar");

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .addPlugin(JAVA_PLUGIN_LOCATION)
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-extension.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-version-aware-visitor.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-dit.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-ignored-test.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-complexity.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-filtered-issues.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-using-aar-dep.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/java/SquidTest/squid-backup.xml"));
    orchestratorBuilder.addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")));
    ORCHESTRATOR = orchestratorBuilder.build();
  }

  public static String keyFor(String projectKey, String pkgDir, String cls) {
    return projectKey + ":src/main/java/" + pkgDir + cls;
  }

  public static boolean sonarqube_version_is_prior_to_6_2() {
    return !ORCHESTRATOR.getServer().version().isGreaterThanOrEquals("6.2");
  }

  @CheckForNull
  static Measure getMeasure(String componentKey, String metricKey) {
    WsMeasures.ComponentWsResponse response = TestUtils.newWsClient(ORCHESTRATOR).measures().component(new ComponentWsRequest()
      .setComponentKey(componentKey)
      .setMetricKeys(singletonList(metricKey)));
    List<Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }

  @CheckForNull
  static Map<String, Measure> getMeasures(String componentKey, String... metricKeys) {
    return TestUtils.newWsClient(ORCHESTRATOR).measures().component(new ComponentWsRequest()
      .setComponentKey(componentKey)
      .setMetricKeys(asList(metricKeys)))
      .getComponent().getMeasuresList()
      .stream()
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  @CheckForNull
  static Integer getMeasureAsInteger(String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : Integer.parseInt(measure.getValue());
  }

  @CheckForNull
  static Double getMeasureAsDouble(String componentKey, String metricKey) {
    Measure measure = getMeasure(componentKey, metricKey);
    return (measure == null) ? null : Double.parseDouble(measure.getValue());
  }

  static Component getComponent(String componentKey) {
    return TestUtils.newWsClient(ORCHESTRATOR).components().show(new ShowWsRequest().setKey(componentKey)).getComponent();
  }

}
