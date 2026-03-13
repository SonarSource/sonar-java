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

import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.java.GeneratedCheckList;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Agent Quality Profile - Built-in profile focused on security, reliability, and code complexity
 *
 * This profile activates rules that help maintain code quality for AI agents by focusing on:
 * - Security vulnerabilities and security hotspots
 * - Reliability issues (bugs)
 * - Code complexity metrics (cognitive complexity, cyclomatic complexity, etc.)
 */
@SonarLintSide
public class JavaAgentQualityProfile implements BuiltInQualityProfilesDefinition {

  static final String AGENT_PROFILE_PATH = "/org/sonar/l10n/java/rules/java/Agent_quality_profile.json";
  static final String PROFILE_NAME = "Agent Quality Profile";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile agentProfile = context.createBuiltInQualityProfile(PROFILE_NAME, Java.KEY);
    Set<RuleKey> ruleKeys = agentProfileRuleKeys();
    ruleKeys.forEach(ruleKey -> agentProfile.activateRule(ruleKey.repository(), ruleKey.rule()));
    agentProfile.done();
  }

  @VisibleForTesting
  static Set<RuleKey> agentProfileRuleKeys() {
    return BuiltInQualityProfileJsonLoader.loadActiveKeysFromJsonProfile(AGENT_PROFILE_PATH).stream()
      .map(rule -> RuleKey.of(GeneratedCheckList.REPOSITORY_KEY, rule))
      .collect(Collectors.toSet());
  }
}
