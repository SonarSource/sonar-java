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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
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
import static org.mockito.Matchers.anyString;
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
  private SensorContextTester context;
  private PathResolver pathResolver;
  private JaCoCoSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath;

  @Before
  public void setUp() throws Exception {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(jacocoExecutionData.getParentFile(), "Hello.class"));

    context = SensorContextTester.create(new File(""));
    pathResolver = mock(PathResolver.class);

    Settings settings = new MapSettings();
    settings.setProperty(JacocoConfiguration.REPORT_PATH_PROPERTY, JacocoConfiguration.REPORT_PATH_DEFAULT_VALUE);
    configuration = new JacocoConfiguration(settings);
    perspectives = mock(ResourcePerspectives.class);
    javaClasspath = mock(JavaClasspath.class);
    sensor = new JaCoCoSensor(configuration, perspectives, context.fileSystem(), pathResolver, javaResourceLocator, javaClasspath);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoSensor");
  }

  @Test
  public void should_execute_if_report_exists() {
    JacocoConfiguration configuration = mock(JacocoConfiguration.class);
    JaCoCoSensor sensor = new JaCoCoSensor(configuration, perspectives, context.fileSystem(), pathResolver, javaResourceLocator, javaClasspath);
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.exec"));
    when(configuration.getReportPath()).thenReturn("ut.exec");
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.not.found.exec"));
    assertThat(sensor.shouldExecuteOnProject()).isFalse();
  }

  @Test
  public void test_read_execution_data() {
    DefaultInputFile resource = new DefaultInputFile("", "org/sonar/plugins/jacoco/tests/Hello");
    resource.setLines(19);
    when(javaResourceLocator.findResourceByClassName("org/sonar/plugins/jacoco/tests/Hello")).thenReturn(resource);

    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    sensor.execute(context);
    int[] oneHitlines = new int[] {6, 7, 8, 11};
    int[] zeroHitlines = new int[] {15, 16, 18};
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(":org/sonar/plugins/jacoco/tests/Hello", CoverageType.UNIT, zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(":org/sonar/plugins/jacoco/tests/Hello", CoverageType.UNIT, oneHitline)).isEqualTo(1);
    }
    assertThat(context.conditions(":org/sonar/plugins/jacoco/tests/Hello", CoverageType.UNIT, 15)).isEqualTo(2);
    assertThat(context.coveredConditions(":org/sonar/plugins/jacoco/tests/Hello", CoverageType.UNIT, 15)).isEqualTo(0);
  }

  @Test
  public void test_read_execution_data_for_lines_covered_by_tests() throws IOException {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest2/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");
    Files.copy(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest2/org/example/App.class.toCopy"),
        new File(jacocoExecutionData.getParentFile(), "/org/example/App.class"));
    DefaultInputFile resource = new DefaultInputFile("", "");
    resource.setLines(10);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    MutableTestable testAbleFile = mock(MutableTestable.class);
    when(perspectives.as(eq(MutableTestable.class), eq(resource))).thenReturn(testAbleFile);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn("test");
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.testCasesByName("test")).thenReturn(newArrayList(testCase));

    when(perspectives.as(eq(MutableTestPlan.class), eq(resource))).thenReturn(testPlan);
    SensorContextTester spy = Mockito.spy(context);
    sensor.execute(spy);
    verify(spy, times(1)).newCoverage();

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

    DefaultInputFile resource = new DefaultInputFile("", "");
    resource.setLines(25);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    MutableTestable testAbleFile = mock(MutableTestable.class);
    when(perspectives.as(eq(MutableTestable.class), eq(resource))).thenReturn(testAbleFile);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn("test");
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.testCasesByName("testBoth")).thenReturn(newArrayList(testCase));
    when(testPlan.testCasesByName("testFoo")).thenReturn(newArrayList(testCase));

    when(perspectives.as(eq(MutableTestPlan.class), eq(resource))).thenReturn(testPlan);

    sensor.execute(context);
    verify(testCase).setCoverageBlock(testAbleFile, linesExpected);
  }

  @Test
  public void force_coverage_to_zero_when_no_report() {
    Map<String, String> props = ImmutableMap.of(JacocoConfiguration.REPORT_MISSING_FORCE_ZERO, "true", JacocoConfiguration.REPORT_PATH_PROPERTY, "foo");
    DefaultFileSystem fileSystem = new DefaultFileSystem((File)null);
    fileSystem.add(new DefaultInputFile("","foo").setLanguage("java"));
    JacocoConfiguration configuration = new JacocoConfiguration(new MapSettings().addProperties(props));
    JaCoCoSensor sensor_force_coverage = new JaCoCoSensor(configuration, perspectives, fileSystem, pathResolver, javaResourceLocator, javaClasspath);
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    DefaultInputFile resource = new DefaultInputFile("", "");
    resource.setLines(25);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(new File("foo"));
    assertThat(sensor_force_coverage.shouldExecuteOnProject()).isTrue();
    sensor_force_coverage.execute(context);
    int[] zeroHitlines = new int[] {6, 7, 8, 11, 15, 16, 18};
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(":", CoverageType.UNIT, zeroHitline)).isEqualTo(0);
    }
  }

  @Test
  public void do_not_save_measure_on_resource_which_doesnt_exist_in_the_context() {
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(jacocoExecutionData);
    SensorContext context = mock(SensorContext.class);
    sensor.execute(context);
    verify(context, never()).newCoverage();
  }

  @Test
  public void should_do_nothing_if_output_dir_does_not_exists() {
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(new File("nowhere")));
    when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(jacocoExecutionData);
    SensorContext context = mock(SensorContext.class);
    sensor.execute(context);

    verifyZeroInteractions(context);
  }

}
