/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaSquid;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.CheckList;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaVersion;

@Phase(name = Phase.Name.PRE)
@DependsUpon("BEFORE_SQUID")
@DependedUpon("squid")
public class JavaSquidSensor implements Sensor {

  private static final Logger LOG = Loggers.get(JavaSquidSensor.class);

  private final SonarComponents sonarComponents;
  private final FileSystem fs;
  private final DefaultJavaResourceLocator javaResourceLocator;
  private final Configuration settings;
  private final NoSonarFilter noSonarFilter;
  private final PostAnalysisIssueFilter postAnalysisIssueFilter;

  public JavaSquidSensor(SonarComponents sonarComponents, FileSystem fs,
                         DefaultJavaResourceLocator javaResourceLocator, Configuration settings, NoSonarFilter noSonarFilter, PostAnalysisIssueFilter postAnalysisIssueFilter) {
    this.noSonarFilter = noSonarFilter;
    this.sonarComponents = sonarComponents;
    this.fs = fs;
    this.javaResourceLocator = javaResourceLocator;
    this.settings = settings;
    this.postAnalysisIssueFilter = postAnalysisIssueFilter;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(Java.KEY).name("JavaSquidSensor");
  }

  @Override
  public void execute(SensorContext context) {
    javaResourceLocator.setSensorContext(context);
    sonarComponents.setSensorContext(context);

    boolean testAsFirstCitizen = testAsFirstCitizen();
    List<Class<? extends JavaCheck>> checks = ImmutableList.<Class<? extends JavaCheck>>builder()
      .addAll(CheckList.getJavaChecks())
      .addAll(CheckList.getDebugChecks())
      .addAll(testAsFirstCitizen ? CheckList.getJavaTestChecks() : Collections.emptyList())
      .build();
    sonarComponents.registerCheckClasses(CheckList.REPOSITORY_KEY, checks);
    sonarComponents.registerTestCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaTestChecks());
    Measurer measurer = new Measurer(fs, context, noSonarFilter);
    JavaSquid squid = new JavaSquid(getJavaVersion(), isXFileEnabled(), testAsFirstCitizen, sonarComponents, measurer, javaResourceLocator, postAnalysisIssueFilter,
      sonarComponents.checkClasses());
    squid.scan(getSourceFiles(), getTestFiles());
    sonarComponents.saveAnalysisErrors();
  }

  private boolean testAsFirstCitizen() {
    return settings.getBoolean(Java.TESTS_AS_FIRST_CITIZEN).orElse(false);
  }

  private Collection<File> getSourceFiles() {
    return toFile(fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Java.KEY), fs.predicates().hasType(InputFile.Type.MAIN))));
  }

  private Collection<File> getTestFiles() {
    return toFile(fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Java.KEY), fs.predicates().hasType(InputFile.Type.TEST))));
  }

  private static Collection<File> toFile(Iterable<InputFile> inputFiles) {
    return StreamSupport.stream(inputFiles.spliterator(), false).map(InputFile::file).collect(Collectors.toList());
  }

  private JavaVersion getJavaVersion() {
    JavaVersion javaVersion = JavaVersionImpl.fromString(settings.get(Java.SOURCE_VERSION).orElse(null));
    LOG.info("Configured Java source version (" + Java.SOURCE_VERSION + "): " + javaVersion);
    return javaVersion;
  }

  private boolean isXFileEnabled() {
    return settings.getBoolean("sonar.java.xfile").orElse(false);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
