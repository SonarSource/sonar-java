/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.plugins.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.java.AnalysisWarningsWrapper;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaConstants;
import org.sonar.java.SonarComponents;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForMainForSonarLint;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.classpath.ClasspathProperties;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.java.jsp.Jasper;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.surefire.SurefireExtensions;

public class JavaPlugin implements Plugin {

  @SuppressWarnings("unchecked")
  @Override
  public void define(Context context) {
    List<Object> list = new ArrayList<>();

    if (context.getRuntime().getProduct() == SonarProduct.SONARLINT) {
      list.add(ClasspathForMainForSonarLint.class);
    } else {
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
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      JavaRulesDefinition.class,
      SonarComponents.class,
      DefaultJavaResourceLocator.class,
      PropertyDefinition.builder(JavaVersion.ENABLE_PREVIEW)
      .name("Enable JDK's latest preview feature")
      .description("Allow to enable JDK's preview features for analysis. Only the Java's latest supported version preview features are supported.")
      .category(JavaConstants.JAVA_CATEGORY)
      .subCategory("Language")
      .onQualifiers(Qualifiers.PROJECT)
      .type(PropertyType.BOOLEAN)
      .defaultValue("False")
      .build(),
      JavaSensor.class,
      PostAnalysisIssueFilter.class
    ));

    list.add(AnalysisWarningsWrapper.class);
    
    context.addExtensions(Collections.unmodifiableList(list));
  }

  private static boolean supportJspTranspilation(Context context) {
    // currently, only security rules are interested in jsp
    return context.getRuntime().getProduct() == SonarProduct.SONARQUBE
      && context.getRuntime().getEdition() != SonarEdition.COMMUNITY;
  }
}
