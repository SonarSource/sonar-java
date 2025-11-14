/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.apache.commons.lang3.StringUtils;
import org.sonar.api.utils.WildcardPattern;

public final class PatternUtils {

  private PatternUtils() {
  }

  public static WildcardPattern[] createPatterns(String patterns) {
    String[] p = StringUtils.split(patterns, ',');
    WildcardPattern[] result = new WildcardPattern[p.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = WildcardPattern.create(p[i].trim(), ".");
    }
    return result;
  }

}
