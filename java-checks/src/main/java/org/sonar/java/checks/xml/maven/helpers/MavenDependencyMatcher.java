/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks.xml.maven.helpers;

import org.apache.commons.lang.StringUtils;
import org.sonar.maven.model.maven2.Dependency;

/**
 * Matchers targeting maven dependencies.
 */
public class MavenDependencyMatcher {
  private static final LocatedAttributeMatcher ALWAYS_MATCHING_MATCHER = LocatedAttributeMatcher.any();

  private final LocatedAttributeMatcher groupIdMatcher;
  private final LocatedAttributeMatcher artifactIdMatcher;
  private final LocatedAttributeMatcher versionMatcher;

  /**
   * Create a {@link MavenDependencyMatcher} for the given name pattern and optional version
   *
   * @param dependencyName the dependency pattern. Expected format: "<code>[groupId]:[artifactId]</code>",
   *  with following options for both [groupId] and [artifactId]:
   *   <ul>
   *     <li>Wildcard (i.e. "<code>*:myArtifactId</code>" or "<code>myGroupId:*</code>")</li>
   *     <li>Regular expression (i.e. "<code>org.sonar.*:*java</code>")</li>
   *   </ul>
   *
   * @param version the version. Expected formats:
   *   <ul>
   *     <li>Empty string (i.e. "")</li>
   *     <li>Wildcard (i.e. "*")</li>
   *     <li>Regular expression (i.e. "<code>1.3.*</code>")</li>
   *     <li>Dash-delimited range (without version qualifier). Examples: </li>
   *       <ul>
   *         <li>"<code>1.0-3.1</code>" : any version between 1.0 and 3.1</li>
   *         <li>"<code>1.0-*</code>" : any version after version 1.0</li>
   *         <li>"<code>*-3.1</code>" : any version before version 3.1</li>
   *      </ul>
   *   </ul>
   * @return the corresponding matcher
   */
  public MavenDependencyMatcher(String dependencyName, String version) {
    String[] name = dependencyName.split(":");
    if (name.length != 2) {
      throw new IllegalArgumentException(
        "Invalid dependency name. Should match '[groupId]:[artifactId]' use '*' as wildcard");
    }

    groupIdMatcher = getMatcherForPattern(name[0].trim());
    artifactIdMatcher = getMatcherForPattern(name[1].trim());
    versionMatcher = getMatcherForVersion(version);
  }

  private static LocatedAttributeMatcher getMatcherForPattern(String pattern) {
    return (StringUtils.isBlank(pattern) || isWildCard(pattern)) ? ALWAYS_MATCHING_MATCHER : new PatternMatcher(pattern);
  }

  private static boolean isWildCard(String pattern) {
    return "*".equals(pattern);
  }

  private static LocatedAttributeMatcher getMatcherForVersion(String version) {
    if (version.contains("-")) {
      String[] bounds = version.split("-");
      return new RangedVersionMatcher(bounds[0], bounds[1]);
    }
    return getMatcherForPattern(version);
  }

  public boolean matches(Dependency dependency) {
    return groupIdMatcher.test(dependency.getGroupId())
      && artifactIdMatcher.test(dependency.getArtifactId())
      && versionMatcher.test(dependency.getVersion());
  }
}
