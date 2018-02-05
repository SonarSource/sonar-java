/*
 * SonarQube Java
 * Copyright (C) 2013-2018 SonarSource SA
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

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.jsonsimple.JSONValue;


public class ProfileGenerator {
  private static final String LANGUAGE = "java";
  private static final String REPOSITORY_KEY = "squid";

  static void generate(Orchestrator orchestrator, ImmutableMap<String, ImmutableMap<String, String>> rulesParameters,
    Set<String> excluded, Set<String> subsetOfEnabledRules, Set<String> activatedRuleKeys) {
    try {
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
      orchestrator.getServer().restoreProfile(FileLocation.of(file));
      file.delete();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private static List<String> getRuleKeys(Orchestrator orchestrator) {
    String json = new HttpRequestFactory(orchestrator.getServer().getUrl())
      .get("/api/rules/search", ImmutableMap.<String, Object>of("languages", LANGUAGE, "repositories", REPOSITORY_KEY, "ps", "500"));
    @SuppressWarnings("unchecked")
    Map<Object, Object> jsonObject = (Map<Object, Object>) JSONValue.parse(json);

    Preconditions.checkState((Long) jsonObject.get("total") < 500, "Only collect the 500 first rules. Requires pagination in case of more rules");

    @SuppressWarnings("unchecked")
    List<Map<Object, Object>> jsonRules = (List<Map<Object, Object>>) jsonObject.get("rules");

    return jsonRules.stream()
      .map(jsonRule -> (String) jsonRule.get("key"))
      .map(key -> key.split(":")[1])
      .collect(Collectors.toList());
  }
}
