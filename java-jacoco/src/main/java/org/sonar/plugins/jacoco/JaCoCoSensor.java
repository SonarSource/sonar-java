/*
 * SonarQube Java
 * Copyright (C) 2010-2019 SonarSource SA
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
package org.sonar.plugins.jacoco;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Configuration;
import org.sonar.java.AnalysisWarningsWrapper;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.sonar.plugins.jacoco.JaCoCoExtensions.IT_REPORT_PATH_PROPERTY;
import static org.sonar.plugins.jacoco.JaCoCoExtensions.LOG;
import static org.sonar.plugins.jacoco.JaCoCoExtensions.REPORT_MISSING_FORCE_ZERO;
import static org.sonar.plugins.jacoco.JaCoCoExtensions.REPORT_PATHS_PROPERTY;
import static org.sonar.plugins.jacoco.JaCoCoExtensions.REPORT_PATH_PROPERTY;

public class JaCoCoSensor implements Sensor {

  private static final String JACOCO_MERGED_FILENAME = "jacoco-merged.exec";
  private final ResourcePerspectives perspectives;
  private final JavaResourceLocator javaResourceLocator;
  private final JavaClasspath javaClasspath;
  static final String JACOCO_XML_PROPERTY = "sonar.coverage.jacoco.xmlReportPaths";
  private static final String[] JACOCO_XML_DEFAULT_PATHS = {"target/site/jacoco/jacoco.xml", "build/reports/jacoco/test/jacocoTestReport.xml"};
  private AnalysisWarningsWrapper analysisWarnings;

  /**
   * Used by SQ < 7.4 where AnalysisWarning is not yet available. Should be dropped once SQ > 7.4 is the minimal version
   */
  public JaCoCoSensor(ResourcePerspectives perspectives, JavaResourceLocator javaResourceLocator, JavaClasspath javaClasspath) {
    this(perspectives, javaResourceLocator, javaClasspath, AnalysisWarningsWrapper.NOOP_ANALYSIS_WARNINGS);
  }

  public JaCoCoSensor(ResourcePerspectives perspectives, JavaResourceLocator javaResourceLocator, JavaClasspath javaClasspath, AnalysisWarningsWrapper analysisWarnings) {
    this.perspectives = perspectives;
    this.javaResourceLocator = javaResourceLocator;
    this.javaClasspath = javaClasspath;
    this.analysisWarnings = analysisWarnings;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage("java")
      .name("JaCoCoSensor");
  }

  @Override
  public void execute(SensorContext context) {
    Configuration config = context.config();
    if (config.hasKey(REPORT_MISSING_FORCE_ZERO)) {
      addAnalysisWarning("Property '%s' is deprecated and its value will be ignored.", REPORT_MISSING_FORCE_ZERO);
    }
    List<Report> reports = getReportPaths(context);
    if (reports.isEmpty()) {
      return;
    }
    boolean hasXmlReport = hasXmlReport(context);
    warnAboutDeprecatedProperties(config, reports, hasXmlReport);
    if (hasXmlReport) {
      LOG.debug("JaCoCo XML report found, skipping processing of binary JaCoCo exec report.", JACOCO_XML_PROPERTY);
      return;
    }
    File reportMerged = mergeJacocoReports(context, reports);
    new UnitTestAnalyzer(reportMerged, perspectives, javaResourceLocator, javaClasspath, analysisWarnings).analyse(context);
  }

  private static File mergeJacocoReports(SensorContext context, List<Report> reportPaths) {
    // Merge JaCoCo reports
    File reportMerged;
    if (reportPaths.size() == 1) {
      reportMerged = reportPaths.get(0).file;
    } else {
      reportMerged = new File(context.fileSystem().workDir(), JACOCO_MERGED_FILENAME);
      reportMerged.getParentFile().mkdirs();
      File[] reports = reportPaths.stream().map(report -> report.file).toArray(File[]::new);
      JaCoCoReportMerger.mergeReports(reportMerged, reports);
    }
    return reportMerged;
  }

  private void warnAboutDeprecatedProperties(Configuration config, List<Report> reports, boolean hasXmlReport) {
    Set<String> usedProperties = reports.stream().map(report -> report.propertyKey).collect(Collectors.toSet());
    usedProperties.forEach(deprecatedProperty -> {
      if (!hasXmlReport) {
        addAnalysisWarning("Property '%s' is deprecated (JaCoCo binary format). '%s' should be used instead (JaCoCo XML format)." +
          " Please check that the JaCoCo plugin is installed on your SonarQube Instance.", deprecatedProperty, JACOCO_XML_PROPERTY);
      } else if (config.hasKey(deprecatedProperty)) {
        // only log for those properties which were set explicitly
        LOG.info("Both '{}' and '{}' were set. '{}' is deprecated therefore, only '{}' will be taken into account." +
            " Please check that the JaCoCo plugin is installed on your SonarQube Instance.",
          deprecatedProperty, JACOCO_XML_PROPERTY, deprecatedProperty, JACOCO_XML_PROPERTY);
      }
    });
  }

  private static boolean hasXmlReport(SensorContext context) {
    return context.config().hasKey(JACOCO_XML_PROPERTY) ||
      Arrays.stream(JACOCO_XML_DEFAULT_PATHS).map(path -> context.fileSystem().baseDir().toPath().resolve(path)).anyMatch(Files::isRegularFile);
  }

  private void addAnalysisWarning(String format, Object... args) {
    String msg = String.format(format, args);
    LOG.warn(msg);
    analysisWarnings.addUnique(msg);
  }

  private static List<Report> getReportPaths(SensorContext context) {
    List<Report> reports = new ArrayList<>();
    Configuration settings = context.config();
    FileSystem fs = context.fileSystem();
    for (String reportPath : settings.getStringArray(REPORT_PATHS_PROPERTY)) {
      File report = fs.resolvePath(reportPath);
      if (!report.isFile()) {
        if (settings.hasKey(REPORT_PATHS_PROPERTY)) {
          LOG.info("JaCoCo report not found: '{}'", reportPath);
        }
      } else {
        reports.add(new Report(report, REPORT_PATHS_PROPERTY));
      }
    }
    getReport(settings, fs, REPORT_PATH_PROPERTY, "JaCoCo UT report not found: '{}'").ifPresent(file -> reports.add(new Report(file, REPORT_PATH_PROPERTY)));
    getReport(settings, fs, IT_REPORT_PATH_PROPERTY, "JaCoCo IT report not found: '{}'").ifPresent(file -> reports.add(new Report(file, IT_REPORT_PATH_PROPERTY)));
    return reports;
  }

  private static Optional<File> getReport(Configuration settings, FileSystem fs, String reportPathPropertyKey, String msg) {
    Optional<String> reportPathProp = settings.get(reportPathPropertyKey);
    if (reportPathProp.isPresent()) {
      String reportPathProperty = reportPathProp.get();
      File report = fs.resolvePath(reportPathProperty);
      if (!report.isFile()) {
        LOG.info(msg, reportPathProperty);
      } else {
        return Optional.of(report);
      }
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  static class Report {
    File file;
    String propertyKey;

    Report(File file, String propertyKey) {
      this.file = file;
      this.propertyKey = propertyKey;
    }
  }
}
