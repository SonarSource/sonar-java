/*
 * SonarQube Java
 * Copyright (C) 2010-2016 SonarSource SA
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
package org.sonar.plugins.jacoco;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.sonar.plugins.jacoco.JacocoConstants.IT_REPORT_PATH_PROPERTY;
import static org.sonar.plugins.jacoco.JacocoConstants.REPORT_MISSING_FORCE_ZERO;
import static org.sonar.plugins.jacoco.JacocoConstants.REPORT_PATHS_PROPERTY;
import static org.sonar.plugins.jacoco.JacocoConstants.REPORT_PATH_PROPERTY;

public class JaCoCoSensor implements Sensor {

  private static final Logger LOG = Loggers.get(JaCoCoSensor.class);

  public static final String JACOCO_MERGED = "jacoco-merged.exec";

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
    descriptor.onlyOnLanguage("java").name("JaCoCo");
  }

  @Override
  public void execute(SensorContext context) {
    if (context.getSonarQubeVersion().isGreaterThanOrEqual(JacocoConstants.SQ_6_2)) {
      doExecute(context);
    } else {
      doExecutePrior6dot2(context);
    }
  }

  private void doExecute(SensorContext context) {
    List<File> reportPaths = getReportPaths(context);
    if (reportPaths.isEmpty()) {
      return;
    }
    // Do not let platform merge the reports since JaCoCo is better at merging overall coverage
    File reportMerged = new File(context.fileSystem().workDir(), JACOCO_MERGED);
    reportMerged.getParentFile().mkdirs();
    JaCoCoReportMerger.mergeReports(reportMerged, reportPaths.toArray(new File[0]));
    new UnitTestsAnalyzer(reportMerged).analyse(context);
  }

  private void doExecutePrior6dot2(SensorContext context) {
    Settings settings = context.settings();
    String reportPath = settings.getString(REPORT_PATH_PROPERTY);
    File report = context.fileSystem().resolvePath(reportPath);
    if (!report.isFile()) {
      if (settings.hasKey(REPORT_PATH_PROPERTY)) {
        LOG.info("JaCoCo report not found: '{}'", reportPath);
      }
      if (settings.getBoolean(REPORT_MISSING_FORCE_ZERO)) {
        LOG.info("Project coverage is set to 0% as no JaCoCo execution data has been dumped");
      }
    }
    if (report.isFile() || settings.getBoolean(REPORT_MISSING_FORCE_ZERO)) {
      new UnitTestsAnalyzer(report).analyse(context);
    }
  }

  public List<File> getReportPaths(SensorContext context) {
    List<File> reportPaths = new ArrayList<>();
    Settings settings = context.settings();
    FileSystem fs = context.fileSystem();
    if (settings.hasKey(REPORT_PATH_PROPERTY)) {
      LOG.warn("Property '{}' is deprecated. Please use '{}' instead.", REPORT_PATH_PROPERTY, REPORT_PATHS_PROPERTY);
      File report = fs.resolvePath(settings.getString(REPORT_PATH_PROPERTY));
      if (!report.isFile()) {
        LOG.info("JaCoCo UT report not found: '{}'", settings.getString(REPORT_PATH_PROPERTY));
      } else {
        reportPaths.add(report);
      }
    }
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
    if (settings.hasKey(IT_REPORT_PATH_PROPERTY)) {
      LOG.warn("Property '{}' is deprecated. Please use '{}' instead.", IT_REPORT_PATH_PROPERTY, REPORT_PATHS_PROPERTY);
      File report = fs.resolvePath(settings.getString(IT_REPORT_PATH_PROPERTY));
      if (!report.isFile()) {
        LOG.info("JaCoCo IT report not found: '{}'", settings.getString(IT_REPORT_PATH_PROPERTY));
      } else {
        reportPaths.add(report);
      }
    }
    return reportPaths;
  }

  class UnitTestsAnalyzer extends AbstractAnalyzer {
    private File report;

    public UnitTestsAnalyzer(File report) {
      super(perspectives, javaResourceLocator, javaClasspath);
      this.report = report;
    }

    @Override
    protected CoverageType coverageType() {
      return CoverageType.UNIT;
    }

    @Override
    protected File getReportPath() {
      return report;
    }

  }

}
