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
package org.sonar.java.semsitter;

import org.sonar.api.config.Configuration;

/**
 * The switch that turns the SemSitter frontend on for an analysis. The feature flag itself lives in SonarQube Cloud, which
 * sends its value to the analyzer as the context property {@value #ENABLED_PROPERTY}. The analyzer never evaluates a flag and
 * never reaches out to a flag service: an absent property means {@link #DEFAULT}.
 *
 * <p>{@value #ENABLED_PROPERTY} is a contract with SonarQube Cloud. Renaming it requires the same change there.
 */
public final class SemSitterFlag {

  public static final String ENABLED_PROPERTY = "sonar.java.internal.semsitter.enabled";
  static final boolean DEFAULT = true;

  private SemSitterFlag() {
  }

  public static boolean isEnabled(Configuration configuration) {
    return configuration.getBoolean(ENABLED_PROPERTY).orElse(DEFAULT);
  }
}
