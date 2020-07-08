/*
 * SonarQube Java
 * Copyright (C) 2013-2020 SonarSource SA
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

import com.google.common.io.Files;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.rules.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class SelfAnalysisTest {

  private static final String LANGUAGE = "java";
  private static final String REPOSITORY_KEY = "java";
  // note that 500 is the maximum allowed by SQ rules/api/search
  private static final int NUMBER_RULES_BY_PAGE = 500;

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void prepare_quality_profiles() throws Exception {
    enableAllNonTemplateRules();
  }

  /**
   * Verifies that we are not failing our own analysis with all the rules enabled.
   * If any change has been done on our side, it will prevent us to deploy a faulty version of SonarJava
   */
  @Test
  public void self_analysis() throws Exception {
    final String projectKey = "sonarjava-self-analysis";

    File pomFile = FileLocation.of("../../../pom.xml").getFile().getCanonicalFile();

    orchestrator.getServer().provisionProject(projectKey, "SonarJava Self-analysis");
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", "rules");

    MavenBuild build = MavenBuild.create().setPom(pomFile)
      .setGoals("package", "sonar:sonar")
      .addArgument("-DskipTests")
      .setProperty("sonar.projectKey", projectKey)
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.import_unknown_files", "true")
      .setProperty("sonar.skipPackageDesign", "true")
      .setProperty("sonar.java.xfile", "true")
      .setProperty("sonar.internal.analysis.failFast", "true");
    BuildResult buildResult = orchestrator.executeBuild(build);

    assertThat(buildResult.isSuccess()).isTrue();
  }

  private static final Logger LOG = LoggerFactory.getLogger(SelfAnalysisTest.class);

  private static void enableAllNonTemplateRules() throws Exception {
    LOG.info("Generating profile containing all non-template rules");
    StringBuilder sb = new StringBuilder()
      .append("<profile>")
      .append("<name>rules</name>")
      .append("<language>").append(LANGUAGE).append("</language>")
      .append("<rules>");

    for (String key : getRuleKeys(orchestrator)) {
      sb.append("<rule>")
        .append("<repositoryKey>").append(REPOSITORY_KEY).append("</repositoryKey>")
        .append("<key>").append(key).append("</key>")
        .append("<priority>INFO</priority>");
      sb.append("</rule>");
    }

    sb.append("</rules>")
      .append("</profile>");

    File file = File.createTempFile("profile", ".xml");
    Files.asCharSink(file, StandardCharsets.UTF_8).write(sb);
    LOG.info("Restoring profile to SonarQube");
    orchestrator.getServer().restoreProfile(FileLocation.of(file));
    file.delete();
  }

  private static List<String> getRuleKeys(Orchestrator orchestrator) {
    List<String> ruleKeys = new ArrayList<>();
    // pages are 1-based
    int currentPage = 1;

    long totalNumberRules;
    long collectedRulesNumber;

    do {
      Rules.SearchResponse searchResponse = newAdminWsClient(orchestrator).rules().search(new SearchRequest()
        .setLanguages(Collections.singletonList(LANGUAGE))
        .setRepositories(Collections.singletonList(REPOSITORY_KEY))
        .setP(Integer.toString(currentPage))
        .setPs(Integer.toString(NUMBER_RULES_BY_PAGE)));

      searchResponse.getRulesList().stream()
        .map(Rules.Rule::getKey)
        .map(key -> key.split(":")[1])
        .forEach(ruleKeys::add);

      // update number of rules
      collectedRulesNumber = ruleKeys.size();
      totalNumberRules = searchResponse.getTotal();
      LOG.info("Collected rule keys: {} / {}", collectedRulesNumber, totalNumberRules);
      // prepare for next page
      currentPage++;
    } while (collectedRulesNumber != totalNumberRules);

    return ruleKeys;
  }

  private static WsClient newAdminWsClient(Orchestrator orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .url(orchestrator.getServer().getUrl())
      .build());
  }

}
