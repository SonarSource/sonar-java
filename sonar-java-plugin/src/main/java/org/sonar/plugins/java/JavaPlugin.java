/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.plugins.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.java.AnalysisWarningsWrapper;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaConstants;
import org.sonar.java.SonarComponents;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.telemetry.TelemetryStorage;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForMainForSonarLint;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.classpath.ClasspathProperties;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.java.jsp.Jasper;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.caching.SonarLintCache;
import org.sonar.plugins.surefire.SurefireExtensions;

public class JavaPlugin implements Plugin {

  @SuppressWarnings("unchecked")
  @Override
  public void define(Context context) {
    List<Object> list = new ArrayList<>();

    if (context.getRuntime().getProduct() == SonarProduct.SONARLINT) {
      list.add(Telemetry.class);
      list.add(ClasspathForMainForSonarLint.class);
      // Some custom rules (i.e. DBD) depend on the presence of SonarLintCache when executing in a SonarLint context.
      // Hence, we must provide it here.
      list.add(SonarLintCache.class);
    } else {
      list.add(TelemetryStorage.class);
      list.add(ProjectEndOfAnalysisSensor.class);
      list.addAll(SurefireExtensions.getExtensions());
      list.add(DroppedPropertiesSensor.class);
      list.add(JavaSonarWayProfile.class);
      list.add(ClasspathForMain.class);

      ExternalReportExtensions.define(context);
    }
    if (supportJspTranspilation(context)) {
      list.add(Jasper.class);
    }
    list.addAll(ClasspathProperties.getProperties());
    list.addAll(Arrays.asList(
      ClasspathForTest.class,
      Java.class,
      PropertyDefinition.builder(Java.FILE_SUFFIXES_KEY)
        .defaultValue(Java.DEFAULT_FILE_SUFFIXES)
        .category(JavaConstants.JAVA_CATEGORY)
        .name("File suffixes")
        .multiValues(true)
        .description("List of suffixes for Java files to analyze. To not filter, leave the list empty.")
        .subCategory("General")
        .onConfigScopes(Set.of(PropertyDefinition.ConfigScope.PROJECT))
        .build(),
      JavaRulesDefinition.class,
      SonarComponents.class,
      DefaultJavaResourceLocator.class,
      PropertyDefinition.builder(JavaVersion.ENABLE_PREVIEW)
        .name("Enable JDK's latest preview feature")
        .description("Allow to enable JDK's preview features for analysis. Only the Java's latest supported version preview features are supported.")
        .category(JavaConstants.JAVA_CATEGORY)
        .subCategory("Language")
        .onConfigScopes(Set.of(PropertyDefinition.ConfigScope.PROJECT))
        .type(PropertyType.BOOLEAN)
        .defaultValue("False")
        .build(),
      PropertyDefinition.builder(SonarComponents.SONAR_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE)
        .name("Ignore unnamed module for split package")
        .description(
          "<p>" +
            "Prevent the Java parser from enforcing Java platform modularization by omitting package and class re-declarations gathered in the unnamed module." +
            "</p>" +
            "<p>" +
            "With the Java Platform Module System introduced in Java 9, packages and classes declared outside of" +
            " explicitly named modules are placed in a common \"unnamed module\"." +
            " When a package or class is found in both the unnamed module and a named one, modularization is broken." +
            " As a result, the parser may be unable to build the project semantic successfully, leading to analysis failure." +
            " This parameter allows users to bypass Java platform modularity enforcement to prevent analysis failure." +
            "</p>"
        )
        .category(JavaConstants.JAVA_CATEGORY)
        .subCategory("Language")
        .onConfigScopes(Set.of(PropertyDefinition.ConfigScope.PROJECT))
        .type(PropertyType.BOOLEAN)
        .defaultValue("False")
        .build(),
      JavaSensor.class,
      PostAnalysisIssueFilter.class));

    list.add(AnalysisWarningsWrapper.class);
    context.addExtensions(Collections.unmodifiableList(list));
  }

  private static boolean supportJspTranspilation(Context context) {
    // currently, only security rules are interested in jsp
    return context.getRuntime().getProduct() == SonarProduct.SONARQUBE
      && context.getRuntime().getEdition() != SonarEdition.COMMUNITY;
  }
}
