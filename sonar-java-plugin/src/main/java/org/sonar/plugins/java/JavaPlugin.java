/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.api.Plugin;
import org.sonar.api.SonarProduct;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaClasspathProperties;
import org.sonar.java.JavaConstants;
import org.sonar.java.JavaSonarLintClasspath;
import org.sonar.java.JavaTestClasspath;
import org.sonar.java.SonarComponents;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.plugins.jacoco.JaCoCoExtensions;
import org.sonar.plugins.surefire.SurefireExtensions;

public class JavaPlugin implements Plugin {

  @Override
  public void define(Context context) {
    ImmutableList.Builder<Object> builder = ImmutableList.builder();
    if (context.getRuntime().getProduct() == SonarProduct.SONARLINT) {
      builder.add(JavaSonarLintClasspath.class);
    } else {
      builder.addAll(SurefireExtensions.getExtensions());
      builder.addAll(JaCoCoExtensions.getExtensions());
      builder.add(JavaSonarWayProfile.class);
      builder.add(JavaClasspath.class);
      builder.add(PropertyDefinition.builder("sonar.java.failOnException")
        .defaultValue("false")
        .hidden()
        .name("Fail on exceptions")
        .description("when set to true, if an exception is thrown by the analyzer the analysis will fail")
        .build());
    }
    builder.addAll(JavaClasspathProperties.getProperties());
    builder.add(
      JavaTestClasspath.class,
      Java.class,
      PropertyDefinition.builder(Java.FILE_SUFFIXES_KEY)
        .defaultValue(Java.DEFAULT_FILE_SUFFIXES)
        .category(JavaConstants.JAVA_CATEGORY)
        .name("File suffixes")
        .multiValues(true)
        .description("Comma-separated list of suffixes for files to analyze. To not filter, leave the list empty.")
        .subCategory("General")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      JavaRulesDefinition.class,
      SonarComponents.class,
      DefaultJavaResourceLocator.class,
      JavaSquidSensor.class,
      PostAnalysisIssueFilter.class,
      XmlFileSensor.class);
    context.addExtensions(builder.build());
  }

}
