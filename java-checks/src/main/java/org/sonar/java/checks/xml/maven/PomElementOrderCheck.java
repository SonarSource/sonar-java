/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.xml.maven.PomCheck;
import org.sonar.java.xml.maven.PomCheckContext;
import org.sonar.java.xml.maven.PomCheckContext.Location;
import org.sonar.maven.model.LocatedTree;
import org.sonar.maven.model.maven2.MavenProject;

@Rule(key = "S3423")
public class PomElementOrderCheck implements PomCheck {

  private static final Comparator<LocatedTree> LINE_COMPARATOR = (l1, l2) -> Integer.compare(l1.startLocation().line(), l2.startLocation().line());

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
      project.getDescription(),
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
    List<LocatedTree> expectedOrder = Arrays.stream(trees).filter(Objects::nonNull).collect(Collectors.toList());
    List<LocatedTree> observedOrder = expectedOrder.stream().sorted(LINE_COMPARATOR).collect(Collectors.toList());

    int lastWrongPosition = -1;
    int firstWrongPosition = -1;

    for (int index = 0; index < expectedOrder.size(); index++) {
      if (observedOrder.indexOf(expectedOrder.get(index)) != index) {
        lastWrongPosition = index;
        if (firstWrongPosition == -1) {
          firstWrongPosition = index;
        }
      }
    }

    if (lastWrongPosition == -1) {
      return Collections.emptyList();
    }

    List<Location> issues = new ArrayList<>();
    // only reports between first and last wrong position
    for (int index = firstWrongPosition; index <= lastWrongPosition; index++) {
      issues.add(new Location("Expected position: " + (index + 1), expectedOrder.get(index).startLocation().line()));
    }

    return issues;
  }
}
