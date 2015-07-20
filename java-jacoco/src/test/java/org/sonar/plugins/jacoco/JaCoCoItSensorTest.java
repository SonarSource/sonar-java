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
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JaCoCoItSensorTest {
  private static File outputDir;
  private static File jacocoExecutionData;

  private JacocoConfiguration configuration;
  private PathResolver pathResolver;
  private JaCoCoItSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath = mock(JavaClasspath.class);

  @BeforeClass
  public static void setUpOutputDir() throws IOException {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(jacocoExecutionData.getParentFile(), "Hello.class"));
  }

  @Before
  public void setUp() {
    configuration = mock(JacocoConfiguration.class);
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    ModuleFileSystem fileSystem = mock(ModuleFileSystem.class);
    pathResolver = mock(PathResolver.class);
    sensor = new JaCoCoItSensor(configuration, perspectives, fileSystem, pathResolver, javaResourceLocator, javaClasspath);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoItSensor");
  }

  @Test
  public void shouldExecuteIfReportPathIsDefined() {
    Project project = mock(Project.class);
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.exec"));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.not.found.exec"));
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void testReadExecutionData() {
    org.sonar.api.resources.File resource = mock(org.sonar.api.resources.File.class);
    when(javaResourceLocator.findResourceByClassName("org/sonar/plugins/jacoco/tests/Hello")).thenReturn(resource);
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    sensor.analyse(project, context);

    verify(context, times(1)).getResource(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_LINES_TO_COVER, 7.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_UNCOVERED_LINES, 3.0)));
    verify(context).saveMeasure(eq(resource),
      argThat(new IsMeasure(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA, "6=1;7=1;8=1;11=1;15=0;16=0;18=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_CONDITIONS_TO_COVER, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_UNCOVERED_CONDITIONS, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_CONDITIONS_BY_LINE, "15=2")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE, "15=0")));
  }

  @Test
  public void doNotSaveMeasureOnResourceWhichDoesntExistInTheContext() {
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    when(context.getResource(any(Resource.class))).thenReturn(null);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));

    sensor.analyse(project, context);

    verify(context, never()).saveMeasure(any(Resource.class), any(Measure.class));
  }

}
