/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.it.java.suite;

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheEnabledTest {
  @Rule  public OrchestratorRule orchestrator = initServer();

  @Test
  public void test_cache_is_enabled() {
    SonarScanner build = TestUtils.createSonarScanner()
      .setProjectDir(TestUtils.projectDir("java-tutorial"))
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
    SonarScanner build = TestUtils.createSonarScanner()
      .setProjectDir(TestUtils.projectDir("java-tutorial"))
      .setProperty("sonar.projectKey", "org.sonarsource.it.projects:java-tutorial")
      .setProperty("sonar.projectName", "java-tutorial")
      .setProperty("sonar.sources", "src/main/java")
      .setProperty("sonar.java.binaries", "target/classes")
      .setProperty("sonar.analysisCache.enabled", "false");

    BuildResult buildResult = orchestrator.executeBuild(build);

    assertThat(buildResult.getLogs()).contains("Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.");
  }




  private static OrchestratorRule initServer() {
    return OrchestratorRule.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
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
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-suppress-warnings-pmd.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-depends-on-jdk-types.xml"))
      .build();
  }
}
