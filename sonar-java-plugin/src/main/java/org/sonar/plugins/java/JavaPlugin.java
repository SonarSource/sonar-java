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
package org.sonar.plugins.java;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaClasspathProperties;
import org.sonar.java.JavaTestClasspath;
import org.sonar.java.SonarComponents;
import org.sonar.java.filters.SuppressWarningsFilter;
import org.sonar.plugins.jacoco.JaCoCoExtensions;
import org.sonar.plugins.surefire.SurefireExtensions;

import java.util.List;

public class JavaPlugin extends SonarPlugin {

  private static final String JAVA_CATEGORY = "java";
  private static final String GENERAL_SUBCATEGORY = "General";

  public static final String SQUID_ANALYSE_ACCESSORS_PROPERTY = "sonar.squid.analyse.property.accessors";
  public static final boolean SQUID_ANALYSE_ACCESSORS_DEFAULT_VALUE = true;
  public static final String JSON_OUTPUT_FOLDER = "sonar.java.jsonoutput.folder";

  @Override
  public List getExtensions() {
    ImmutableList.Builder<Object> builder = ImmutableList.builder();
    builder.addAll(SurefireExtensions.getExtensions());
    builder.addAll(JaCoCoExtensions.getExtensions());
    builder.addAll(JavaClasspathProperties.getProperties());
    builder.add(
        JavaClasspath.class,
        JavaTestClasspath.class,
        JavaCommonRulesEngine.class,
        JavaCommonRulesDecorator.class,
        Java.class,
        CommonRulesSonarWayProfile.class,
        PropertyDefinition.builder(Java.FILE_SUFFIXES_KEY)
            .defaultValue(Java.DEFAULT_FILE_SUFFIXES)
            .name("File suffixes")
            .description("Comma-separated list of suffixes for files to analyze. To not filter, leave the list empty.")
            .subCategory("General")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder(JavaPlugin.SQUID_ANALYSE_ACCESSORS_PROPERTY)
            .defaultValue(Boolean.toString(JavaPlugin.SQUID_ANALYSE_ACCESSORS_DEFAULT_VALUE))
            .category(JAVA_CATEGORY)
            .subCategory(GENERAL_SUBCATEGORY)
            .name("Separate Accessors")
            .description("Flag whether SonarQube should separate accessors (getters/setters) from methods. " +
                "In that case, accessors are not counted in metrics such as complexity or API documentation.")
            .type(PropertyType.BOOLEAN)
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder(CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY)
            .defaultValue(Boolean.toString(CoreProperties.DESIGN_SKIP_DESIGN_DEFAULT_VALUE))
            .category(JAVA_CATEGORY)
            .subCategory(GENERAL_SUBCATEGORY)
            .name("Skip design analysis")
            .type(PropertyType.BOOLEAN)
            .hidden()
            .build(),
        PropertyDefinition.builder(JavaPlugin.JSON_OUTPUT_FOLDER)
            .category(JAVA_CATEGORY)
            .subCategory(GENERAL_SUBCATEGORY)
            .name("Output folder of issues as Json files")
            .type(PropertyType.STRING)
            .hidden()
            .build(),

        JavaRulesDefinition.class,
        JavaSonarWayProfile.class,
        SonarComponents.class,
        DefaultJavaResourceLocator.class,
        JavaSquidSensor.class,
        SuppressWarningsFilter.class);
    return builder.build();
  }

}
