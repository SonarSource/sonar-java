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
package com.sonar.it.java.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheEnabledTest {
  @Rule  public Orchestrator orchestrator = initServer();

  @Test
  public void test_cache_is_enabled() {
    SonarScanner build = SonarScanner.create(TestUtils.projectDir("java-tutorial"))
      .setProperty("sonar.projectKey", "org.sonarsource.it.projects:java-tutorial")
      .setProperty("sonar.projectName", "java-tutorial")
      .setProperty("sonar.sources", "src/main/java")
      .setProperty("sonar.java.binaries", "target/classes")
      .setProperty("sonar.analysisCache.enabled", "true");

    BuildResult buildResult = orchestrator.executeBuild(build);

    assertThat(buildResult.getLogs())
      .contains("Server-side caching is enabled. The Java analyzer was able to leverage cached data from previous analyses for 0 out of 8 files. These files will not be parsed.");
  }

  @Test
  public void test_cache_is_disabled() {
    SonarScanner build = SonarScanner.create(TestUtils.projectDir("java-tutorial"))
      .setProperty("sonar.projectKey", "org.sonarsource.it.projects:java-tutorial")
      .setProperty("sonar.projectName", "java-tutorial")
      .setProperty("sonar.sources", "src/main/java")
      .setProperty("sonar.java.binaries", "target/classes")
      .setProperty("sonar.analysisCache.enabled", "false");

    BuildResult buildResult = orchestrator.executeBuild(build);

    assertThat(buildResult.getLogs()).contains("Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.");
  }




  private static Orchestrator initServer() {
    return Orchestrator.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[9.4]"))
      .addPlugin(JavaTestSuite.JAVA_PLUGIN_LOCATION)
      // for support of custom rules
      .addPlugin(FileLocation.of(TestUtils.pluginJar("java-extension-plugin")))
      // for suppress-warnings tests
      .addPlugin(MavenLocation.of("org.sonarsource.pmd", "sonar-pmd-plugin", "3.2.1"))
      // profiles for each test projects
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-extension.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-tutorial.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-version-aware-visitor.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-dit.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-ignored-test.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-complexity.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-filtered-issues.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-using-aar-dep.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-package-info.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-package-info-annotations.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings-pmd.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-depends-on-jdk-types.xml"))
      .build();
  }
}
