/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.GeneratedCheckList;
import org.sonar.plugins.java.api.ProfileRegistrar;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

class QualityProfileUtils {
  private QualityProfileUtils() {
    /* This utility class should not be instantiated */
  }

  static Set<RuleKey> registerRulesFromJson(
    String pathToJsonProfile,
    @Nullable ProfileRegistrar[] profileRegistrars) {

    Set<RuleKey> ruleKeys = new HashSet<>(loadRuleKeys(pathToJsonProfile));
    if (profileRegistrars != null) {
      for (ProfileRegistrar profileRegistrar : profileRegistrars) {
        profileRegistrar.register(ruleKeys::addAll);
      }
    }

    return ruleKeys;
  }

  static Set<RuleKey> loadRuleKeys(final String pathToJsonProfile) {
    return BuiltInQualityProfileJsonLoader.loadActiveKeysFromJsonProfile(pathToJsonProfile).stream()
      .map(rule -> RuleKey.of(GeneratedCheckList.REPOSITORY_KEY, rule))
      .collect(Collectors.toSet());
  }
}
