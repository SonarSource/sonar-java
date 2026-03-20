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
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.plugins.java.api.ProfileRegistrar;
import org.sonarsource.api.sonarlint.SonarLintSide;

@SonarLintSide
public class JavaAgenticWayProfile implements BuiltInQualityProfilesDefinition {
  private final ProfileRegistrar[] profileRegistrars;

  /**
   * Constructor used by Pico container (SC) when no ProfileRegistrar are available
   */
  public JavaAgenticWayProfile() {
    this(null);
  }

  public JavaAgenticWayProfile(@Nullable ProfileRegistrar[] profileRegistrars) {
    this.profileRegistrars = profileRegistrars;
  }

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile agenticWay = context.createBuiltInQualityProfile("AI Quality Profile", Java.KEY);
    Set<RuleKey> ruleKeys = QualityProfileUtils.registerRulesFromJson(
      "/org/sonar/l10n/java/rules/java/Agentic_way_profile.json",
      this.profileRegistrars
    );

    ruleKeys.forEach(ruleKey -> agenticWay.activateRule(ruleKey.repository(), ruleKey.rule()));
    agenticWay.done();
  }
}
