/*
 * SonarQube Java
 * Copyright (C) 2010-2017 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;

public class JaCoCoItSensor implements Sensor {
  private final JacocoConfiguration configuration;
  private final ResourcePerspectives perspectives;
  private final FileSystem fileSystem;
  private final PathResolver pathResolver;
  private final JavaResourceLocator javaResourceLocator;
  private final JavaClasspath javaClasspath;

  public JaCoCoItSensor(JacocoConfiguration configuration, ResourcePerspectives perspectives, FileSystem fileSystem, PathResolver pathResolver,
                        JavaResourceLocator javaResourceLocator, JavaClasspath javaClasspath) {
    this.configuration = configuration;
    this.perspectives = perspectives;
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
    this.javaResourceLocator = javaResourceLocator;
    this.javaClasspath = javaClasspath;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage("java").name("JaCoCoItSensor");
  }

  @Override
  public void execute(SensorContext context) {
    if(shouldExecuteOnProject()) {
      new ITAnalyzer().analyse(context);
    }
  }

  @VisibleForTesting
  boolean shouldExecuteOnProject() {
    File report = pathResolver.relativeFile(fileSystem.baseDir(), configuration.getItReportPath());
    boolean foundReport = report.isFile();
    if(!foundReport) {
      JaCoCoExtensions.LOG.info("JaCoCoItSensor: JaCoCo IT report not found: "+report.getPath());
    }
    return configuration.shouldExecuteOnProject(foundReport);
  }

  class ITAnalyzer extends AbstractAnalyzer {
    public ITAnalyzer() {
      super(perspectives, javaResourceLocator, javaClasspath);
    }

    @Override
    protected CoverageType coverageType() {
      return CoverageType.IT;
    }

    @Override
    protected File getReport() {
      return pathResolver.relativeFile(fileSystem.baseDir(), configuration.getItReportPath());
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
