/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.java.checks.CheckList;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replacement for org.sonar.plugins.squid.SonarWayProfile
 */
public class JavaSonarWayProfile extends ProfileDefinition {

  private final Gson gson = new Gson();

  @Override
  public RulesProfile createProfile(ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create("Sonar way", Java.KEY);
    URL resource = JavaRulesDefinition.class.getResource("/org/sonar/l10n/java/rules/squid/Sonar_way_profile.json");
    Profile jsonProfile = gson.fromJson(readResource(resource), Profile.class);
    Map<String, String> keys = legacyKeys();
    for (String key : jsonProfile.ruleKeys) {
      profile.activateRule(Rule.create(CheckList.REPOSITORY_KEY, keys.get(key)), null);
    }
    return profile;
  }

  private static String readResource(URL resource) {
    try {
      return Resources.toString(resource, Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read: " + resource, e);
    }
  }

  private static Map<String, String> legacyKeys() {
    Map<String, String> result = new HashMap<>();
    for (Class checkClass : CheckList.getChecks()) {
      org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(checkClass, org.sonar.check.Rule.class);
      String key = ruleAnnotation.key();
      org.sonar.java.RspecKey rspecKeyAnnotation = AnnotationUtils.getAnnotation(checkClass, org.sonar.java.RspecKey.class);
      if(rspecKeyAnnotation == null) {
        result.put(key, key);
      } else {
        result.put(rspecKeyAnnotation.value(), key);
      }
    }
    return result;
  }

  private static class Profile {
    String name;
    List<String> ruleKeys;
  }

}
