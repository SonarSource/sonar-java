/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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
import org.sonar.java.PerformanceMeasure;
import org.sonar.java.PerformanceMeasure.DurationReport;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.CheckList;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.se.SymbolicExecutionVisitor;
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
  private final PostAnalysisIssueFilter postAnalysisIssueFilter;

  public JavaSquidSensor(SonarComponents sonarComponents, FileSystem fs, JavaResourceLocator javaResourceLocator,
                         Configuration settings, NoSonarFilter noSonarFilter, PostAnalysisIssueFilter postAnalysisIssueFilter) {
    this(sonarComponents, fs, javaResourceLocator, settings, noSonarFilter, postAnalysisIssueFilter, null);
  }

  public JavaSquidSensor(SonarComponents sonarComponents, FileSystem fs, JavaResourceLocator javaResourceLocator,
                         Configuration settings, NoSonarFilter noSonarFilter,
                         PostAnalysisIssueFilter postAnalysisIssueFilter, @Nullable Jasper jasper) {
    this.noSonarFilter = noSonarFilter;
    this.sonarComponents = sonarComponents;
    this.fs = fs;
    this.javaResourceLocator = javaResourceLocator;
    this.settings = settings;
    this.postAnalysisIssueFilter = postAnalysisIssueFilter;
    this.jasper = jasper;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(Java.KEY).name("JavaSquidSensor");
  }

  @Override
  public void execute(SensorContext context) {
    DurationReport sensorDuration = PerformanceMeasure.start(context.config(), "JavaSquidSensor", System::nanoTime);

    sonarComponents.setSensorContext(context);

    sonarComponents.registerCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaChecks());
    sonarComponents.registerTestCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaTestChecks());

    Measurer measurer = new Measurer(context, noSonarFilter);

    List<JavaCheck> javaChecksOrderedLikeInCheckList = Arrays.stream(sonarComponents.checkClasses())
      .sorted(Comparator.comparing(CheckList::rankOf))
      .collect(Collectors.toList());

    JavaSquid squid = new JavaSquid(getJavaVersion(), sonarComponents, measurer, javaResourceLocator, postAnalysisIssueFilter,
      // FIXME Find a better way to inject the Symbolic Execution engine
      new SymbolicExecutionVisitor(javaChecksOrderedLikeInCheckList),
      javaChecksOrderedLikeInCheckList.toArray(new JavaCheck[0]));
    squid.scan(getSourceFiles(), getTestFiles(), runJasper(context));

    sensorDuration.stopAndLog(context.fileSystem().workDir(), true);
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
    JavaVersion javaVersion = JavaVersionImpl.fromString(settings.get(JavaVersion.SOURCE_VERSION).orElse(null));
    LOG.info("Configured Java source version (" + JavaVersion.SOURCE_VERSION + "): " + javaVersion);
    return javaVersion;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
