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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.xml.maven.helpers.MavenDependencyCollector;
import org.sonar.java.xml.maven.PomCheck;
import org.sonar.java.xml.maven.PomCheckContext;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.maven2.Dependency;

import javax.annotation.Nullable;
import java.util.List;

@Rule(key = "S3422")
public class DependencyWithSystemScopeCheck implements PomCheck {

  @Override
  public void scanFile(PomCheckContext context) {
    List<Dependency> dependencies = new MavenDependencyCollector(context.getMavenProject()).allDependencies();
    for (Dependency dependency : dependencies) {
      LocatedAttribute scope = dependency.getScope();
      if (scope != null && "system".equalsIgnoreCase(scope.getValue())) {
        String message = "Update this scope.";
        LocatedAttribute systemPath = dependency.getSystemPath();
        List<PomCheckContext.Location> secondaries = getSecondary(systemPath);
        if (systemPath != null) {
          message = "Update this scope and remove the \"systemPath\".";
        }
        context.reportIssue(this, scope.startLocation().line(), message, secondaries);
      }
    }
  }

  private static List<PomCheckContext.Location> getSecondary(@Nullable LocatedAttribute systemPath) {
    if (systemPath != null && StringUtils.isNotBlank(systemPath.getValue())) {
      return Lists.newArrayList(new PomCheckContext.Location("Remove this", systemPath));
    }
    return ImmutableList.of();
  }
}
