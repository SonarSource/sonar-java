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
package org.sonar.java.se.plugin;

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
public class JavaSESonarWayProfile implements BuiltInQualityProfilesDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(JavaSESonarWayProfile.class);

  static final String SONAR_WAY_PATH = "/org/sonar/l10n/java/rules/javase/Sonar_way_profile.json";

  private final ProfileRegistrar[] profileRegistrars;

  /**
   * Constructor used by Pico container (SC) when no ProfileRegistrar are available
   */
  public JavaSESonarWayProfile() {
    this(null);
  }

  public JavaSESonarWayProfile(@Nullable ProfileRegistrar[] profileRegistrars) {
    this.profileRegistrars = profileRegistrars;
  }

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile sonarWay = context.createBuiltInQualityProfile("Sonar way", "java");
    Set<RuleKey> ruleKeys = new HashSet<>(sonarJavaSonarWayRuleKeys());
    if (profileRegistrars != null) {
      for (ProfileRegistrar profileRegistrar : profileRegistrars) {
        profileRegistrar.register(ruleKeys::addAll);
      }
    }

    ruleKeys.forEach(ruleKey -> sonarWay.activateRule(ruleKey.repository(), ruleKey.rule()));
    sonarWay.done();
  }

  static Set<RuleKey> sonarJavaSonarWayRuleKeys() {
    return BuiltInQualityProfileJsonLoader.loadActiveKeysFromJsonProfile(SONAR_WAY_PATH).stream()
      .map(rule -> RuleKey.of(JavaSERulesDefinition.REPOSITORY_KEY, rule))
      .collect(Collectors.toSet());
  }

}
