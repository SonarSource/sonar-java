/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java;

import java.io.File;
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
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.GeneratedCheckList;
import org.sonar.java.JavaFrontend;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.performance.measure.PerformanceMeasure;

import static org.sonar.api.rules.RuleAnnotationUtils.getRuleKey;
import static org.sonar.java.TelemetryKey.JAVA_SCANNER_APP;
import static org.sonar.java.telemetry.TelemetryKey.JAVA_LANGUAGE_VERSION;
import static org.sonar.java.telemetry.TelemetryKey.JAVA_MODULE_COUNT;

@Phase(name = Phase.Name.PRE)
@DependedUpon("org.sonar.plugins.java.JavaSensor")
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
  private final Telemetry telemetry;

  public JavaSensor(SonarComponents sonarComponents, FileSystem fs, JavaResourceLocator javaResourceLocator,
    Configuration settings, NoSonarFilter noSonarFilter, PostAnalysisIssueFilter postAnalysisIssueFilter, Telemetry telemetry) {
    this(sonarComponents, fs, javaResourceLocator, settings, noSonarFilter, postAnalysisIssueFilter, null, telemetry);
  }

  public JavaSensor(SonarComponents sonarComponents, FileSystem fs, JavaResourceLocator javaResourceLocator,
    Configuration settings, NoSonarFilter noSonarFilter,
    PostAnalysisIssueFilter postAnalysisIssueFilter, @Nullable Jasper jasper, Telemetry telemetry) {
    this.noSonarFilter = noSonarFilter;
    this.sonarComponents = sonarComponents;
    this.fs = fs;
    this.javaResourceLocator = javaResourceLocator;
    this.settings = settings;
    this.postAnalysisIssueFilter = postAnalysisIssueFilter;
    this.jasper = jasper;
    this.telemetry = telemetry;
    this.sonarComponents.registerMainChecks(GeneratedCheckList.REPOSITORY_KEY, GeneratedCheckList.getJavaChecks());
    this.sonarComponents.registerTestChecks(GeneratedCheckList.REPOSITORY_KEY, GeneratedCheckList.getJavaTestChecks());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguages(Java.KEY, Jasper.JSP_LANGUAGE_KEY).name("JavaSensor");
  }

  @Override
  public void execute(SensorContext context) {
    PerformanceMeasure.Duration sensorDuration = createPerformanceMeasureReport(context);

    sonarComponents.setSensorContext(context);
    sonarComponents.setCheckFilter(createCheckFilter(sonarComponents.isAutoScanCheckFiltering()));

    Measurer measurer = new Measurer(context, noSonarFilter);

    JavaVersion javaVersion = getJavaVersion();
    telemetry.aggregateAsSortedSet(JAVA_LANGUAGE_VERSION, javaVersion.toString());
    telemetry.aggregateAsCounter(JAVA_MODULE_COUNT, 1L);

    telemetry.aggregateAsSortedSet(JAVA_SCANNER_APP, settings.get("sonar.scanner.app").orElse("none"));

    JavaFrontend frontend = new JavaFrontend(javaVersion, sonarComponents, measurer, javaResourceLocator, postAnalysisIssueFilter,
      sonarComponents.mainChecks().toArray(new JavaCheck[0]));
    frontend.scan(getSourceFiles(), getTestFiles(), runJasper(context));

    sensorDuration.stop();
  }

  private UnaryOperator<List<JavaCheck>> createCheckFilter(boolean isAutoScanCheckFiltering) {
    if (isAutoScanCheckFiltering) {
      Set<RuleKey> autoScanCompatibleRules = new HashSet<>(JavaSonarWayProfile.sonarJavaSonarWayRuleKeys());

      GeneratedCheckList.getJavaChecksNotWorkingForAutoScan().stream()
        .map(checkClass -> RuleKey.of(GeneratedCheckList.REPOSITORY_KEY, getRuleKey(checkClass)))
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

    JavaVersion javaVersion = JavaVersionImpl.fromString(javaVersionAsString.get(), enablePreviewAsString);
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
