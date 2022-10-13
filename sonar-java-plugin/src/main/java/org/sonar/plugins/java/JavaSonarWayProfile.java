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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.CheckList;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * define built-in profile
 */
@SonarLintSide
public class JavaSonarWayProfile implements BuiltInQualityProfilesDefinition {


  private static final Logger LOG = Loggers.get(JavaSonarWayProfile.class);

  static final String SECURITY_RULES_CLASS_NAME = "com.sonar.plugins.security.api.JavaRules";
  static final String DBD_RULES_CLASS_NAME = "com.sonarsource.plugins.dbd.api.JavaRules";
  static final String SECURITY_RULE_KEYS_METHOD_NAME = "getSecurityRuleKeys";
  static final String DBD_RULE_KEYS_METHOD_NAME = "getDataflowBugDetectionRuleKeys";
  static final String GET_REPOSITORY_KEY = "getRepositoryKey";

  static final String SONAR_WAY_PATH = "/org/sonar/l10n/java/rules/java/Sonar_way_profile.json";


  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile sonarWay = context.createBuiltInQualityProfile("Sonar way", Java.KEY);

    BuiltInQualityProfileJsonLoader.load(sonarWay, CheckList.REPOSITORY_KEY, SONAR_WAY_PATH);

    getSecurityRuleKeys().forEach(key -> sonarWay.activateRule(key.repository(), key.rule()));
    getDataflowBugDetectionRuleKeys().forEach(key -> sonarWay.activateRule(key.repository(), key.rule()));
    sonarWay.done();
  }

  static Set<String> ruleKeys() {
    return BuiltInQualityProfileJsonLoader.loadActiveKeysFromJsonProfile(SONAR_WAY_PATH);
  }

  @VisibleForTesting
  static Set<RuleKey> getSecurityRuleKeys() {
    return getExternalRuleKeys(SECURITY_RULES_CLASS_NAME, SECURITY_RULE_KEYS_METHOD_NAME, "security");
  }

  @VisibleForTesting
  static Set<RuleKey> getDataflowBugDetectionRuleKeys() {
    return getExternalRuleKeys(DBD_RULES_CLASS_NAME, DBD_RULE_KEYS_METHOD_NAME, "dataflow bug detection");
  }

  @SuppressWarnings("unchecked")
  @VisibleForTesting
  static Set<RuleKey> getExternalRuleKeys(String className, String ruleKeysMethod, String rulesCategory) {
    try {
      Class<?> javaRulesClass = Class.forName(className);
      Method getRuleKeysMethod = javaRulesClass.getMethod(ruleKeysMethod);
      Set<String> ruleKeys = (Set<String>) getRuleKeysMethod.invoke(null);
      Method getRepositoryKeyMethod = javaRulesClass.getMethod(GET_REPOSITORY_KEY);
      String repositoryKey = (String) getRepositoryKeyMethod.invoke(null);
      return ruleKeys.stream().map(k -> RuleKey.of(repositoryKey, k)).collect(Collectors.toSet());
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      LOG.debug(String.format("[%s], no %s rules added to Sonar way java profile: %s", e.getClass().getSimpleName(), rulesCategory, e.getMessage()));
    }
    return new HashSet<>();
  }
}
