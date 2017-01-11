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
package org.sonar.java.checks.xml.maven;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.xml.maven.helpers.MavenDependencyCollector;
import org.sonar.java.checks.xml.maven.helpers.MavenDependencyMatcher;
import org.sonar.java.xml.maven.PomCheck;
import org.sonar.java.xml.maven.PomCheckContext;
import org.sonar.maven.model.maven2.Dependency;
import org.sonar.squidbridge.annotations.RuleTemplate;

@Rule(key = DisallowedDependenciesCheck.KEY)
@RuleTemplate
public class DisallowedDependenciesCheck implements PomCheck {

  public static final String KEY = "S3417";

  @RuleProperty(
    key = "dependencyName",
    description = "Pattern describing forbidden dependencies group and artifact ids. E.G. '``*:.*log4j``' or '``x.y:*``'")
  public String dependencyName = "";

  @RuleProperty(
    key = "version",
    description = "Dependency version pattern or dash-delimited range. Leave blank for all versions. E.G. '``1.3.*``', '``1.0-3.1``', '``1.0-*``' or '``*-3.1``'")
  public String version = "";

  private MavenDependencyMatcher matcher = null;

  @Override
  public void scanFile(PomCheckContext context) {
    for (Dependency dependency : new MavenDependencyCollector(context.getMavenProject()).allDependencies()) {
      if (getMatcher().matches(dependency)) {
        context.reportIssue(this, dependency, "Remove this forbidden dependency.");
      }
    }
  }

  private MavenDependencyMatcher getMatcher() {
    if (matcher == null) {
      try {
        matcher = new MavenDependencyMatcher(dependencyName, version);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("[" + KEY + "] Unable to build matchers from provided dependency name: " + dependencyName, e);
      }
    }
    return matcher;
  }
}
