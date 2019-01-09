/*
 * SonarQube Java
 * Copyright (C) 2010-2019 SonarSource SA
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
package org.sonar.plugins.jacoco;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.JavaConstants;


public class JaCoCoExtensions {


  public static final Logger LOG = Loggers.get(JaCoCoExtensions.class.getName());

  public static final String REPORT_PATHS_PROPERTY = "sonar.jacoco.reportPaths";
  public static final String REPORT_PATHS_DEFAULT_VALUE = "target/jacoco.exec, target/jacoco-it.exec";
  public static final String REPORT_PATH_PROPERTY = "sonar.jacoco.reportPath";
  public static final String IT_REPORT_PATH_PROPERTY = "sonar.jacoco.itReportPath";
  public static final String REPORT_MISSING_FORCE_ZERO = "sonar.jacoco.reportMissing.force.zero";

  private JaCoCoExtensions(){
  }

  public static List getExtensions() {
    ImmutableList.Builder<Object> extensions = ImmutableList.builder();

    extensions.add(
      PropertyDefinition.builder(REPORT_PATHS_PROPERTY)
      .defaultValue(REPORT_PATHS_DEFAULT_VALUE)
      .category(JavaConstants.JAVA_CATEGORY)
      .subCategory("JaCoCo")
      .name("JaCoCo Reports")
      .multiValues(true)
      .description("Path to the JaCoCo report files containing coverage data by unit tests. The path may be absolute or relative to the project base directory.")
      .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
      .build(),
      JaCoCoSensor.class);
    return extensions.build();
  }
}
