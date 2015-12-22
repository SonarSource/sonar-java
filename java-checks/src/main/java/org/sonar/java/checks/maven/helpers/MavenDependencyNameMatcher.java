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

import org.sonar.java.checks.maven.AbstractNamingConvention;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.maven2.Dependency;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Matchers targeting names of dependencies.
 */
public class MavenDependencyNameMatcher extends MavenDependencyAbstractMatcher {
  private Pattern groupIdPattern;
  private Pattern artifactIdPattern;

  /**
   * Build a list of dependency name matchers for a given check.
   *
   * <br />
   *
   * Details regarding format of parameter <code>dependencyNames</code>:
   * <ul>
   *   <li>Expected format: "<code>[groupId]:[artifactId]</code>".</li>
   *   <li>Wildcard "*" can be used instead of providing a groupId or artifactId (i.e. "<code>*:myArtifact</code>")</li>
   *   <li>Regular expression can be used to define groupId and/or artifactId (i.e. "<code>org.sonar.*:*java</code>")</li>
   *   <li>Multiple dependency names matchers can be provided when separated by comma (i.e. "<code>*:log,a.b.c:*</code>")</li>
   * </ul>
   *
   * @param dependencyNames the comma-separated list of dependency names expected.
   * @param callingCheckKey the check requiring the matchers
   * @return
   */
  public static List<MavenDependencyNameMatcher> fromString(String dependencyNames, String callingCheckKey) {
    String[] names = dependencyNames.split(",");
    List<MavenDependencyNameMatcher> results = new LinkedList<>();
    for (String dependencyName : names) {
      results.add(getNamePattern(dependencyName.trim(), callingCheckKey));
    }

    return results;
  }

  private static MavenDependencyNameMatcher getNamePattern(String dependencyName, String callingCheckKey) {
    MavenDependencyNameMatcher namePattern = new MavenDependencyNameMatcher();
    String[] name = dependencyName.split(":");
    if (name.length != 2) {
      throw new IllegalArgumentException("[" + callingCheckKey + "] invalid dependency name. Should match '[groupId]:[artifactId]', use '*' as wildcard");
    }
    String groupId = name[0].trim();
    if (!isWildCard(groupId)) {
      namePattern.groupIdPattern = AbstractNamingConvention.compileRegex(groupId, callingCheckKey);
    }
    String artifactId = name[1].trim();
    if (!isWildCard(artifactId)) {
      namePattern.artifactIdPattern = AbstractNamingConvention.compileRegex(artifactId, callingCheckKey);
    }
    return namePattern;
  }

  @Override
  public boolean matches(Dependency dependency) {
    return matchesGroupId(dependency.getGroupId()) && matchesArtifactId(dependency.getArtifactId());
  }

  private boolean matchesGroupId(LocatedAttribute groupId) {
    return attributeMatchesPattern(groupId, groupIdPattern);
  }

  private boolean matchesArtifactId(LocatedAttribute artifactId) {
    return attributeMatchesPattern(artifactId, artifactIdPattern);
  }
}
