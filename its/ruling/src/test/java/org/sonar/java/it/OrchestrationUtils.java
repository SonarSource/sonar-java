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
package org.sonar.java.it;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Fail;
import org.junit.BeforeClass;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.qualityprofiles.ActivateRuleRequest;
import org.sonarqube.ws.client.qualityprofiles.SearchRequest;
import org.sonarqube.ws.client.rules.CreateRequest;

public class OrchestrationUtils {
  private static final Logger LOG = LoggerFactory.getLogger(OrchestrationUtils.class);

  @BeforeClass
  public static void prepare_quality_profiles(Orchestrator orchestrator,
                                              ImmutableSet<String> subsetOfEnabledRules,
                                              TemporaryFolder temporaryFolder) throws Exception {
    ImmutableMap<String, ImmutableMap<String, String>> rulesParameters = ImmutableMap.<String, ImmutableMap<String, String>>builder()
      .put(
        "S1120",
        ImmutableMap.of("indentationLevel", "4"))
      .put(
        "S1451",
        ImmutableMap.of(
          "headerFormat",
          "\n/*\n" +
            " * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.\n" +
            " * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms."))
      .put("S5961", ImmutableMap.of("MaximumAssertionNumber", "50"))
      .build();
    ImmutableSet<String> disabledRules = ImmutableSet.of(
      "S1874",
      "CycleBetweenPackages",
      // disable because it generates too many issues, performance reasons
      "S1106"
    );
    Set<String> activatedRuleKeys = new HashSet<>();
    ProfileGenerator.generate(orchestrator, rulesParameters, disabledRules, subsetOfEnabledRules, activatedRuleKeys);
    instantiateTemplateRule(orchestrator, "S2253", "stringToCharArray", "className=\"java.lang.String\";methodName=\"toCharArray\"", activatedRuleKeys, subsetOfEnabledRules);
    instantiateTemplateRule(orchestrator,"S4011", "longDate", "className=\"java.util.Date\";argumentTypes=\"long\"", activatedRuleKeys, subsetOfEnabledRules);
    instantiateTemplateRule(orchestrator,"S124", "commentRegexTest", "regularExpression=\"(?i).*TODO\\(user\\).*\";message=\"bad user\"", activatedRuleKeys, subsetOfEnabledRules);
    instantiateTemplateRule(orchestrator,"S3546", "InstancesOfNewControllerClosedWithDone",
      "factoryMethod=\"org.sonar.api.server.ws.WebService$Context#createController\";closingMethod=\"org.sonar.api.server.ws.WebService$NewController#done\"", activatedRuleKeys, subsetOfEnabledRules);
    instantiateTemplateRule(orchestrator,"S3546", "JsonWriterNotClosed",
      "factoryMethod=\"org.sonar.api.server.ws.Response#newJsonWriter\";closingMethod=\"org.sonar.api.utils.text.JsonWriter#close\"", activatedRuleKeys, subsetOfEnabledRules);

    subsetOfEnabledRules.stream()
      .filter(ruleKey -> !activatedRuleKeys.contains(ruleKey))
      .forEach(ruleKey -> Fail.fail("Specified rule does not exist: " + ruleKey));

    prepareDumpOldFolder(subsetOfEnabledRules, temporaryFolder);
  }

  public static void instantiateTemplateRule(Orchestrator orchestrator,
                                             String ruleTemplateKey,
                                             String instantiationKey,
                                             String params,
                                             Set<String> activatedRuleKeys,
                                             ImmutableSet<String> subsetOfEnabledRules) {
    if (!subsetOfEnabledRules.isEmpty() && !subsetOfEnabledRules.contains(instantiationKey)) {
      return;
    }
    activatedRuleKeys.add(instantiationKey);
    newAdminWsClient(orchestrator)
      .rules()
      .create(new CreateRequest()
        .setName(instantiationKey)
        .setMarkdownDescription(instantiationKey)
        .setSeverity("INFO")
        .setStatus("READY")
        .setTemplateKey("java:" + ruleTemplateKey)
        .setCustomKey(instantiationKey)
        .setPreventReactivation("true")
        .setParams(Arrays.asList(("name=\"" + instantiationKey + "\";key=\"" + instantiationKey + "\";" +
          "markdown_description=\"" + instantiationKey + "\";" + params).split(";", 0))));

    String profileKey = newAdminWsClient(orchestrator).qualityprofiles()
      .search(new SearchRequest())
      .getProfilesList().stream()
      .filter(qualityProfile -> "rules".equals(qualityProfile.getName()))
      .map(Qualityprofiles.SearchWsResponse.QualityProfile::getKey)
      .findFirst()
      .orElse(null);

    if (StringUtils.isEmpty(profileKey)) {
      LOG.error("Could not retrieve profile key : Template rule " + ruleTemplateKey + " has not been activated");
    } else {
      String ruleKey = "java:" + instantiationKey;
      newAdminWsClient(orchestrator).qualityprofiles()
        .activateRule(new ActivateRuleRequest()
          .setKey(profileKey)
          .setRule(ruleKey)
          .setSeverity("INFO")
          .setParams(Collections.emptyList()));
      LOG.info(String.format("Successfully activated template rule '%s'", ruleKey));
    }
  }

  static WsClient newAdminWsClient(Orchestrator orchestrator) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .url(orchestrator.getServer().getUrl())
      .build());
  }

  private static void prepareDumpOldFolder(ImmutableSet<String> subsetOfEnabledRules, TemporaryFolder TMP_DUMP_OLD_FOLDER) throws Exception {
    Path allRulesFolder = Paths.get("src/test/resources");
    if (!subsetOfEnabledRules.isEmpty()) {
      final Path effectiveDumpOldFolder = TMP_DUMP_OLD_FOLDER.getRoot().toPath().toAbsolutePath();
      Files.list(allRulesFolder)
        .filter(p -> p.toFile().isDirectory())
        .forEach(srcProjectDir -> copyDumpSubset(srcProjectDir, effectiveDumpOldFolder.resolve(srcProjectDir.getFileName()), subsetOfEnabledRules));
    }
  }

  private static void copyDumpSubset(Path srcProjectDir, Path dstProjectDir, ImmutableSet<String> SUBSET_OF_ENABLED_RULES) {
    try {
      Files.createDirectory(dstProjectDir);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create directory: " + dstProjectDir.toString());
    }
    SUBSET_OF_ENABLED_RULES.stream()
      .map(ruleKey -> srcProjectDir.resolve("java-" + ruleKey + ".json"))
      .filter(p -> p.toFile().exists())
      .forEach(srcJsonFile -> copyFile(srcJsonFile, dstProjectDir));
  }

  private static void copyFile(Path source, Path targetDir) {
    try {
      Files.copy(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to copy file: " + source.toString());
    }
  }
}
