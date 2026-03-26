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
import org.sonar.plugins.java.api.ProfileRegistrar;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * define built-in profile
 */
@SonarLintSide
public class JavaSonarWayProfile extends BuiltInJavaQualityProfile {
  static final String SONAR_WAY_PATH = "/org/sonar/l10n/java/rules/java/Sonar_way_profile.json";

  /**
   * Constructor used by Pico container (SC) when no ProfileRegistrar are available
   */
  public JavaSonarWayProfile() {
    this(null);
  }

  public JavaSonarWayProfile(@Nullable ProfileRegistrar[] profileRegistrars) {
    super(profileRegistrars);
  }

  @Override
  String getProfileName() {
    return "Sonar way";
  }

  @Override
  String getPathToJsonProfile() {
    return SONAR_WAY_PATH;
  }

  @Override
  boolean isDefault() {
    return true;
  }

  public static Set<RuleKey> sonarJavaSonarWayRuleKeys() {
    return loadRuleKeys(SONAR_WAY_PATH);
  }
}
