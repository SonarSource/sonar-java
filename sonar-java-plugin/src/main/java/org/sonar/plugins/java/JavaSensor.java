/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.JavaFrontend;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
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

import static org.sonar.api.rules.RuleAnnotationUtils.getRuleKey;

@Phase(name = Phase.Name.PRE)
public class JavaSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(JavaSensor.class);

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
    this.sonarComponents.registerMainChecks(CheckList.REPOSITORY_KEY, CheckList.getJavaChecks());
    this.sonarComponents.registerTestChecks(CheckList.REPOSITORY_KEY, CheckList.getJavaTestChecks());
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

  private UnaryOperator<List<JavaCheck>> createCheckFilter(boolean isAutoScanCheckFiltering) {
    if (isAutoScanCheckFiltering) {
      Set<RuleKey> autoScanCompatibleRules = new HashSet<>(JavaSonarWayProfile.sonarJavaSonarWayRuleKeys());

      CheckList.getJavaChecksNotWorkingForAutoScan().stream()
        .map(checkClass -> RuleKey.of(CheckList.REPOSITORY_KEY, getRuleKey(checkClass)))
        .forEach(autoScanCompatibleRules::remove);

      autoScanCompatibleRules.addAll(sonarComponents.getAdditionalAutoScanCompatibleRuleKeys());

      return checks -> checks.stream()
        .filter(check -> sonarComponents.getRuleKey(check).map(autoScanCompatibleRules::contains).orElse(false))
        .toList();
    } else {
      return UnaryOperator.identity();
    }
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
      .toList();
    if (seChecks.isEmpty()) {
      LOG.info("No rules with 'symbolic-execution' tag were enabled,"
        + " the Symbolic Execution Engine will not run during the analysis.");
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
    Optional<String> javaVersionAsString = settings.get(JavaVersion.SOURCE_VERSION);
    if (!javaVersionAsString.isPresent()) {
      return new JavaVersionImpl();
    }
    String enablePreviewAsString = settings.get(JavaVersion.ENABLE_PREVIEW).orElse("false");

    JavaVersion javaVersion = JavaVersionImpl.fromStrings(javaVersionAsString.get(), enablePreviewAsString);
    if (javaVersion.arePreviewFeaturesEnabled() && javaVersion.asInt() < JavaVersionImpl.MAX_SUPPORTED) {
      LOG.warn("sonar.java.enablePreview is set but will be discarded as the Java version is less than the max" +
        " supported version ({} < {})", javaVersion.asInt(), JavaVersionImpl.MAX_SUPPORTED);
      javaVersion = new JavaVersionImpl(javaVersion.asInt(), false);
    }
    LOG.info("Configured Java source version ({}): {}, preview features enabled ({}): {}",
      JavaVersion.SOURCE_VERSION, javaVersion.asInt(), JavaVersion.ENABLE_PREVIEW, javaVersion.arePreviewFeaturesEnabled());
    return javaVersion;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
