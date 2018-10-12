/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.plugins.java;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.CheckList;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * define built-in profile
 */
@SonarLintSide
public class JavaSonarWayProfile implements BuiltInQualityProfilesDefinition {

  private final SonarRuntime sonarRuntime;

  public JavaSonarWayProfile(SonarRuntime sonarRuntime) {
    this.sonarRuntime = sonarRuntime;
  }

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile sonarWay = context.createBuiltInQualityProfile("Sonar way", Java.KEY);
    sonarWay.activateRule("common-" + Java.KEY, "DuplicatedBlocks");
    Profile jsonProfile = readProfile();
    Map<String, String> keys = legacyKeys();
    for (String key : jsonProfile.ruleKeys) {
      if (shouldActivateRule(key)) {
        sonarWay.activateRule(CheckList.REPOSITORY_KEY, keys.get(key));
      }
    }

    SonarComponents.getSecurityRuleKeys().forEach(key -> sonarWay.activateRule(CheckList.REPOSITORY_KEY, key));

    sonarWay.done();
  }

  private boolean shouldActivateRule(String ruleKey) {
    JavaRulesDefinition.RuleMetadata ruleMetadata = JavaRulesDefinition.readRuleMetadata(ruleKey);
    if (ruleMetadata == null) {
      return true;
    }
    if (!ruleMetadata.isSecurityHotspot()) {
      return true;
    }
    return SecurityHotspots.securityHotspotsSupported(sonarRuntime);
  }


  private static Map<String, String> legacyKeys() {
    Map<String, String> result = new HashMap<>();
    for (Class checkClass : CheckList.getChecks()) {
      org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(checkClass, org.sonar.check.Rule.class);
      String key = ruleAnnotation.key();
      org.sonar.java.RspecKey rspecKeyAnnotation = AnnotationUtils.getAnnotation(checkClass, org.sonar.java.RspecKey.class);
      String rspecKey = key;
      if(rspecKeyAnnotation != null) {
        rspecKey = rspecKeyAnnotation.value();
      }
      result.put(rspecKey, key);
    }
    return result;
  }

  static Profile readProfile() {
    URL resource = JavaSonarWayProfile.class.getResource("/org/sonar/l10n/java/rules/squid/Sonar_way_profile.json");
    return new Gson().fromJson(readResource(resource), Profile.class);
  }

  private static String readResource(URL resource) {
    try {
      return Resources.toString(resource, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read: " + resource, e);
    }
  }

  static class Profile {
    String name;
    List<String> ruleKeys;
  }

}
