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

import com.google.common.collect.Iterables;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

  public static List<Issue> issuesForComponent(Orchestrator orchestrator, String componentKey) {
    return newWsClient(orchestrator)
      .issues()
      .search(new SearchRequest().setComponentKeys(Collections.singletonList(componentKey)))
      .getIssuesList();
  }

  static WsClient newWsClient(Orchestrator orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .build());
  }

  static WsClient newAdminWsClient(Orchestrator orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .credentials(ADMIN_LOGIN, ADMIN_PASSWORD)
      .url(orchestrator.getServer().getUrl())
      .build());
  }

  public static void provisionProject(Orchestrator ORCHESTRATOR, String projectKey, String projectName, String languageKey, String profileName) {
    Server server = ORCHESTRATOR.getServer();
    server.provisionProject(projectKey, projectName);
    server.associateProjectToQualityProfile(projectKey, languageKey, profileName);
  }
}
