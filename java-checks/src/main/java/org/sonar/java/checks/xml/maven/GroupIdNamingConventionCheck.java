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
import org.sonar.maven.model.maven2.MavenProject;

@Rule(key = GroupIdNamingConventionCheck.KEY)
public class GroupIdNamingConventionCheck extends AbstractNamingConvention {

  public static final String KEY = "S3419";
  private static final String DEFAULT_REGEX = "(com|org)(\\.[a-z][a-z-0-9]*)+";

  @RuleProperty(
    key = "regex",
    description = "The regular expression the \"groupId\" should match",
    defaultValue = "" + DEFAULT_REGEX)
  public String regex = DEFAULT_REGEX;

  @Override
  protected String getRegex() {
    return regex;
  }

  @Override
  protected String getRuleKey() {
    return KEY;
  }

  @Override
  protected NamedLocatedAttribute getTargetedLocatedAttribute(MavenProject mavenProject) {
    return new NamedLocatedAttribute("groupId", mavenProject.getGroupId());
  }

}
