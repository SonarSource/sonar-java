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
package org.sonar.java.checks.maven;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.maven.MavenFileScanner;
import org.sonar.maven.MavenFileScannerContext;
import org.sonar.maven.model.LocatedTree;
import org.sonar.maven.model.XmlLocation;
import org.sonar.maven.model.maven2.MavenProject;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S3423",
  name = "pom elements should be in the recommended order",
  priority = Priority.MINOR,
  tags = {Tag.CONVENTION, Tag.MAVEN})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("10min")
public class PomElementOrderCheck implements MavenFileScanner {

  @Override
  public void scanFile(MavenFileScannerContext context) {
    MavenProject project = context.getMavenProject();
    boolean correctOrder = checkPositions(project.startLocation(),
      project.getModelVersion(),
      project.getParent(),
      project.getGroupId(),
      project.getArtifactId(),
      project.getVersion(),
      project.getPackaging(),
      project.getInceptionYear(),
      project.getOrganization(),
      project.getLicenses(),
      project.getDevelopers(),
      project.getContributors(),
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

    if (!correctOrder) {
      context.reportIssue(this, project.startLocation().line(), "Reorder the elements of this pom to match the recommended order.");
    }
  }

  private static boolean checkPositions(XmlLocation projectLocation, LocatedTree... trees) {
    int currentLine = projectLocation.line();
    for (LocatedTree locatedTree : trees) {
      if (locatedTree != null) {
        int treeLine = locatedTree.startLocation().line();
        if (treeLine < currentLine) {
          return false;
        }
        currentLine = treeLine;
      }
    }
    return true;
  }
}
