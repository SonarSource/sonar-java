/*
 * SonarQube Java
 * Copyright (C) 2010-2016 SonarSource SA
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
package org.sonar.plugins.jacoco;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.Version;

public class JacocoConstants {

  public static final Version SQ_6_2 = Version.create(6, 2);
  public static final String REPORT_PATH_PROPERTY = "sonar.jacoco.reportPath";
  public static final String REPORT_PATH_DEFAULT_VALUE = "target/jacoco.exec";
  public static final String REPORT_PATHS_PROPERTY = "sonar.jacoco.reportPaths";
  public static final String REPORT_PATHS_DEFAULT_VALUE = "target/jacoco.exec,target/jacoco-it.exec";
  public static final String IT_REPORT_PATH_PROPERTY = "sonar.jacoco.itReportPath";
  public static final String IT_REPORT_PATH_DEFAULT_VALUE = "target/jacoco-it.exec";
  public static final String REPORT_MISSING_FORCE_ZERO = "sonar.jacoco.reportMissing.force.zero";
  public static final boolean REPORT_MISSING_FORCE_ZERO_DEFAULT_VALUE = false;

  private JacocoConstants() {
  }

  public static List<PropertyDefinition> getPropertyDefinitions(Version sqVersion) {
    String subCategory = "JaCoCo";
    if (sqVersion.isGreaterThanOrEqual(SQ_6_2)) {
      return ImmutableList.of(
        PropertyDefinition.builder(JacocoConstants.REPORT_PATHS_PROPERTY)
          .defaultValue(JacocoConstants.REPORT_PATHS_DEFAULT_VALUE)
          .category(CoreProperties.CATEGORY_JAVA)
          .subCategory(subCategory)
          .name("JaCoCo Reports")
          .description("Path to the JaCoCo report files containing coverage data. The paths may be absolute or relative to the project base directory.")
          .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
          .build(),
        PropertyDefinition.builder(JacocoConstants.REPORT_PATH_PROPERTY)
          .defaultValue(JacocoConstants.REPORT_PATH_DEFAULT_VALUE)
          .category(CoreProperties.CATEGORY_JAVA)
          .subCategory(subCategory)
          .name("UT JaCoCo Report [DEPRECATED]")
          .description("Deprecated since SonarQube 6.2. Use '" + JacocoConstants.REPORT_PATHS_PROPERTY + "' instead.")
          .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
          .build(),
        PropertyDefinition.builder(JacocoConstants.IT_REPORT_PATH_PROPERTY)
          .defaultValue(JacocoConstants.IT_REPORT_PATH_DEFAULT_VALUE)
          .category(CoreProperties.CATEGORY_JAVA)
          .subCategory(subCategory)
          .name("IT JaCoCo Report [DEPRECATED]")
          .description("Deprecated since SonarQube 6.2. Use '" + JacocoConstants.REPORT_PATHS_PROPERTY + "' instead.")
          .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
          .build());
    } else {
      return ImmutableList.of(
        PropertyDefinition.builder(JacocoConstants.REPORT_PATH_PROPERTY)
          .defaultValue(JacocoConstants.REPORT_PATH_DEFAULT_VALUE)
          .category(CoreProperties.CATEGORY_JAVA)
          .subCategory(subCategory)
          .name("UT JaCoCo Report")
          .description("Path to the JaCoCo report file containing coverage data by unit tests. The path may be absolute or relative to the project base directory.")
          .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
          .build(),
        PropertyDefinition.builder(JacocoConstants.IT_REPORT_PATH_PROPERTY)
          .defaultValue(JacocoConstants.IT_REPORT_PATH_DEFAULT_VALUE)
          .category(CoreProperties.CATEGORY_JAVA)
          .subCategory(subCategory)
          .name("IT JaCoCo Report")
          .description("Path to the JaCoCo report file containing coverage data by integration tests. The path may be absolute or relative to the project base directory.")
          .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
          .build(),
        PropertyDefinition.builder(JacocoConstants.REPORT_MISSING_FORCE_ZERO)
          .defaultValue(Boolean.toString(JacocoConstants.REPORT_MISSING_FORCE_ZERO_DEFAULT_VALUE))
          .name("Force zero coverage")
          .category(CoreProperties.CATEGORY_JAVA)
          .subCategory(subCategory)
          .description("Force coverage to 0% if no JaCoCo reports are found during analysis. Deprecated for SonarQube 6.2+")
          .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
          .type(PropertyType.BOOLEAN)
          .build());
    }
  }

}
