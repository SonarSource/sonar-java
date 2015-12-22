/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks.maven.helpers;

import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.maven2.Dependency;

import javax.annotation.Nullable;

import java.util.regex.Pattern;

public abstract class MavenDependencyAbstractMatcher {

  /**
   * Checks that a dependency matches the matcher.
   * @param dependency The dependency to check
   * @return true if the dependency matches the matcher, false otherwise
   */
  public abstract boolean matches(Dependency dependency);

  /**
   * Check if a pattern should be considered as wild card. Default wild card is "*".
   * @param pattern the pattern provided
   * @return true if the pattern should be considered as wild card
   */
  public static boolean isWildCard(String pattern) {
    return pattern.isEmpty() || "*".equals(pattern);
  }

  /**
   * Check that the provided attribute (which can be null) matches the pattern (which can be null too).
   * @param attribute The attribute of the dependency (groupId, artifactId, version, ...)
   * @param pattern The pattern that it should match
   * @return true if the attribute matches, or if there is no pattern to check. False if there is no attribute or if the pattern does not match.
   */
  public static boolean attributeMatchesPattern(@Nullable LocatedAttribute attribute, @Nullable Pattern pattern) {
    if (attribute == null) {
      return false;
    }
    if (pattern == null) {
      return true;
    }
    return pattern.matcher(attribute.getValue()).matches();
  }

  public static MavenDependencyAbstractMatcher alwaysMatchingMatcher() {
    return new MavenDependencyAbstractMatcher() {
      @Override
      public boolean matches(Dependency dependency) {
        return true;
      }
    };
  }

  public static Pattern compileRegex(String regex) {
    try {
      return Pattern.compile(regex, Pattern.DOTALL);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unable to compile the regular expression: " + regex, e);
    }
  }

}
