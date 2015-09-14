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

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.IsMeasure;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JaCoCoOverallSensorTest {

  private JacocoConfiguration configuration;
  private SensorContext context;
  private ModuleFileSystem fileSystem;
  private PathResolver pathResolver;
  private Project project;
  private ResourcePerspectives perspectives;
  private JaCoCoOverallSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath = mock(JavaClasspath.class);

  @Before
  public void before() {
    configuration = mock(JacocoConfiguration.class);
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    context = mock(SensorContext.class);
    fileSystem = mock(ModuleFileSystem.class);
    pathResolver = mock(PathResolver.class);
    project = mock(Project.class);
    perspectives = mock(ResourcePerspectives.class);
    sensor = new JaCoCoOverallSensor(configuration, perspectives, fileSystem, pathResolver, javaResourceLocator, javaClasspath);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoOverallSensor");
  }

  @Test
  public void should_execute_if_both_report_exists() {
    Project project = mock(Project.class);
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.exec"));
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.exec"));
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(configuration.getReportPath()).thenReturn("ut.exec");

    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void execute_when_it_report_does_not_exists() {
    Project project = mock(Project.class);
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.exec"));
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.not.found.exec"));
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(configuration.getReportPath()).thenReturn("ut.exec");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void execute_when_ut_report_does_not_exists() {
    Project project = mock(Project.class);
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.not.found.exec"));
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.exec"));
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(configuration.getReportPath()).thenReturn("ut.exec");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_save_measures() throws IOException {
    Resource resource = analyseReports("ut.exec", "it.exec");
    verifyOverallMetrics(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "3=1;6=1;7=1;10=1;11=1;14=1;15=1;17=1;18=1;20=1;23=0;24=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 0.0)));
  }

  @Test
  public void should_save_measures_when_it_report_is_not_found() throws IOException {
    Resource resource = analyseReports("ut.exec", "it.not.found.exec");
    verifyOverallMetrics(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 6.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "3=1;6=1;7=1;10=0;11=0;14=1;15=1;17=0;18=0;20=1;23=0;24=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 1.0)));
  }

  @Test
  public void should_save_measures_when_ut_report_is_not_found() throws IOException {
    Resource resource = analyseReports("ut.not.found.exec", "it.exec");
    verifyOverallMetrics(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 5.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "3=1;6=0;7=0;10=1;11=1;14=1;15=0;17=1;18=1;20=1;23=0;24=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 1.0)));
  }


  @Test
  public void should_save_measures_when_no_reports_and_force_property() throws IOException {
    Resource resource = analyseReports("ut.not.found.exec", "it.not.found.exec");
    verifyOverallMetrics(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 12.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "3=0;6=0;7=0;10=0;11=0;14=0;15=0;17=0;18=0;20=0;23=0;24=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 2.0)));
  }

  private void verifyOverallMetrics(Resource resource) {
    verify(context, times(1)).getResource(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_LINES_TO_COVER, 12.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_TO_COVER, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, "14=2")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, (String) null)));
  }

  private Resource analyseReports(String utReport, String itReport) throws IOException {
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    File to = new File(outputDir, "HelloWorld.class");
    Files.copy(TestUtils.getResource("HelloWorld.class.toCopy"), to);
    org.sonar.api.resources.File resource = mock(org.sonar.api.resources.File.class);

    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(javaResourceLocator.findResourceByClassName("com/sonar/coverages/HelloWorld")).thenReturn(resource);
    when(configuration.getReportPath()).thenReturn(utReport);
    when(configuration.getItReportPath()).thenReturn(itReport);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), eq(utReport))).thenReturn(new File(outputDir, utReport));
    when(pathResolver.relativeFile(any(File.class), eq(itReport))).thenReturn(new File(outputDir, itReport));
    when(pathResolver.relativeFile(any(File.class), eq(new File("target/sonar/jacoco-overall.exec").getAbsolutePath()))).thenReturn(new File("target/sonar/jacoco-overall.exec"));
    when(fileSystem.workingDir()).thenReturn(new File("target/sonar"));

    sensor.analyse(project, context);
    return resource;
  }
}
