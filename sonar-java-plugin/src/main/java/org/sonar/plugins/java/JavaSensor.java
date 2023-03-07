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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.JavaFrontend;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.CheckList;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.performance.measure.PerformanceMeasure;

@Phase(name = Phase.Name.PRE)
public class JavaSensor implements Sensor {

  private static final Logger LOG = Loggers.get(JavaSensor.class);

  private static final String PERFORMANCE_MEASURE_ACTIVATION_PROPERTY = "sonar.java.performance.measure";
  private static final String PERFORMANCE_MEASURE_FILE_PATH_PROPERTY = "sonar.java.performance.measure.path";
  private static final String PERFORMANCE_MEASURE_DESTINATION_FILE = "sonar.java.performance.measure.json";

  private final SonarComponents sonarComponents;
  private final FileSystem fs;
  private final JavaResourceLocator javaResourceLocator;
  private final Configuration settings;
  private final NoSonarFilter noSonarFilter;
  @Nullable
  private final Jasper jasper;
  private final PostAnalysisIssueFilter postAnalysisIssueFilter;

  public JavaSensor(SonarComponents sonarComponents, FileSystem fs, JavaResourceLocator javaResourceLocator,
                    Configuration settings, NoSonarFilter noSonarFilter, PostAnalysisIssueFilter postAnalysisIssueFilter) {
    this(sonarComponents, fs, javaResourceLocator, settings, noSonarFilter, postAnalysisIssueFilter, null);
  }

  public JavaSensor(SonarComponents sonarComponents, FileSystem fs, JavaResourceLocator javaResourceLocator,
                    Configuration settings, NoSonarFilter noSonarFilter,
                    PostAnalysisIssueFilter postAnalysisIssueFilter, @Nullable Jasper jasper) {
    this.noSonarFilter = noSonarFilter;
    this.sonarComponents = sonarComponents;
    this.fs = fs;
    this.javaResourceLocator = javaResourceLocator;
    this.settings = settings;
    this.postAnalysisIssueFilter = postAnalysisIssueFilter;
    this.jasper = jasper;
    this.sonarComponents.registerMainCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaChecks());
    this.sonarComponents.registerTestCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getJavaTestChecks());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(Java.KEY).name("JavaSensor");
  }

  @Override
  public void execute(SensorContext context) {
    PerformanceMeasure.Duration sensorDuration = createPerformanceMeasureReport(context);

    sonarComponents.setSensorContext(context);
    sonarComponents.setCheckFilter(createCheckFilter(sonarComponents.isAutoScanCheckFiltering()));

    Measurer measurer = new Measurer(context, noSonarFilter);

    JavaFrontend frontend = new JavaFrontend(getJavaVersion(), sonarComponents, measurer, javaResourceLocator, postAnalysisIssueFilter,
      insertSymbolicExecutionVisitor(sonarComponents.mainChecks()));
    frontend.scan(getSourceFiles(), getTestFiles(), runJasper(context));

    sensorDuration.stop();
  }

  private static UnaryOperator<List<JavaCheck>> createCheckFilter(boolean isAutoScanCheckFiltering) {
    if (isAutoScanCheckFiltering) {
      Set<String> sonarWayRuleKeys = JavaSonarWayProfile.ruleKeys();
      Set<Class<? extends JavaCheck>> notWorkingChecks = CheckList.getJavaChecksNotWorkingForAutoScan();
      return checks -> checks.stream()
        .filter(c -> !(c instanceof SECheck))
        .filter(c -> sonarWayRuleKeys.contains(getKeyFromCheck(c)))
        .filter(c -> !notWorkingChecks.contains(c.getClass()))
        .collect(Collectors.toList());
    } else {
      return UnaryOperator.identity();
    }
  }

  @VisibleForTesting
  static String getKeyFromCheck(JavaCheck check) {
    Rule ruleAnnotation = AnnotationUtils.getAnnotation(check.getClass(), Rule.class);
    return ruleAnnotation != null ? ruleAnnotation.key() : "";
  }

  private static PerformanceMeasure.Duration createPerformanceMeasureReport(SensorContext context) {
    return PerformanceMeasure.reportBuilder()
      .activate(context.config().get(PERFORMANCE_MEASURE_ACTIVATION_PROPERTY).filter("true"::equals).isPresent())
      .toFile(context.config().get(PERFORMANCE_MEASURE_FILE_PATH_PROPERTY)
        .filter(path -> !path.isEmpty())
        .orElseGet(() -> Optional.ofNullable(context.fileSystem().workDir())
          .filter(File::exists)
          .map(file -> file.toPath().resolve(PERFORMANCE_MEASURE_DESTINATION_FILE).toString())
          .orElse(null)))
      .appendMeasurementCost()
      .start("JavaSensor");
  }

  @VisibleForTesting
  static JavaCheck[] insertSymbolicExecutionVisitor(List<JavaCheck> checks) {
    List<SECheck> seChecks = checks.stream()
      .filter(SECheck.class::isInstance)
      .map(SECheck.class::cast)
      .collect(Collectors.toList());
    if (seChecks.isEmpty()) {
      return checks.toArray(new JavaCheck[0]);
    }
    List<JavaCheck> newList = new ArrayList<>(checks);
    // insert an instance of SymbolicExecutionVisitor before the first SECheck
    newList.add(newList.indexOf(seChecks.get(0)), new SymbolicExecutionVisitor(seChecks));
    return newList.toArray(new JavaCheck[0]);
  }

  private Collection<GeneratedFile> runJasper(SensorContext context) {
    if (sonarComponents.isAutoScan()) {
      // for security reasons, do not run jasper to generate code in autoscan mode
      return Collections.emptyList();
    }
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
    JavaVersion javaVersion = settings.get(JavaVersion.SOURCE_VERSION)
      .map(JavaVersionImpl::fromString)
      .orElse(new JavaVersionImpl());
    LOG.info("Configured Java source version (" + JavaVersion.SOURCE_VERSION + "): " + javaVersion);
    return javaVersion;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
