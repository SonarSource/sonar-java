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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Matchers targeting names of dependencies.
 */
public class MavenDependencyMatcher extends MavenDependencyAbstractMatcher {
  private Pattern groupIdPattern;
  private Pattern artifactIdPattern;
  private MavenDependencyAbstractMatcher versionMatcher;

  /**
   * Build a list of dependency matchers, based on a comma-separated list of dependency name
   *
   * <br />
   *
   * Expected formats for each dependency:
   * <ul>
   *   <li><code>[groupId]:[artifactId]</code></li>
   *   <li><code>[groupId]:[artifactId]:[version]</code></li>
   * </ul>
   *
   * Details regarding format of <code>[groupId]</code> and <code>[artifactId]</code>:
   * <ul>
   *   <li>Wildcard "*" can be used instead of providing a [groupId] or [artifactId] (i.e. "<code>*:myArtifact</code>")</li>
   *   <li>Regular expression can be used to define [groupId] and/or [artifactId] (i.e. "<code>org.sonar.*:*java</code>")</li>
   *   <li>Multiple dependency matchers can be provided when separated by comma (i.e. "<code>*:log,a.b.c:*</code>")</li>
   * </ul>
   *
   * Details regarding supported formats of <code>[version]</code>:
   * <ul>
   *   <li>java regular expression can be used to define pattern: "<code>1.3.*</code>"</li>
   *   <li>dash-delimited range (without version qualifier). Examples: </li>
   *   <ul>
   *     <li>"<code>1.0-3.1</code>" : any version between 1.0 and 3.1</li>
   *     <li>"<code>1.0-*</code>" : any version after version 1.0</li>
   *     <li>"<code>*-3.1</code>" : any version before version 3.1</li>
   *   </ul>
   * </ul>
   *
   * @param commaSeparatedDependencies the comma-separated list of dependencies.
   * @return a list of corresponding matcher
   */
  public static List<MavenDependencyMatcher> fromString(String commaSeparatedDependencies) {
    String[] names = commaSeparatedDependencies.split(",");
    List<MavenDependencyMatcher> results = new LinkedList<>();
    for (String dependency : names) {
      results.add(getMatcher(dependency.trim()));
    }
    return results;
  }

  private static MavenDependencyMatcher getMatcher(String dependencyName) {
    MavenDependencyMatcher matcher = new MavenDependencyMatcher();
    String[] name = dependencyName.split(":");
    boolean groupIdAndArtifactId = name.length == 2;
    boolean groupIdAndArtifactIdAndVersion = name.length == 3;
    if (!(groupIdAndArtifactId || groupIdAndArtifactIdAndVersion)) {
      throw new IllegalArgumentException(
        "Invalid dependency name. Should match '[groupId]:[artifactId]' or '[groupId]:[artifactId]:[version]', use '*' as wildcard, version is optionnal");
    }
    String groupId = name[0].trim();
    if (!isWildCard(groupId)) {
      matcher.groupIdPattern = compileRegex(groupId);
    }
    String artifactId = name[1].trim();
    if (!isWildCard(artifactId)) {
      matcher.artifactIdPattern = compileRegex(artifactId);
    }
    if (groupIdAndArtifactIdAndVersion) {
      String version = name[2].trim();
      matcher.versionMatcher = MavenDependencyVersionMatcher.fromString(version);
    } else {
      matcher.versionMatcher = MavenDependencyAbstractMatcher.alwaysMatchingMatcher();
    }

    return matcher;
  }

  @Override
  public boolean matches(Dependency dependency) {
    return matchesGroupId(dependency.getGroupId()) && matchesArtifactId(dependency.getArtifactId()) && versionMatcher.matches(dependency);
  }

  private boolean matchesGroupId(LocatedAttribute groupId) {
    return attributeMatchesPattern(groupId, groupIdPattern);
  }

  private boolean matchesArtifactId(LocatedAttribute artifactId) {
    return attributeMatchesPattern(artifactId, artifactIdPattern);
  }
}
