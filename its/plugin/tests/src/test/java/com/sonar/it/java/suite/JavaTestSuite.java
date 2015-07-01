/*
 * Java :: IT :: Plugin :: Tests
 * Copyright (C) 2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.it.java.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  JavaExtensionsTest.class,
  UnitTestsTest.class,
  JavaTest.class,
  JavaComplexityTest.class,
  SquidTest.class,
  Struts139Test.class,
  Struts139WithoutAccessorsTest.class,
  JavaClasspathTest.class,
  JaCoCoTest.class
})
public class JavaTestSuite {

  private static final String PLUGIN_KEY = "java";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      .addPlugin(PLUGIN_KEY)
      .setMainPluginKey(PLUGIN_KEY)
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-extension.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-dit.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-ignored-test.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-complexity.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/java/SquidTest/squid-backup.xml"));

    if (Version.create(Orchestrator.builderEnv().getPluginVersion("java")).isGreaterThanOrEquals("3.1")) {
      orchestratorBuilder.addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")));
    }
    ORCHESTRATOR = orchestratorBuilder.build();
  }

  private static boolean is_after_plugin(String version) {
    return ORCHESTRATOR.getConfiguration().getPluginVersion(PLUGIN_KEY).isGreaterThanOrEquals(version);
  }

  public static boolean sonarqube_version_is_prior_to_5_0() {
    return !ORCHESTRATOR.getServer().version().isGreaterThanOrEquals("5.0");
  }
  public static boolean sonarqube_version_is_prior_to_5_2() {
    return !ORCHESTRATOR.getServer().version().isGreaterThanOrEquals("5.2");
  }

  public static String keyFor(String projectKey, String pkgDir, String cls) {
    return projectKey + ":src/main/java/" + pkgDir + cls;
  }

  public static boolean isAtLeastPlugin3_4() {
    return is_after_plugin("3.4");
  }
}
