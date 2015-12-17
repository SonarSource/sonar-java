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
import org.sonar.check.RuleProperty;
import org.sonar.java.tag.Tag;
import org.sonar.maven.MavenFileScanner;
import org.sonar.maven.MavenFileScannerContext;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.regex.Pattern;

@Rule(
  key = "S3419",
  name = "Group ids should follow a naming convention",
  priority = Priority.MINOR,
  tags = {Tag.CONVENTION, Tag.MAVEN})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class GroupIdNamingConventionCheck implements MavenFileScanner {

  private static final String DEFAULT_REGEX = "(com|org)(\\.[a-z][a-z-0-9]*)+";

  @RuleProperty(
    key = "regex",
    description = "The regular expression the \"groupId\" should match",
    defaultValue = "" + DEFAULT_REGEX)
  public String regex = DEFAULT_REGEX;

  private Pattern pattern = null;

  @Override
  public void scanFile(MavenFileScannerContext context) {
    if (pattern == null) {
      try {
        pattern = Pattern.compile(regex, Pattern.DOTALL);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("[S3419] Unable to compile the regular expression: " + regex, e);
      }
    }
    LocatedAttribute groupId = context.getMavenProject().getGroupId();
    if (groupId != null && !pattern.matcher(groupId.getValue()).matches()) {
      context.reportIssue(this, groupId, "Update this \"groupId\" to match the provided regular expression: '" + regex + "'");
    }
  }

}
