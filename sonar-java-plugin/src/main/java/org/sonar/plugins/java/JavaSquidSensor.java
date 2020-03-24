/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
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
import org.sonar.java.JavaSquid;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.CheckList;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;

@Phase(name = Phase.Name.PRE)
@DependsUpon("BEFORE_SQUID")
@DependedUpon("squid")
public class JavaSquidSensor implements Sensor {

  private static final Logger LOG = Loggers.get(JavaSquidSensor.class);

  private final SonarComponents sonarComponents;
  private final FileSystem fs;
  private final JavaResourceLocator javaResourceLocator;
  private final Configuration settings;
  private final NoSonarFilter noSonarFilter;
  @Nullable
  private final Jasper jasper;

  public JavaSquidSensor(SonarComponents sonarComponents, FileSystem fs, JavaResourceLocator javaResourceLocator,
                         Configuration settings, NoSonarFilter noSonarFilter) {
    this(sonarComponents, fs, javaResourceLocator, settings, noSonarFilter, null);
  }

  public JavaSquidSensor(SonarComponents sonarComponents, FileSystem fs, JavaResourceLocator javaResourceLocator,
                         Configuration settings, NoSonarFilter noSonarFilter, @Nullable Jasper jasper) {
    this.noSonarFilter = noSonarFilter;
    this.sonarComponents = sonarComponents;
    this.fs = fs;
    this.javaResourceLocator = javaResourceLocator;
    this.settings = settings;
    this.jasper = jasper;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(Java.KEY).name("JavaSquidSensor");
  }

  @Override
  public void execute(SensorContext context) {
    sonarComponents.setSensorContext(context);

    List<Class<? extends JavaCheck>> checks = ImmutableList.<Class<? extends JavaCheck>>builder()
      .addAll(CheckList.getJavaChecks())
      .addAll(CheckList.getDebugChecks())
      .build();
    sonarComponents.registerCheckClasses(CheckList.REPOSITORY_KEY, checks);
    sonarComponents.registerTestCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaTestChecks());

    Measurer measurer = new Measurer(context, noSonarFilter);
    PostAnalysisIssueFilter postAnalysisIssueFilter = new PostAnalysisIssueFilter();

    JavaSquid squid = new JavaSquid(getJavaVersion(), isXFileEnabled(), sonarComponents, measurer, javaResourceLocator, postAnalysisIssueFilter, sonarComponents.checkClasses());
    squid.scan(getSourceFiles(), getTestFiles(), runJasper(context));
  }

  private Collection<GeneratedFile> runJasper(SensorContext context) {
    return jasper != null ? jasper.generateFiles(context, sonarComponents.getJavaClasspath()) : Collections.emptyList();
  }

  private Iterable<InputFile> getSourceFiles() {
    return javaFiles(InputFile.Type.MAIN);
  }

  private Iterable<InputFile> getTestFiles() {
    return javaFiles(InputFile.Type.TEST);
  }

  private Iterable<InputFile> javaFiles(InputFile.Type type) {
    return fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Java.KEY), fs.predicates().hasType(type)));
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
