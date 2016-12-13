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

public class JaCoCoItSensor implements Sensor {

  private static final Logger LOG = Loggers.get(JaCoCoItSensor.class);

  private final ResourcePerspectives perspectives;
  private final JavaResourceLocator javaResourceLocator;
  private final JavaClasspath javaClasspath;

  public JaCoCoItSensor(ResourcePerspectives perspectives, JavaResourceLocator javaResourceLocator, JavaClasspath javaClasspath) {
    this.perspectives = perspectives;
    this.javaResourceLocator = javaResourceLocator;
    this.javaClasspath = javaClasspath;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage("java").name("JaCoCoIt");
  }

  @Override
  public void execute(SensorContext context) {
    Settings settings = context.settings();
    String reportPath = settings.getString(IT_REPORT_PATH_PROPERTY);
    File itReport = context.fileSystem().resolvePath(reportPath);
    if (!itReport.isFile()) {
      if (settings.hasKey(IT_REPORT_PATH_PROPERTY)) {
        LOG.info("JaCoCo IT report not found: '{}'", reportPath);
      }
      return;
    }
    new ITAnalyzer(itReport).analyse(context);
  }

  class ITAnalyzer extends AbstractAnalyzer {
    private final File itReport;

    public ITAnalyzer(File itReport) {
      super(perspectives, javaResourceLocator, javaClasspath);
      this.itReport = itReport;
    }

    @Override
    protected CoverageType coverageType() {
      return CoverageType.IT;
    }

    @Override
    protected File getReportPath() {
      return itReport;
    }
  }
}
