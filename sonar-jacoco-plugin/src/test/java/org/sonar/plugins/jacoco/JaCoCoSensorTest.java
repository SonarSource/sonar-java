/*
 * Sonar Java
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

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.tests.ProjectTests;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Evgeny Mandrikov
 */
public class JaCoCoSensorTest {

  private static File jacocoExecutionData;
  private static File outputDir;

  private JacocoConfiguration configuration;
  private ProjectTests projectTests;

  private SensorContext context;
  private ProjectFileSystem pfs;
  private Project project;

  private JaCoCoSensor sensor;

  @BeforeClass
  public static void setUpOutputDir() throws IOException {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(jacocoExecutionData.getParentFile(), "Hello.class"));
  }

  @Before
  public void setUp() {
    context = mock(SensorContext.class);
    pfs = mock(ProjectFileSystem.class);
    project = mock(Project.class);

    configuration = mock(JacocoConfiguration.class);
    projectTests = mock(ProjectTests.class);
    sensor = new JaCoCoSensor(configuration, projectTests);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString(), is("JaCoCoSensor"));
  }

  @Test
  public void should_execute_if_enabled() {
    Project project = mock(Project.class);

    when(configuration.isEnabled(project)).thenReturn(true);
    assertThat(sensor.shouldExecuteOnProject(project), is(true));

    when(configuration.isEnabled(project)).thenReturn(false);
    assertThat(sensor.shouldExecuteOnProject(project), is(false));
  }

  @Test
  public void test_read_execution_data() {
    JavaFile resource = new JavaFile("org.sonar.plugins.jacoco.tests.Hello");
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(pfs.getBuildOutputDir()).thenReturn(outputDir);
    when(pfs.resolvePath(anyString())).thenReturn(jacocoExecutionData);
    when(project.getFileSystem()).thenReturn(pfs);

    sensor.analyse(project, context);

    verify(context).getResource(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 7.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 3.0)));
    verify(context).saveMeasure(eq(resource),
      argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "6=1;7=1;8=1;11=1;15=0;16=0;18=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.CONDITIONS_BY_LINE, "15=2" +
      "")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "15=0")));
    verifyNoMoreInteractions(context);
  }

  @Test
  public void test_read_execution_data_for_lines_covered_by_tests() throws IOException {
    jacocoExecutionData = new File(outputDir, "jacoco_unit_test.exec");
    Files.copy(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/App.class.toCopy"),
        new File(jacocoExecutionData.getParentFile(), "Hello.class"));

    JavaFile resource = new JavaFile("org.example.App");
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(pfs.getBuildOutputDir()).thenReturn(outputDir);
    when(pfs.resolvePath(anyString())).thenReturn(jacocoExecutionData);
    when(project.getFileSystem()).thenReturn(pfs);

    sensor.analyse(project, context);

    verify(projectTests).cover("org.example.FirstTest", "test", "org.example.App", newArrayList(3, 6));
    verify(projectTests).cover("org.example.SecondTest", "test", "org.example.App", newArrayList(3, 10));
    verifyNoMoreInteractions(projectTests);
  }

  @Test
  public void do_not_save_measure_on_resource_which_doesnt_exist_in_the_context() {
    when(context.getResource(any(Resource.class))).thenReturn(null);
    when(pfs.getBuildOutputDir()).thenReturn(outputDir);
    when(project.getFileSystem()).thenReturn(pfs);

    sensor.analyse(project, context);

    verify(context, never()).saveMeasure(any(Resource.class), any(Measure.class));
  }

  @Test
  public void should_do_nothing_if_output_dir_does_not_exists() {
    when(pfs.getBuildOutputDir()).thenReturn(new File("nowhere"));
    when(project.getFileSystem()).thenReturn(pfs);

    sensor.analyse(project, context);

    verifyZeroInteractions(context);
  }
}
