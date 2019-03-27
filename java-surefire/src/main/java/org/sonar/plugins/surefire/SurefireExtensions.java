/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.plugins.surefire;

import com.google.common.collect.ImmutableList;

import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.java.JavaConstants;
import org.sonar.plugins.surefire.api.SurefireUtils;

import java.util.List;

public final class SurefireExtensions {

  private SurefireExtensions() {
  }

  public static List getExtensions() {
    return ImmutableList.of(
        /**
         * @since 4.11
         */
        PropertyDefinition.builder(SurefireUtils.SUREFIRE_REPORT_PATHS_PROPERTY)
            .name("JUnit Report Paths")
            .description("Comma-separated paths to the various directories containing the *.xml JUnit report files. "
              + "Each path may be absolute or relative to the project base directory.")
            .onQualifiers(Qualifiers.PROJECT)
            .multiValues(true)
            .category(JavaConstants.JAVA_CATEGORY)
            .subCategory("JUnit")
            .build(),

        SurefireSensor.class,
        SurefireJavaParser.class);
  }

}
