/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.jacoco;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.util.Collection;

public class JaCoCoItSensor implements Sensor {
  private final JacocoConfiguration configuration;
  private final ResourcePerspectives perspectives;
  private final ModuleFileSystem fileSystem;
  private final PathResolver pathResolver;
  private final JavaResourceLocator javaResourceLocator;
  private final JavaClasspath javaClasspath;

  public JaCoCoItSensor(JacocoConfiguration configuration, ResourcePerspectives perspectives, ModuleFileSystem fileSystem, PathResolver pathResolver,
                        JavaResourceLocator javaResourceLocator, JavaClasspath javaClasspath) {
    this.configuration = configuration;
    this.perspectives = perspectives;
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
    this.javaResourceLocator = javaResourceLocator;
    this.javaClasspath = javaClasspath;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    File report = pathResolver.relativeFile(fileSystem.baseDir(), configuration.getItReportPath());
    boolean foundReport = report.exists() && report.isFile();
    boolean shouldExecute = configuration.shouldExecuteOnProject(foundReport);
    if(!foundReport && shouldExecute) {
      JaCoCoExtensions.LOG.info("JaCoCoItSensor: JaCoCo IT report not found.");
    }
    return shouldExecute;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    new ITAnalyzer(perspectives).analyse(project, context);
  }

  class ITAnalyzer extends AbstractAnalyzer {
    public ITAnalyzer(ResourcePerspectives perspectives) {
      super(perspectives, fileSystem, pathResolver, javaResourceLocator, javaClasspath);
    }

    @Override
    protected String getReportPath(Project project) {
      return configuration.getItReportPath();
    }

    @Override
    protected void saveMeasures(SensorContext context, Resource resource, Collection<Measure> measures) {
      for (Measure measure : measures) {
        Measure itMeasure = convertForIT(measure);
        if (itMeasure != null) {
          context.saveMeasure(resource, itMeasure);
        }
      }
    }

    private Measure convertForIT(Measure measure) {
      Measure itMeasure = null;
      if (CoreMetrics.LINES_TO_COVER.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_LINES_TO_COVER, measure.getValue());

      } else if (CoreMetrics.UNCOVERED_LINES.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_UNCOVERED_LINES, measure.getValue());

      } else if (CoreMetrics.COVERAGE_LINE_HITS_DATA.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA, measure.getData());

      } else if (CoreMetrics.CONDITIONS_TO_COVER.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_CONDITIONS_TO_COVER, measure.getValue());

      } else if (CoreMetrics.UNCOVERED_CONDITIONS.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_UNCOVERED_CONDITIONS, measure.getValue());

      } else if (CoreMetrics.COVERED_CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE, measure.getData());

      } else if (CoreMetrics.CONDITIONS_BY_LINE.equals(measure.getMetric())) {
        itMeasure = new Measure(CoreMetrics.IT_CONDITIONS_BY_LINE, measure.getData());
      }
      return itMeasure;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
