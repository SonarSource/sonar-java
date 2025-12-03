/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.surefire;

import java.util.Arrays;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.java.JavaConstants;
import org.sonar.plugins.surefire.api.SurefireUtils;

import java.util.List;

public final class SurefireExtensions {

  private SurefireExtensions() {
  }

  @SuppressWarnings("rawtypes")
  public static List getExtensions() {
    return Arrays.asList(
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
