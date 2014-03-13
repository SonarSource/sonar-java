/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.plugins.jacoco;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.jacoco.core.runtime.AgentOptions;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.java.api.JavaSettings;

import java.util.List;

public class JacocoConfiguration implements BatchExtension {

  public static final String REPORT_PATH_PROPERTY = "sonar.jacoco.reportPath";
  public static final String REPORT_PATH_DEFAULT_VALUE = "target/jacoco.exec";
  public static final String IT_REPORT_PATH_PROPERTY = "sonar.jacoco.itReportPath";
  public static final String IT_REPORT_PATH_DEFAULT_VALUE = "";
  public static final String INCLUDES_PROPERTY = "sonar.jacoco.includes";
  public static final String EXCLUDES_PROPERTY = "sonar.jacoco.excludes";

  /**
   * Hibernate uses Javassist to modify entity classes and without exclusion of such classes from JaCoCo exception might be thrown:
   * <pre>
   * Javassist Enhancement failed: org.sonar.api.profiles.Alert
   * java.lang.VerifyError: (class: org/sonar/api/profiles/Alert_$$_javassist_3, method: <clinit> signature: ()V) Illegal local variable number
   * </pre>
   */
  public static final String EXCLUDES_DEFAULT_VALUE = "*_javassist_*";
  public static final String EXCLCLASSLOADER_PROPERTY = "sonar.jacoco.exclclassloader";

  private final Settings settings;
  private final JavaSettings javaSettings;
  private final JaCoCoAgentDownloader downloader;

  public JacocoConfiguration(Settings settings, JaCoCoAgentDownloader downloader, JavaSettings javaSettings) {
    this.settings = settings;
    this.downloader = downloader;
    this.javaSettings = javaSettings;
  }

  public boolean isEnabled(Project project) {
    return (!project.getFileSystem().mainFiles(Java.KEY).isEmpty() || !project.getFileSystem().testFiles(Java.KEY).isEmpty()) &&
      project.getAnalysisType().isDynamic(true) &&
      JaCoCoUtils.PLUGIN_KEY.equals(javaSettings.getEnabledCoveragePlugin());
  }

  public String getReportPath() {
    return settings.getString(REPORT_PATH_PROPERTY);
  }

  public String getItReportPath() {
    return settings.getString(IT_REPORT_PATH_PROPERTY);
  }

  public String getJvmArgument() {
    AgentOptions options = new AgentOptions();
    options.setDestfile(getReportPath());
    String includes = Joiner.on(':').join(settings.getStringArray(INCLUDES_PROPERTY));
    if (StringUtils.isNotBlank(includes)) {
      options.setIncludes(includes);
    }
    String excludes = Joiner.on(':').join(settings.getStringArray(EXCLUDES_PROPERTY));
    if (StringUtils.isNotBlank(excludes)) {
      options.setExcludes(excludes);
    }
    String exclclassloader = Joiner.on(':').join(settings.getStringArray(EXCLCLASSLOADER_PROPERTY));
    if (StringUtils.isNotBlank(exclclassloader)) {
      options.setExclClassloader(exclclassloader);
    }
    return options.getQuotedVMArgument(downloader.getAgentJarFile());
  }

  public String getExcludes() {
    return settings.getString(EXCLUDES_PROPERTY);
  }

  public static List<PropertyDefinition> getPropertyDefinitions() {
    String subCategory = "JaCoCo";
    return ImmutableList.of(
        PropertyDefinition.builder(JacocoConfiguration.REPORT_PATH_PROPERTY)
            .defaultValue(JacocoConfiguration.REPORT_PATH_DEFAULT_VALUE)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(subCategory)
            .name("UT JaCoCo Report")
            .description("Path to the JaCoCo report file containing coverage data by unit tests. The path may be absolute or relative to the project base directory.")
            .onlyOnQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .build(),
        PropertyDefinition.builder(JacocoConfiguration.INCLUDES_PROPERTY)
            .multiValues(true)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(subCategory)
            .name("Includes")
            .description("A list of class names that should be included in execution analysis (see wildcards)." +
              " Except for performance optimization or technical corner cases this option is normally not required.")
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .build(),
        PropertyDefinition.builder(JacocoConfiguration.EXCLUDES_PROPERTY)
            .defaultValue(JacocoConfiguration.EXCLUDES_DEFAULT_VALUE)
            .multiValues(true)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(subCategory)
            .name("Excludes")
            .description("A list of class names that should be excluded from execution analysis (see wildcards)." +
              " Except for performance optimization or technical corner cases this option is normally not required.")
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .build(),
        PropertyDefinition.builder(JacocoConfiguration.EXCLCLASSLOADER_PROPERTY)
            .multiValues(true)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(subCategory)
            .name("Excluded Class Loaders")
            .description("A list of class loader names that should be excluded from execution analysis (see wildcards)." +
              " This option might be required in case of special frameworks that conflict with JaCoCo code" +
              " instrumentation, in particular class loaders that do not have access to the Java runtime classes.")
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .build(),
        PropertyDefinition.builder(JacocoConfiguration.IT_REPORT_PATH_PROPERTY)
            .defaultValue(JacocoConfiguration.IT_REPORT_PATH_DEFAULT_VALUE)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(subCategory)
            .name("IT JaCoCo Report")
            .description("Path to the JaCoCo report file containing coverage data by integration tests. The path may be absolute or relative to the project base directory.")
            .onlyOnQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .build());
  }

}
