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
package org.sonar.java.it;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.http.HttpResponse;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileGenerator {
  private static final String LANGUAGE = "java";
  private static final String REPOSITORY_KEY = "squid";
  // note that 500 is the maximum allowed by SQ rules/api/search
  private static final int NUMBER_RULES_BY_PAGE = 500;

  private static final Logger LOG = LoggerFactory.getLogger(ProfileGenerator.class);
  private static final Gson GSON = new Gson();

  static void generate(Orchestrator orchestrator, ImmutableMap<String, ImmutableMap<String, String>> rulesParameters,
    Set<String> excluded, Set<String> subsetOfEnabledRules, Set<String> activatedRuleKeys) {
    try {
      LOG.info("Generating profile containing all the rules");
      StringBuilder sb = new StringBuilder()
        .append("<profile>")
        .append("<name>rules</name>")
        .append("<language>").append(LANGUAGE).append("</language>")
        .append("<rules>");

      for (String key : getRuleKeys(orchestrator)) {
        if (excluded.contains(key) || (!subsetOfEnabledRules.isEmpty() && !subsetOfEnabledRules.contains(key))) {
          continue;
        }
        activatedRuleKeys.add(key);
        sb.append("<rule>")
          .append("<repositoryKey>").append(REPOSITORY_KEY).append("</repositoryKey>")
          .append("<key>").append(key).append("</key>")
          .append("<priority>INFO</priority>");
        if (rulesParameters.containsKey(key)) {
          sb.append("<parameters>");
          for (Map.Entry<String, String> parameter : rulesParameters.get(key).entrySet()) {
            sb.append("<parameter>")
              .append("<key>").append(parameter.getKey()).append("</key>")
              .append("<value>").append(parameter.getValue()).append("</value>")
              .append("</parameter>");
          }
          sb.append("</parameters>");
        }
        sb.append("</rule>");
      }

      sb.append("</rules>")
        .append("</profile>");

      File file = File.createTempFile("profile", ".xml");
      Files.write(sb, file, StandardCharsets.UTF_8);
      LOG.info("Restoring profile to SonarQube");
      orchestrator.getServer().restoreProfile(FileLocation.of(file));
      file.delete();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private static List<String> getRuleKeys(Orchestrator orchestrator) {
    List<String> ruleKeys = new ArrayList<>();
    // pages are 1-based
    int currentPage = 1;

    Double totalNumberRules;
    int collectedRulesNumber;
    do {
      HttpResponse response = orchestrator.getServer()
        .newHttpCall("/api/rules/search")
        .setMethod(HttpMethod.GET)
        .setParam("languages", LANGUAGE)
        .setParam("repositories", REPOSITORY_KEY)
        .setParam("p", Integer.toString(currentPage))
        .setParam("ps", Integer.toString(NUMBER_RULES_BY_PAGE)).execute();

      @SuppressWarnings("unchecked")
      Map<Object, Object> jsonObject = GSON.fromJson(response.getBodyAsString(), Map.class);

      @SuppressWarnings("unchecked")
      List<Map<Object, Object>> jsonRules = (List<Map<Object, Object>>) jsonObject.get("rules");

      jsonRules.stream()
        .map(jsonRule -> (String) jsonRule.get("key"))
        .map(key -> key.split(":")[1])
        .forEach(ruleKeys::add);

      // update number of rules
      collectedRulesNumber = ruleKeys.size();
      totalNumberRules = (Double) jsonObject.get("total");
      LOG.info("Collected rule keys: {} / {}", collectedRulesNumber, totalNumberRules);
      // prepare for next page
      currentPage++;
    } while (collectedRulesNumber != totalNumberRules);

    return ruleKeys;
  }
}
