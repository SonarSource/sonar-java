/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.CheckList;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * define built-in profile
 */
@SonarLintSide
public class JavaSonarWayProfile implements BuiltInQualityProfilesDefinition {

  private static final Logger LOG = Loggers.get(JavaSonarWayProfile.class);

  static final String SECURITY_RULES_CLASS_NAME = "com.sonar.plugins.security.api.JavaRules";
  static final String DBD_RULES_CLASS_NAME = "com.sonarsource.plugins.dbd.api.JavaRules";
  static final String DBD_RULE_KEYS_METHOD_NAME = "getDataflowBugDetectionRuleKeys";
  static final String GET_REPOSITORY_KEY = "getRepositoryKey";


  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile sonarWay = context.createBuiltInQualityProfile("Sonar way", Java.KEY);
    sonarWay.activateRule("common-" + Java.KEY, "DuplicatedBlocks");
    Profile jsonProfile = readProfile();
    Map<String, String> keys = legacyKeys();
    for (String key : jsonProfile.ruleKeys) {
      sonarWay.activateRule(CheckList.REPOSITORY_KEY, keys.get(key));
    }

    getSecurityRuleKeys(isSonarSecurityBefore78()).forEach(key -> sonarWay.activateRule(key.repository(), key.rule()));
    getDataflowBugDetectionRuleKeys().forEach(key -> sonarWay.activateRule(key.repository(), key.rule()));
    sonarWay.done();
  }

  private static Map<String, String> legacyKeys() {
    Map<String, String> result = new HashMap<>();
    for (Class<?> checkClass : CheckList.getChecks()) {
      org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(checkClass, org.sonar.check.Rule.class);
      String key = ruleAnnotation.key();
      org.sonar.java.RspecKey rspecKeyAnnotation = AnnotationUtils.getAnnotation(checkClass, org.sonar.java.RspecKey.class);
      String rspecKey = key;
      if (rspecKeyAnnotation != null) {
        rspecKey = rspecKeyAnnotation.value();
      }
      result.put(rspecKey, key);
    }
    return result;
  }

  static Profile readProfile() {
    URL resource = JavaSonarWayProfile.class.getResource("/org/sonar/l10n/java/rules/java/Sonar_way_profile.json");
    return new Gson().fromJson(readResource(resource), Profile.class);
  }

  private static String readResource(URL resource) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read: " + resource, e);
    }
  }

  @VisibleForTesting
  static Set<RuleKey> getSecurityRuleKeys(boolean sonarSecurityBefore78) {
    String ruleKeysMethod = sonarSecurityBefore78 ? "getRuleKeys" : "getSecurityRuleKeys";
    return getExternalRuleKeys(SECURITY_RULES_CLASS_NAME, ruleKeysMethod, "security", sonarSecurityBefore78);
  }

  @VisibleForTesting
  static Set<RuleKey> getDataflowBugDetectionRuleKeys() {
    return getExternalRuleKeys(DBD_RULES_CLASS_NAME, DBD_RULE_KEYS_METHOD_NAME, "dataflow bug detection", false);
  }

  @SuppressWarnings("unchecked")
  @VisibleForTesting
  static Set<RuleKey> getExternalRuleKeys(String className, String ruleKeysMethod, String rulesCategory, boolean sonarSecurityBefore78) {
    try {
      Class<?> javaRulesClass = Class.forName(className);
      Method getRuleKeysMethod = javaRulesClass.getMethod(ruleKeysMethod);
      Set<String> ruleKeys = (Set<String>) getRuleKeysMethod.invoke(null);
      Method getRepositoryKeyMethod = javaRulesClass.getMethod(GET_REPOSITORY_KEY);
      String repositoryKey;
      if (sonarSecurityBefore78) {
        repositoryKey = CheckList.REPOSITORY_KEY;
      } else {
        repositoryKey = (String) getRepositoryKeyMethod.invoke(null);
      }
      return ruleKeys.stream().map(k -> RuleKey.of(repositoryKey, k)).collect(Collectors.toSet());
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      LOG.debug(String.format("[%s], no %s rules added to Sonar way java profile: %s", e.getClass().getSimpleName(), rulesCategory, e.getMessage()));
    }
    return new HashSet<>();
  }

  private static boolean isSonarSecurityBefore78() {
    try {
      Class<?> javaRulesClass = Class.forName(SECURITY_RULES_CLASS_NAME);
      javaRulesClass.getMethod(GET_REPOSITORY_KEY);
      return false;

    } catch (NoSuchMethodException | ClassNotFoundException e) {
      return true;
    }
  }

  static class Profile {
    String name;
    Set<String> ruleKeys;
  }

}
