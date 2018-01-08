/*
 * SonarQube Java
 * Copyright (C) 2010-2018 SonarSource SA
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Configuration;
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

  public JaCoCoSensor(ResourcePerspectives perspectives, JavaResourceLocator javaResourceLocator, JavaClasspath javaClasspath) {
    this.perspectives = perspectives;
    this.javaResourceLocator = javaResourceLocator;
    this.javaClasspath = javaClasspath;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage("java").name("JaCoCoSensor");
  }

  @Override
  public void execute(SensorContext context) {
    if (context.config().hasKey(REPORT_MISSING_FORCE_ZERO)) {
      LOG.warn("Property '{}' is deprecated and its value will be ignored.", REPORT_MISSING_FORCE_ZERO);
    }
    Set<File> reportPaths = getReportPaths(context);
    if (reportPaths.isEmpty()) {
      return;
    }
    // Merge JaCoCo reports
    File reportMerged;
    if(reportPaths.size() == 1) {
      reportMerged = reportPaths.iterator().next();
    } else {
      reportMerged = new File(context.fileSystem().workDir(), JACOCO_MERGED_FILENAME);
      reportMerged.getParentFile().mkdirs();
      JaCoCoReportMerger.mergeReports(reportMerged, reportPaths.toArray(new File[0]));
    }
    new UnitTestAnalyzer(reportMerged, perspectives, javaResourceLocator, javaClasspath).analyse(context);
  }

  private static Set<File> getReportPaths(SensorContext context) {
    Set<File> reportPaths = new HashSet<>();
    Configuration settings = context.config();
    FileSystem fs = context.fileSystem();
    for (String reportPath : settings.getStringArray(REPORT_PATHS_PROPERTY)) {
      File report = fs.resolvePath(reportPath);
      if (!report.isFile()) {
        if (settings.hasKey(REPORT_PATHS_PROPERTY)) {
          LOG.info("JaCoCo report not found: '{}'", reportPath);
        }
      } else {
        reportPaths.add(report);
      }
    }
    getReport(settings, fs, REPORT_PATH_PROPERTY, "JaCoCo UT report not found: '{}'").ifPresent(reportPaths::add);
    getReport(settings, fs, IT_REPORT_PATH_PROPERTY, "JaCoCo IT report not found: '{}'").ifPresent(reportPaths::add);
    return reportPaths;
  }

  private static Optional<File> getReport(Configuration settings, FileSystem fs, String reportPathPropertyKey, String msg) {
    Optional<String> reportPathProp = settings.get(reportPathPropertyKey);
    if (reportPathProp.isPresent()) {
      warnUsageOfDeprecatedProperty(settings, reportPathPropertyKey);
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

  private static void warnUsageOfDeprecatedProperty(Configuration settings, String reportPathProperty) {
    if (!settings.hasKey(REPORT_PATHS_PROPERTY)) {
      LOG.warn("Property '{}' is deprecated. Please use '{}' instead.", reportPathProperty, REPORT_PATHS_PROPERTY);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
