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
import org.sonar.java.xml.maven.PomCheck;
import org.sonar.java.xml.maven.PomCheckContext;
import org.sonar.java.xml.maven.PomCheckContext.Location;
import org.sonar.maven.model.LocatedTree;
import org.sonar.maven.model.maven2.MavenProject;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "S3423")
public class PomElementOrderCheck implements PomCheck {

  private static final Comparator<LocatedTree> LINE_COMPARATOR = new LineComparator();

  @Override
  public void scanFile(PomCheckContext context) {
    MavenProject project = context.getMavenProject();
    List<Location> issues = checkPositions(
      project.getModelVersion(),
      project.getParent(),
      project.getGroupId(),
      project.getArtifactId(),
      project.getVersion(),
      project.getPackaging(),
      project.getName(),
      project.getUrl(),
      project.getInceptionYear(),
      project.getOrganization(),
      project.getLicenses(),
      project.getDevelopers(),
      project.getContributors(),
      project.getMailingLists(),
      project.getPrerequisites(),
      project.getModules(),
      project.getScm(),
      project.getIssueManagement(),
      project.getCiManagement(),
      project.getDistributionManagement(),
      project.getProperties(),
      project.getDependencyManagement(),
      project.getDependencies(),
      project.getRepositories(),
      project.getPluginRepositories(),
      project.getBuild(),
      project.getReporting(),
      project.getProfiles()
      );

    if (!issues.isEmpty()) {
      context.reportIssue(this, project.startLocation().line(), "Reorder the elements of this pom to match the recommended order.", issues);
    }
  }

  private static List<Location> checkPositions(LocatedTree... trees) {
    List<Location> issues = new LinkedList<>();
    List<LocatedTree> expectedOrder = getNonNullTrees(trees);
    List<LocatedTree> observedOrder = sortByLine(expectedOrder);

    for (int index = 0; index < expectedOrder.size(); index++) {
      LocatedTree expected = expectedOrder.get(index);
      int indexObserved = observedOrder.indexOf(expected);
      if (!issues.isEmpty() || index != indexObserved) {
        issues.add(new Location("Expected position: " + (index + 1), expected));
      }
    }

    return issues;
  }

  private static List<LocatedTree> getNonNullTrees(LocatedTree... trees) {
    List<LocatedTree> result = new LinkedList<>();
    for (LocatedTree locatedTree : trees) {
      if (locatedTree != null) {
        result.add(locatedTree);
      }
    }
    return result;
  }

  private static List<LocatedTree> sortByLine(List<LocatedTree> expectedOrder) {
    List<LocatedTree> result = new LinkedList<>(expectedOrder);
    Collections.sort(result, LINE_COMPARATOR);
    return result;
  }

  private static class LineComparator implements Comparator<LocatedTree> {
    @Override
    public int compare(LocatedTree o1, LocatedTree o2) {
      return Integer.compare(o1.startLocation().line(), o2.startLocation().line());
    }
  }
}
