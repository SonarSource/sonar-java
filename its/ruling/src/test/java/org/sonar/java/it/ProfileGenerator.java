/*
 * SonarQube Java
 * Copyright (C) 2013-2017 SonarSource SA
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
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.jsonsimple.JSONValue;

public class ProfileGenerator {

  static void generate(Orchestrator orchestrator, String language, String repositoryKey, ImmutableMap<String, ImmutableMap<String, String>> rulesParameters,
    Set<String> excluded, Set<String> subsetOfEnabledRules, Set<String> activatedRuleKeys) {
    try {
      StringBuilder sb = new StringBuilder()
        .append("<profile>")
        .append("<name>rules</name>")
        .append("<language>").append(language).append("</language>")
        .append("<alerts>")
        .append("<alert>")
        .append("<metric>blocker_violations</metric>")
        .append("<operator>&gt;</operator>")
        .append("<warning></warning>")
        .append("<error>0</error>")
        .append("</alert>")
        .append("<alert>")
        .append("<metric>info_violations</metric>")
        .append("<operator>&gt;</operator>")
        .append("<warning></warning>")
        .append("<error>0</error>")
        .append("</alert>")
        .append("</alerts>")
        .append("<rules>");

      List<String> ruleKeys = Lists.newArrayList();
      String json = new HttpRequestFactory(orchestrator.getServer().getUrl())
        .get("/api/rules/search", ImmutableMap.<String, Object>of("languages", language, "repositories", repositoryKey, "ps", "1000"));
      @SuppressWarnings("unchecked")
      List<Map> jsonRules = (List<Map>) ((Map) JSONValue.parse(json)).get("rules");
      for (Map jsonRule : jsonRules) {
        String key = (String) jsonRule.get("key");
        ruleKeys.add(key.split(":")[1]);
      }

      for (String key : ruleKeys) {
        if (excluded.contains(key) || (!subsetOfEnabledRules.isEmpty() && !subsetOfEnabledRules.contains(key))) {
          continue;
        }
        activatedRuleKeys.add(key);
        sb.append("<rule>")
          .append("<repositoryKey>").append(repositoryKey).append("</repositoryKey>")
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
}
