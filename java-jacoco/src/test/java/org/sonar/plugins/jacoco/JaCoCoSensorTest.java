/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JaCoCoSensorTest {

  private File jacocoExecutionData;
  private File outputDir;
  private JacocoConfiguration configuration;
  private ResourcePerspectives perspectives;
  private SensorContext context;
  private ModuleFileSystem fileSystem;
  private PathResolver pathResolver;
  private Project project;
  private JaCoCoSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);

  @Before
  public void setUp() throws Exception {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(jacocoExecutionData.getParentFile(), "Hello.class"));

    context = mock(SensorContext.class);
    fileSystem = mock(ModuleFileSystem.class);
    pathResolver = mock(PathResolver.class);
    project = mock(Project.class);

    configuration = mock(JacocoConfiguration.class);
    perspectives = mock(ResourcePerspectives.class);
    sensor = new JaCoCoSensor(configuration, perspectives, fileSystem, pathResolver, javaResourceLocator);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoSensor");
  }

  @Test
  public void should_depend_on_surefire() {
    assertThat(sensor.dependsOnSurefireSensors()).isEqualTo("surefire-java");
  }

  @Test
  public void should_execute_if_report_exists() {
    Project project = mock(Project.class);
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.exec"));
    when(configuration.getReportPath()).thenReturn("ut.exec");
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.not.found.exec"));
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void test_read_execution_data() {
    JavaFile resource = new JavaFile("org.sonar.plugins.jacoco.tests.Hello");
    when(javaResourceLocator.findResourceByClassName("org/sonar/plugins/jacoco/tests/Hello")).thenReturn(resource);
    when(context.getResource(any(Resource.class))).thenReturn(resource);

    when(fileSystem.binaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    sensor.analyse(project, context);

    verify(context, times(1)).getResource(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 7.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 3.0)));
    verify(context).saveMeasure(eq(resource),
        argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "6=1;7=1;8=1;11=1;15=0;16=0;18=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.CONDITIONS_BY_LINE, "15=2")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "15=0")));
  }

  @Test
  public void test_read_execution_data_for_lines_covered_by_tests() throws IOException {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest2/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");
    Files.copy(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest2/org/example/App.class.toCopy"),
        new File(jacocoExecutionData.getParentFile(), "/org/example/App.class"));

    JavaFile resource = new JavaFile("org.example.App");
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(fileSystem.binaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    MutableTestable testAbleFile = mock(MutableTestable.class);
    when(perspectives.as(eq(MutableTestable.class), any(JavaFile.class))).thenReturn(testAbleFile);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn("test");
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.testCasesByName("test")).thenReturn(newArrayList(testCase));

    when(perspectives.as(eq(MutableTestPlan.class), any(Resource.class))).thenReturn(testPlan);

    sensor.analyse(project, context);

    verify(testCase).setCoverageBlock(testAbleFile, newArrayList(3, 6));
  }

  @Test
  public void do_not_save_measure_on_resource_which_doesnt_exist_in_the_context() {
    when(context.getResource(any(Resource.class))).thenReturn(null);
    when(fileSystem.binaryDirs()).thenReturn(ImmutableList.of(outputDir));

    sensor.analyse(project, context);

    verify(context, never()).saveMeasure(any(Resource.class), any(Measure.class));
  }

  @Test
  public void should_do_nothing_if_output_dir_does_not_exists() {
    when(fileSystem.binaryDirs()).thenReturn(ImmutableList.of(new File("nowhere")));

    sensor.analyse(project, context);

    verifyZeroInteractions(context);
  }

}
