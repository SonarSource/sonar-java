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

import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.maven.helpers.MavenDependencyCollector;
import org.sonar.java.tag.Tag;
import org.sonar.maven.MavenFileScanner;
import org.sonar.maven.MavenFileScannerContext;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.maven2.Dependency;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S3422",
  name = "Dependencies should not have \"system\" scope",
  priority = Priority.MAJOR,
  tags = {Tag.LOCK_IN, Tag.MAVEN})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.OS_RELATED_PORTABILITY)
@SqaleConstantRemediation("5min")
public class DependencyWithSystemScopeCheck implements MavenFileScanner {

  @Override
  public void scanFile(MavenFileScannerContext context) {
    List<Dependency> dependencies = new MavenDependencyCollector(context.getMavenProject()).allDependencies();
    for (Dependency dependency : dependencies) {
      LocatedAttribute scope = dependency.getScope();
      if (scope != null && "system".equalsIgnoreCase(scope.getValue())) {
        context.reportIssue(
          this,
          scope.startLocation().line(),
          "Update this scope and remove the \"systemPath\".",
          Lists.newArrayList(new MavenFileScannerContext.Location("Remove this", dependency.getSystemPath())));
      }
    }
  }
}
