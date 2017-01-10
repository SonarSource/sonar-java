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

import org.sonar.java.xml.maven.PomCheck;
import org.sonar.java.xml.maven.PomCheckContext;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.maven2.MavenProject;

import javax.annotation.Nullable;

import java.util.regex.Pattern;

public abstract class AbstractNamingConvention implements PomCheck {

  private Pattern pattern = null;

  @Override
  public void scanFile(PomCheckContext context) {
    String regex = getRegex();
    if (pattern == null) {
      try {
        pattern = Pattern.compile(regex, Pattern.DOTALL);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("[" + getRuleKey() + "] Unable to compile the regular expression: " + regex, e);
      }
    }
    NamedLocatedAttribute namedAttribute = getTargetedLocatedAttribute(context.getMavenProject());
    if (namedAttribute.attribute != null && !pattern.matcher(namedAttribute.attribute.getValue()).matches()) {
      context.reportIssue(this, namedAttribute.attribute, "Update this \"" + namedAttribute.name + "\" to match the provided regular expression: '" + regex + "'");
    }
  }

  protected abstract String getRegex();

  protected abstract String getRuleKey();

  protected abstract NamedLocatedAttribute getTargetedLocatedAttribute(MavenProject mavenProject);

  protected static class NamedLocatedAttribute {
    private final String name;
    @Nullable
    private final LocatedAttribute attribute;

    public NamedLocatedAttribute(String name, @Nullable LocatedAttribute attribute) {
      this.name = name;
      this.attribute = attribute;
    }
  }
}
