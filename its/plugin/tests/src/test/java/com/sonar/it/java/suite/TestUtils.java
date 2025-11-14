/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import com.google.common.collect.Iterables;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.issues.SearchRequest;

import static com.sonar.orchestrator.container.Server.ADMIN_LOGIN;
import static com.sonar.orchestrator.container.Server.ADMIN_PASSWORD;

public class TestUtils {
  private static final File home;

  static {
    File testResources = FileUtils.toFile(TestUtils.class.getResource("/TestUtils.txt"));
    home = testResources // home/tests/src/tests/resources
      .getParentFile() // home/tests/src/tests
      .getParentFile() // home/tests/src
      .getParentFile() // home/tests
      .getParentFile(); // home
  }

  private static final Pattern TELEMETRY_PATTERN = Pattern.compile("Telemetry java\\.[^\r\n]++");

  public static File homeDir() {
    return home;
  }

  public static File pluginJar(String artifactId) {
    return Iterables.getOnlyElement(Arrays.asList(new File(homeDir(), "plugins/" + artifactId + "/target/").listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".jar") && !name.endsWith("-sources.jar");
      }
    })));
  }

  public static File projectDir(String projectName) {
    return new File(homeDir(), "projects/" + projectName);
  }

  public static File projectPom(String projectName) {
    return new File(homeDir(), "projects/" + projectName + "/pom.xml");
  }

  public static List<Issue> issuesForComponent(OrchestratorRule orchestrator, String componentKey) {
    return newWsClient(orchestrator)
      .issues()
      .search(new SearchRequest().setComponentKeys(Collections.singletonList(componentKey)))
      .getIssuesList();
  }

  static WsClient newWsClient(OrchestratorRule orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .build());
  }

  static WsClient newAdminWsClient(OrchestratorRule orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .credentials(ADMIN_LOGIN, ADMIN_PASSWORD)
      .url(orchestrator.getServer().getUrl())
      .build());
  }

  public static void provisionProject(OrchestratorRule orchestrator, String projectKey, String projectName, String languageKey, String profileName) {
    Server server = orchestrator.getServer();
    server.provisionProject(projectKey, projectName);
    server.associateProjectToQualityProfile(projectKey, languageKey, profileName);
  }

  public static SonarScanner createSonarScanner() {
    return SonarScanner.create()
      .setProperty("sonar.scanner.skipJreProvisioning", "true");
  }

  public static MavenBuild createMavenBuild() {
    return MavenBuild.create()
      .setProperty("sonar.scanner.skipJreProvisioning", "true");
  }


  public static String extractTelemetryLogs(BuildResult buildResult) {
    var telemetryLogs = new StringBuilder();
    var telemetryMatcher = TELEMETRY_PATTERN.matcher(buildResult.getLogs());
    while (telemetryMatcher.find()) {
      telemetryLogs.append(telemetryMatcher.group()).append("\n");
    }
    return telemetryLogs.toString();
  }

  public static Pattern patternWithLiteralDot(String regex) {
    return Pattern.compile(regex.replace(".", "\\."));
  }

}
