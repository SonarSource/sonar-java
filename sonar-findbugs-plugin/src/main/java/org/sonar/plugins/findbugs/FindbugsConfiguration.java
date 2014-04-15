/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
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
package org.sonar.plugins.findbugs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;


public class FindbugsConfiguration implements BatchExtension {

  private final ModuleFileSystem fileSystem;
  private final Settings settings;
  private final RulesProfile profile;
  private final FindbugsProfileExporter exporter;
  private final ProjectClasspath projectClasspath;
  private final JavaResourceLocator javaResourceLocator;

  public FindbugsConfiguration(ModuleFileSystem fileSystem, Settings settings, RulesProfile profile, FindbugsProfileExporter exporter, ProjectClasspath classpath,
                               JavaResourceLocator javaResourceLocator) {
    this.fileSystem = fileSystem;
    this.settings = settings;
    this.profile = profile;
    this.exporter = exporter;
    this.projectClasspath = classpath;
    this.javaResourceLocator = javaResourceLocator;
  }

  public File getTargetXMLReport() {
    return new File(fileSystem.workingDir(), "findbugs-result.xml");
  }

  public edu.umd.cs.findbugs.Project getFindbugsProject() throws IOException {
    edu.umd.cs.findbugs.Project findbugsProject = new edu.umd.cs.findbugs.Project();
    for (File dir : fileSystem.sourceDirs()) {
      findbugsProject.addSourceDir(dir.getAbsolutePath());
    }

    Collection<File> classFilesToAnalyze = javaResourceLocator.classFilesToAnalyze();
    for (File classToAnalyze : classFilesToAnalyze) {
      findbugsProject.addFile(classToAnalyze.getCanonicalPath());
    }

    if (classFilesToAnalyze.isEmpty()) {
      throw new SonarException("Findbugs needs sources to be compiled. "
          + "Please build project before executing sonar and check the location of compiled classes.");
    }

    for (File file : projectClasspath.getElements()) {
      findbugsProject.addAuxClasspathEntry(file.getAbsolutePath());
    }
    copyLibs();
    if (annotationsLib != null) {
      // Findbugs dependencies are packaged by Maven. They are not available during execution of unit tests.
      findbugsProject.addAuxClasspathEntry(annotationsLib.getAbsolutePath());
      findbugsProject.addAuxClasspathEntry(jsr305Lib.getAbsolutePath());
    }
    findbugsProject.setCurrentWorkingDirectory(fileSystem.buildDir());
    return findbugsProject;
  }

  @VisibleForTesting
  File saveIncludeConfigXml() throws IOException {
    StringWriter conf = new StringWriter();
    exporter.exportProfile(profile, conf);
    File file = new File(fileSystem.workingDir(), "findbugs-include.xml");
    FileUtils.write(file, conf.toString(), CharEncoding.UTF_8);
    return file;
  }

  @VisibleForTesting
  List<File> getExcludesFilters() {
    List<File> result = Lists.newArrayList();
    PathResolver pathResolver = new PathResolver();
    String[] filters = settings.getStringArray(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY);
    for (String excludesFilterPath : filters) {
      excludesFilterPath = StringUtils.trim(excludesFilterPath);
      if (StringUtils.isNotBlank(excludesFilterPath)) {
        result.add(pathResolver.relativeFile(fileSystem.baseDir(), excludesFilterPath));
      }
    }
    return result;
  }

  public String getEffort() {
    return StringUtils.lowerCase(settings.getString(FindbugsConstants.EFFORT_PROPERTY));
  }

  public String getConfidenceLevel() {
    return StringUtils.lowerCase(settings.getString(FindbugsConstants.CONFIDENCE_LEVEL_PROPERTY));
  }

  public long getTimeout() {
    return settings.getLong(FindbugsConstants.TIMEOUT_PROPERTY);
  }

  private File jsr305Lib;
  private File annotationsLib;

  public void copyLibs() {
    if (jsr305Lib == null) {
      jsr305Lib = copyLib("/jsr305.jar");
    }
    if (annotationsLib == null) {
      annotationsLib = copyLib("/annotations.jar");
    }
  }

  /**
   * Invoked by PicoContainer to remove temporary files.
   */
  public void stop() {
    jsr305Lib.delete();
    annotationsLib.delete();
  }

  private File copyLib(String name) {
    InputStream input = null;
    try {
      input = getClass().getResourceAsStream(name);
      File dir = new File(fileSystem.workingDir(), "findbugs");
      FileUtils.forceMkdir(dir);
      File target = new File(dir, name);
      FileUtils.copyInputStreamToFile(input, target);
      return target;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to extract Findbugs dependency", e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  public static List<PropertyDefinition> getPropertyDefinitions() {
    String subCategory = "FindBugs";
    return ImmutableList.of(
        PropertyDefinition.builder(FindbugsConstants.EFFORT_PROPERTY)
            .defaultValue(FindbugsConstants.EFFORT_DEFAULT_VALUE)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(subCategory)
            .name("Effort")
            .description("Effort of the bug finders. Valid values are Min, Default and Max. Setting 'Max' increases precision but also increases " +
                "memory consumption.")
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .build(),
        PropertyDefinition.builder(FindbugsConstants.TIMEOUT_PROPERTY)
            .defaultValue(FindbugsConstants.TIMEOUT_DEFAULT_VALUE + "")
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(subCategory)
            .name("Timeout")
            .description("Specifies the amount of time, in milliseconds, that FindBugs may run before it is assumed to be hung and is terminated. " +
                "The default is 600,000 milliseconds, which is ten minutes.")
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .type(PropertyType.INTEGER)
            .build(),
        PropertyDefinition.builder(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(subCategory)
            .name("Excludes Filters")
            .description("Paths to findbugs filter-files with exclusions.")
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .multiValues(true)
            .build(),
        PropertyDefinition.builder(FindbugsConstants.CONFIDENCE_LEVEL_PROPERTY)
            .defaultValue(FindbugsConstants.CONFIDENCE_LEVEL_DEFAULT_VALUE)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(subCategory)
            .name("Confidence Level")
            .description("Specifies the confidence threshold (previously called \"priority\") for reporting issues. If set to \"low\", confidence is not used to filter bugs. " +
                "If set to \"medium\" (the default), low confidence issues are supressed. If set to \"high\", only high confidence bugs are reported. ")
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .build()
    );
  }

}
