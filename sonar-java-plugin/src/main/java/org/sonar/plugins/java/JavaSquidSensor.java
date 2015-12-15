/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
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
package org.sonar.plugins.java;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.resources.Project;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.api.JavaUtils;
import org.sonar.java.checks.CheckList;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.bridges.DesignBridge;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

@Phase(name = Phase.Name.PRE)
@DependsUpon(JavaUtils.BARRIER_BEFORE_SQUID)
@DependedUpon(value = JavaUtils.BARRIER_AFTER_SQUID)
public class JavaSquidSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(JavaSquidSensor.class);

  private final JavaClasspath javaClasspath;
  private final SonarComponents sonarComponents;
  private final FileSystem fs;
  private final DefaultJavaResourceLocator javaResourceLocator;
  private final Settings settings;
  private final NoSonarFilter noSonarFilter;

  public JavaSquidSensor(JavaClasspath javaClasspath, SonarComponents sonarComponents, FileSystem fs,
    DefaultJavaResourceLocator javaResourceLocator, Settings settings, NoSonarFilter noSonarFilter) {
    this.noSonarFilter = noSonarFilter;
    this.javaClasspath = javaClasspath;
    this.sonarComponents = sonarComponents;
    this.fs = fs;
    this.javaResourceLocator = javaResourceLocator;
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fs.hasFiles(fs.predicates().hasLanguage(Java.KEY));
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    javaResourceLocator.setSensorContext(context);
    sonarComponents.registerCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaChecks());
    sonarComponents.registerTestCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaTestChecks());
    JavaConfiguration configuration = createConfiguration();
    Measurer measurer = new Measurer(fs, context, configuration.separatesAccessorsFromMethods(), noSonarFilter);
    JavaSquid squid = new JavaSquid(configuration, sonarComponents, measurer, javaResourceLocator, sonarComponents.checkClasses());
    squid.scan(getSourceFiles(), getTestFiles(), getBytecodeFiles());
    // Design
    boolean skipPackageDesignAnalysis = settings.getBoolean(CoreProperties.DESIGN_SKIP_PACKAGE_DESIGN_PROPERTY);
    if (!skipPackageDesignAnalysis && squid.isBytecodeScanned()) {
      DesignBridge designBridge = new DesignBridge(context, squid.getGraph(), javaResourceLocator.getResourceMapping(), sonarComponents.getResourcePerspectives());
      designBridge.saveDesign(project);
    }
  }

  private Iterable<File> getSourceFiles() {
    return toFile(fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Java.KEY), fs.predicates().hasType(InputFile.Type.MAIN))));
  }

  private Iterable<File> getTestFiles() {
    return toFile(fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Java.KEY), fs.predicates().hasType(InputFile.Type.TEST))));
  }

  private static Iterable<File> toFile(Iterable<InputFile> inputFiles) {
    List<File> files = Lists.newArrayList();
    for (InputFile inputFile : inputFiles) {
      files.add(inputFile.file());
    }
    return files;
  }

  private List<File> getBytecodeFiles() {
    if (settings.getBoolean(CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY)) {
      return Collections.emptyList();
    }
    return javaClasspath.getElements();
  }

  private JavaConfiguration createConfiguration() {
    boolean analyzePropertyAccessors = settings.getBoolean(JavaPlugin.SQUID_ANALYSE_ACCESSORS_PROPERTY);
    Charset charset = fs.encoding();
    JavaConfiguration conf = new JavaConfiguration(charset);
    conf.setSeparateAccessorsFromMethods(analyzePropertyAccessors);
    JavaVersion javaVersion = getJavaVersion();
    LOG.info("Configured Java source version (" + Java.SOURCE_VERSION + "): " + javaVersion);
    conf.setJavaVersion(javaVersion);
    return conf;
  }

  private JavaVersion getJavaVersion() {
    return JavaVersionImpl.fromString(settings.getString(Java.SOURCE_VERSION));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
