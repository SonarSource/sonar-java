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
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
  private PathResolver pathResolver;
  private Project project;
  private JaCoCoSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath;

  @Before
  public void setUp() throws Exception {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(jacocoExecutionData.getParentFile(), "Hello.class"));

    context = mock(SensorContext.class);
    ModuleFileSystem fileSystem = mock(ModuleFileSystem.class);
    pathResolver = mock(PathResolver.class);
    project = mock(Project.class);

    configuration = mock(JacocoConfiguration.class);
    perspectives = mock(ResourcePerspectives.class);
    javaClasspath = mock(JavaClasspath.class);
    sensor = new JaCoCoSensor(configuration, perspectives, fileSystem, pathResolver, javaResourceLocator, javaClasspath);
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
    org.sonar.api.resources.File resource = mock(org.sonar.api.resources.File.class);
    when(javaResourceLocator.findResourceByClassName("org/sonar/plugins/jacoco/tests/Hello")).thenReturn(resource);
    when(context.getResource(any(Resource.class))).thenReturn(resource);

    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
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

    org.sonar.api.resources.File resource = mock(org.sonar.api.resources.File.class);
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    MutableTestable testAbleFile = mock(MutableTestable.class);
    when(perspectives.as(eq(MutableTestable.class), any(org.sonar.api.resources.File.class))).thenReturn(testAbleFile);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn("test");
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.testCasesByName("test")).thenReturn(newArrayList(testCase));

    when(perspectives.as(eq(MutableTestPlan.class), any(Resource.class))).thenReturn(testPlan);

    sensor.analyse(project, context);

    verify(testCase).setCoverageBlock(testAbleFile, newArrayList(3, 6));
  }

  @Test
  public void test_read_execution_data_for_lines_covered_by_tests_v0_7_5() throws IOException {
    testExecutionDataForLinesCoveredByTest("/org/sonar/plugins/jacoco/JaCoCov0_7_5_coverage_per_test/", newArrayList(3, 4, 5, 8, 12));
  }

  @Test
  public void test_read_execution_data_for_lines_covered_by_tests_v0_7_4() throws IOException {
    testExecutionDataForLinesCoveredByTest("/org/sonar/plugins/jacoco/JaCoCov0_7_4_coverage_per_test/", newArrayList(3, 4, 5, 8, 12));
  }

  @Test
  public void test_read_execution_data_for_lines_covered_by_tests_v0_7_4_incompatible() throws IOException {
    testExecutionDataForLinesCoveredByTest("/org/sonar/plugins/jacoco/JaCoCov0_7_4_incompatible_coverage_per_test/", newArrayList(3, 4, 5, 8, 12));
  }

  @Test
  public void test_read_execution_data_for_lines_covered_by_tests_v0_7_5_incompatible() throws IOException {
    testExecutionDataForLinesCoveredByTest("/org/sonar/plugins/jacoco/JaCoCov0_7_5_incompatible_coverage_per_test/", newArrayList(3, 4, 5, 8, 9, 10, 13, 16));
  }

  private void testExecutionDataForLinesCoveredByTest(String path, List<Integer> linesExpected) {
    outputDir = TestUtils.getResource(path);
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    org.sonar.api.resources.File resource = mock(org.sonar.api.resources.File.class);
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    MutableTestable testAbleFile = mock(MutableTestable.class);
    when(perspectives.as(eq(MutableTestable.class), any(org.sonar.api.resources.File.class))).thenReturn(testAbleFile);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn("test");
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.testCasesByName("testBoth")).thenReturn(newArrayList(testCase));
    when(testPlan.testCasesByName("testFoo")).thenReturn(newArrayList(testCase));

    when(perspectives.as(eq(MutableTestPlan.class), any(Resource.class))).thenReturn(testPlan);

    sensor.analyse(project, context);
    verify(testCase).setCoverageBlock(testAbleFile, linesExpected);
  }

  @Test
  public void force_coverage_to_zero_when_no_report() {
    ModuleFileSystem fs = mock(ModuleFileSystem.class);
    Map<String, String> props = ImmutableMap.of(JacocoConfiguration.REPORT_MISSING_FORCE_ZERO, "true", JacocoConfiguration.REPORT_PATH_PROPERTY, "foo");
    DefaultFileSystem fileSystem = new DefaultFileSystem(null);
    fileSystem.add(new DefaultInputFile("foo").setLanguage("java"));
    JacocoConfiguration configuration = new JacocoConfiguration(new Settings().addProperties(props), fileSystem);
    JaCoCoSensor sensor_force_coverage = new JaCoCoSensor(configuration, perspectives, fs, pathResolver, javaResourceLocator, javaClasspath);
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    org.sonar.api.resources.File resource = mock(org.sonar.api.resources.File.class);
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(new File("foo"));
    assertThat(sensor_force_coverage.shouldExecuteOnProject(project)).isTrue();
    sensor_force_coverage.analyse(project, context);

    verify(context, times(1)).saveMeasure(any(Resource.class), eqMetric(CoreMetrics.LINES_TO_COVER_KEY, 7));
    verify(context, times(1)).saveMeasure(any(Resource.class), eqMetric(CoreMetrics.UNCOVERED_LINES_KEY, 7));
    verify(context, times(1)).saveMeasure(any(Resource.class), eqMetric(CoreMetrics.CONDITIONS_TO_COVER_KEY, 2));
    verify(context, times(1)).saveMeasure(any(Resource.class), eqMetric(CoreMetrics.UNCOVERED_CONDITIONS_KEY, 2));
  }

  static Measure<?> eqMetric(String metricKey, int value) {
    return argThat(new MetricMatcher(metricKey, value));
  }

  private static class MetricMatcher extends ArgumentMatcher<Measure<?>>{

    private final String metric;
    private final double value;

    public MetricMatcher(String metric, int value) {
      this.metric = metric;
      this.value = value;
    }

    @Override
    public boolean matches(Object actual) {
      Measure<?> actualMeasure = (Measure<?>) actual;
      return actualMeasure.getMetricKey().equals(metric) && actualMeasure.getIntValue()==value;
    }
  }

  @Test
  public void do_not_save_measure_on_resource_which_doesnt_exist_in_the_context() {
    when(context.getResource(any(Resource.class))).thenReturn(null);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));

    sensor.analyse(project, context);

    verify(context, never()).saveMeasure(any(Resource.class), any(Measure.class));
  }

  @Test
  public void should_do_nothing_if_output_dir_does_not_exists() {
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(new File("nowhere")));

    sensor.analyse(project, context);

    verifyZeroInteractions(context);
  }

}
