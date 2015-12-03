/*
 * SonarQube Java
 * Copyright (C) 2013 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.collect.Iterables;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  JavaExtensionsTest.class,
  UnitTestsTest.class,
  JavaTest.class,
  JavaComplexityTest.class,
  SquidTest.class,
  Struts139Test.class,
  JavaClasspathTest.class,
})
public class JavaTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  static {
    OrchestratorBuilder orchestratorBuilder = Orchestrator.builderEnv()
      // workaround until https://jira.sonarsource.com/browse/PM-27 PM-28 are fixed
      .addPlugin(FileLocation.of(Iterables.getOnlyElement(Arrays.asList(new File("../../../sonar-java-plugin/target/").listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(".jar") && !name.endsWith("-sources.jar");
        }
      }))).getAbsolutePath()))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-extension.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-version-aware-visitor.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-dit.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-ignored-test.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-complexity.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/java/SquidTest/squid-backup.xml"));
      orchestratorBuilder.addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")));
    ORCHESTRATOR = orchestratorBuilder.build();
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

}
