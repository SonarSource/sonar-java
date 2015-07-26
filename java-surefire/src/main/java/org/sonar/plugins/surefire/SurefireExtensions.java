/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.surefire;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.surefire.api.SurefireUtils;

import java.util.List;

public final class SurefireExtensions {

  private SurefireExtensions() {
  }

  public static List getExtensions() {
    return ImmutableList.of(
        PropertyDefinition.builder(SurefireUtils.SUREFIRE_REPORTS_PATH_PROPERTY)
            .name("JUnit Reports")
            .description("Path to the directory containing all the *.xml JUnit report files. The path may be absolute or relative to the project base directory.")
            .onQualifiers(Qualifiers.PROJECT)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory("JUnit")
            .build(),

        SurefireSensor.class,
        SurefireJavaParser.class);
  }

}
