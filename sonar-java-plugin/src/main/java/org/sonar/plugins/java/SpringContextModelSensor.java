/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.check.Rule;
import org.sonar.java.GeneratedCheckList;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.spring.SpringContextCheck;
import org.sonar.java.checks.spring.SpringContextChecks;
import org.sonar.java.checks.spring.SpringContextIssue;
import org.sonar.java.exceptions.ApiMismatchException;
import org.sonar.java.jsp.Jasper;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.SpringContextGatheringModel;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.model.springcontext.SpringContextModelMetrics;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.telemetry.TelemetryKey;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A post-phase {@link ProjectSensor} that builds and checks the shared {@link SpringContextModel}.
 *
 * <p>This sensor runs after the main Java analysis phase, ensuring that all
 * {@link org.sonar.java.model.springcontext.SpringContextModelGatherer} visitors have finished collecting
 * project data before the model is built and consumed by downstream components.
 *
 * <p>The model is built once from the complete gathering model before project checks run.
 */
@Phase(name = Phase.Name.POST)
@DependedUpon(value = "CollectSpringContextBeforeSendingTelemetry")
public class SpringContextModelSensor implements ProjectSensor {

  private final Telemetry telemetry;
  private final SpringContextGatheringModel gatheringModel;

  public SpringContextModelSensor(Telemetry telemetry, SpringContextGatheringModel gatheringModel) {
    this.telemetry = telemetry;
    this.gatheringModel = gatheringModel;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguages(Java.KEY, Jasper.JSP_LANGUAGE_KEY).name("Java SpringContextModelSensor");
  }

  @Override
  public void execute(SensorContext context) {
    if (isFullProjectAnalysis(context)) {
      gatheringModel.removeUnvisitedFiles();
    }
    long buildingStartTime = System.nanoTime();
    SpringContextModel springContextModel = SpringContextModel.of(gatheringModel);
    telemetry.aggregateAsCounter(
      TelemetryKey.JAVA_SPRING_CONTEXT_MODEL_GATHERING_TIME_MS,
      TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - buildingStartTime));
    recordSpringTelemetry(springContextModel);
    Path path = JavaSensor.springContextGatheringModelOutputPath(context, context.fileSystem().baseDir());
    saveSpringContextGatheringModel(path, gatheringModel);

    long startTime = System.nanoTime();
    try {
      for (SpringContextCheck check : SpringContextChecks.getAllChecks()) {
        reportIssues(context, check, springContextModel);
      }
    } finally {
      telemetry.aggregateAsCounter(
        TelemetryKey.JAVA_SPRING_CONTEXT_CHECKS_TIME_MS,
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
    }
  }

  private static boolean isFullProjectAnalysis(SensorContext context) {
    try {
      return context.runtime().getProduct() == SonarProduct.SONARQUBE && !SonarComponents.canSkipUnchangedFiles(context);
    } catch (ApiMismatchException e) {
      return false;
    }
  }

  /**
   * Replaces the persisted Spring context data with the model computed during the last analysis.
   */
  @VisibleForTesting
  static void saveSpringContextGatheringModel(Path path, SpringContextGatheringModel model) {
    try {
      Path directory = path.toAbsolutePath().getParent();
      Files.createDirectories(directory);
      Path temporaryFile = Files.createTempFile(directory, "spring-context-model-", ".tmp");
      try (var writer = Files.newBufferedWriter(temporaryFile, UTF_8)) {
        new Gson().toJson(model, writer);
        Files.move(temporaryFile, path, REPLACE_EXISTING);
      } finally {
        Files.deleteIfExists(temporaryFile);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to save Spring context model to " + path, e);
    }
  }

  private static void reportIssues(SensorContext context, SpringContextCheck check, SpringContextModel springContextModel) {
    RuleKey ruleKey = RuleKey.of(GeneratedCheckList.REPOSITORY_KEY, check.getClass().getAnnotation(Rule.class).key());
    if (context.activeRules().find(ruleKey) == null) {
      // Rule not active in the quality profile: skip running the check, its issues would be discarded anyway.
      return;
    }
    for (SpringContextIssue issue : check.execute(springContextModel)) {
      BeanLocation location = issue.location();
      if (location.inputFile() == null) {
        continue;
      }
      AnalyzerMessage.TextSpan span = location.mainLocation();
      NewIssue newIssue = context.newIssue().forRule(ruleKey);
      newIssue.at(newIssue.newLocation()
        .on(location.inputFile())
        .at(location.inputFile().newRange(span.startLine, span.startCharacter, span.endLine, span.endCharacter))
        .message(issue.message()));
      newIssue.save();
    }
  }

  private void recordSpringTelemetry(SpringContextModel model) {
    var metrics = SpringContextModelMetrics.of(model);
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_BEAN_COUNT, metrics.beanCount());
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_BEAN_NAME_COUNT, metrics.beanNameCount());
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_INJECTION_POINT_COUNT, metrics.injectionPointCount());
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_COMPONENT_SCAN_PACKAGE_COUNT, metrics.componentScanPackageCount());
    telemetry.aggregateAsCounter(TelemetryKey.JAVA_SPRING_CONTEXT_MODEL_SIZE_BYTES, metrics.estimatedSizeInBytes());
  }
}
