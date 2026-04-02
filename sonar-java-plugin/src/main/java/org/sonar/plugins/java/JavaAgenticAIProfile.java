/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import javax.annotation.Nullable;
import org.sonar.plugins.java.api.ProfileRegistrar;
import org.sonarsource.api.sonarlint.SonarLintSide;

@SonarLintSide
public class JavaAgenticAIProfile extends BuiltInJavaQualityProfile {
  static final String PROFILE_NAME = "Sonar agentic AI";

  public JavaAgenticAIProfile() {
    this(null);
  }

  public JavaAgenticAIProfile(@Nullable ProfileRegistrar[] profileRegistrars) {
    super(profileRegistrars);
  }

  @Override
  String getProfileName() {
    return PROFILE_NAME;
  }

  @Override
  String getPathToJsonProfile() {
    return "/org/sonar/l10n/java/rules/java/Sonar_agentic_AI_profile.json";
  }

  @Override
  boolean isDefault() {
    return false;
  }
}
