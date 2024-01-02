/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.ProfileRegistrar;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * define built-in profile
 */
@SonarLintSide
public class JavaSonarWayProfile implements BuiltInQualityProfilesDefinition {


  private static final Logger LOG = LoggerFactory.getLogger(JavaSonarWayProfile.class);

  static final String SECURITY_RULES_CLASS_NAME = "com.sonar.plugins.security.api.JavaRules";
  static final String DBD_RULES_CLASS_NAME = "com.sonarsource.plugins.dbd.api.JavaRules";
  static final String SECURITY_RULE_KEYS_METHOD_NAME = "getSecurityRuleKeys";
  static final String DBD_RULE_KEYS_METHOD_NAME = "getDataflowBugDetectionRuleKeys";
  static final String GET_REPOSITORY_KEY = "getRepositoryKey";
  static final String SECURITY_REPOSITORY_KEY = "javasecurity";
  static final String DBD_REPOSITORY_KEY = "javabugs";

  static final String SONAR_WAY_PATH = "/org/sonar/l10n/java/rules/java/Sonar_way_profile.json";

  private final ProfileRegistrar[] profileRegistrars;

  /**
   * Constructor used by Pico container (SC) when no ProfileRegistrar are available
   */
  public JavaSonarWayProfile() {
    this(null);
  }

  public JavaSonarWayProfile(@Nullable ProfileRegistrar[] profileRegistrars) {
    this.profileRegistrars = profileRegistrars;
  }

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile sonarWay = context.createBuiltInQualityProfile("Sonar way", Java.KEY);
    Set<RuleKey> ruleKeys = new HashSet<>(sonarJavaSonarWayRuleKeys());
    if (profileRegistrars != null) {
      for (ProfileRegistrar profileRegistrar : profileRegistrars) {
        profileRegistrar.register(ruleKeys::addAll);
      }
    }

    // Former activation mechanism, it should be removed once sonar-security and sonar-dataflow-bug-detection
    // support the new mechanism:
    // <code> registrarContext.internal().registerDefaultQualityProfileRules(ruleKeys); </code>
    // For now, it still uses reflexion if rules are not yet defined
    if (ruleKeys.stream().noneMatch(rule -> SECURITY_REPOSITORY_KEY.equals(rule.repository()))) {
      ruleKeys.addAll(getSecurityRuleKeys());
    }
    if (ruleKeys.stream().noneMatch(rule -> DBD_REPOSITORY_KEY.equals(rule.repository()))) {
      ruleKeys.addAll(getDataflowBugDetectionRuleKeys());
    }

    ruleKeys.forEach(ruleKey -> sonarWay.activateRule(ruleKey.repository(), ruleKey.rule()));
    sonarWay.done();
  }

  static Set<RuleKey> sonarJavaSonarWayRuleKeys() {
    return BuiltInQualityProfileJsonLoader.loadActiveKeysFromJsonProfile(SONAR_WAY_PATH).stream()
      .map(rule -> RuleKey.of(CheckList.REPOSITORY_KEY, rule))
      .collect(Collectors.toSet());
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
