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
      result[i] = WildcardPattern.create(StringUtils.trim(p[i]), ".");
    }
    return result;
  }

}
