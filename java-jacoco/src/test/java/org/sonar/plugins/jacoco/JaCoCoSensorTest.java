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
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.jacoco.JacocoConstants.IT_REPORT_PATH_PROPERTY;
import static org.sonar.plugins.jacoco.JacocoConstants.REPORT_MISSING_FORCE_ZERO;
import static org.sonar.plugins.jacoco.JacocoConstants.REPORT_PATHS_PROPERTY;
import static org.sonar.plugins.jacoco.JacocoConstants.REPORT_PATH_PROPERTY;

public class JaCoCoSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private static final Version RUNTIME_6_2 = Version.create(6, 2);
  private static final Version RUNTIME_5_6 = Version.create(5, 6);
  private File jacocoExecutionData;
  private File baseDir;
  private ResourcePerspectives perspectives;
  private SensorContextTester context;
  private JaCoCoSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath;

  @Before
  public void setUp() throws Exception {
    baseDir = temp.newFolder();
    jacocoExecutionData = new File(baseDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(baseDir, "Hello.class"));

    context = SensorContextTester.create(baseDir)
      .setSettings(new MapSettings(new PropertyDefinitions(JacocoConstants.getPropertyDefinitions(RUNTIME_5_6))));

    context.fileSystem().setWorkDir(new File(baseDir, "work"));
    perspectives = mock(ResourcePerspectives.class);
    javaClasspath = mock(JavaClasspath.class);
    sensor = new JaCoCoSensor(perspectives, javaResourceLocator, javaClasspath);
  }

  @Test
  public void testSensorDescriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("JaCoCo");
  }

  @Test
  public void logIfInvalidReportPath_before_6_2() {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(RUNTIME_5_6, SonarQubeSide.SCANNER));
    context.settings().setProperty(REPORT_PATH_PROPERTY, "unknown.exec");
    sensor.execute(context);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("JaCoCo report not found: 'unknown.exec'");
  }

  @Test
  public void logIfInvalidReportPath() {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(RUNTIME_6_2, SonarQubeSide.SCANNER));
    context.settings().setProperty(REPORT_PATH_PROPERTY, "unknown.exec");
    context.settings().setProperty(IT_REPORT_PATH_PROPERTY, "unknownit.exec");
    context.settings().setProperty(REPORT_PATHS_PROPERTY, "unknown1.exec,unknown2.exec");
    sensor.execute(context);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains(
      "JaCoCo UT report not found: 'unknown.exec'",
      "JaCoCo IT report not found: 'unknownit.exec'",
      "JaCoCo report not found: 'unknown1.exec'",
      "JaCoCo report not found: 'unknown2.exec'");
  }

  @Test
  public void test_read_execution_data_before_6_2() throws Exception {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(RUNTIME_5_6, SonarQubeSide.SCANNER));
    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");
    runAnalysis();
  }

  @Test
  public void test_read_execution_data_after_6_2_using_deprecated_prop() throws Exception {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(RUNTIME_6_2, SonarQubeSide.SCANNER));
    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");
    runAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(
      "Property 'sonar.jacoco.reportPath' is deprecated. Please use 'sonar.jacoco.reportPaths' instead.");
  }

  @Test
  public void test_read_execution_data_after_6_2_using_deprecated_it_prop() throws Exception {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(RUNTIME_6_2, SonarQubeSide.SCANNER));
    context.settings().setProperty(IT_REPORT_PATH_PROPERTY, "jacoco.exec");
    runAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(
      "Property 'sonar.jacoco.itReportPath' is deprecated. Please use 'sonar.jacoco.reportPaths' instead.");
  }

  @Test
  public void test_read_execution_data_after_6_2_using_list_prop() throws Exception {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(RUNTIME_6_2, SonarQubeSide.SCANNER));
    context.settings().setProperty(REPORT_PATHS_PROPERTY, "jacoco.exec,jacoco.exec");
    runAnalysis();
  }

  private void runAnalysis() throws IOException {
    Files.copy(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/jacoco.exec"), jacocoExecutionData);

    DefaultInputFile file = new DefaultInputFile(context.module().key(), "org/sonar/plugins/jacoco/tests/Hello");
    file.setLines(19);
    when(javaResourceLocator.findResourceByClassName("org/sonar/plugins/jacoco/tests/Hello")).thenReturn(file);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(baseDir));

    sensor.execute(context);
    int[] oneHitlines = new int[] {6, 7, 8, 11};
    int[] zeroHitlines = new int[] {15, 16, 18};
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(file.key(), zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(file.key(), oneHitline)).isEqualTo(1);
    }
    assertThat(context.conditions(file.key(), 15)).isEqualTo(2);
    assertThat(context.coveredConditions(file.key(), 15)).isEqualTo(0);
  }

  @Test
  public void test_read_execution_data_for_lines_covered_by_tests() throws IOException {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(RUNTIME_5_6, SonarQubeSide.SCANNER));
    Files.copy(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest2/jacoco.exec"), jacocoExecutionData);
    new File(baseDir, "org/example/App.class").getParentFile().mkdirs();
    Files.copy(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest2/org/example/App.class.toCopy"),
      new File(baseDir, "org/example/App.class"));
    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");

    DefaultInputFile file = new DefaultInputFile(context.module().key(), "org/sonar/plugins/jacoco/tests/Hello");
    file.setLines(18);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(file);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(baseDir));

    MutableTestable testAbleFile = mock(MutableTestable.class);
    when(perspectives.as(eq(MutableTestable.class), eq(file))).thenReturn(testAbleFile);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn("test");
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.testCasesByName("test")).thenReturn(newArrayList(testCase));

    when(perspectives.as(eq(MutableTestPlan.class), eq(file))).thenReturn(testPlan);
    SensorContextTester spy = Mockito.spy(context);
    sensor.execute(spy);
    // UT + Overall
    verify(spy, times(2)).newCoverage();

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

  private void testExecutionDataForLinesCoveredByTest(String path, List<Integer> linesExpected) throws IOException {
    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");
    File toCopy = TestUtils.getResource(path);
    FileUtils.copyDirectory(toCopy, baseDir);

    DefaultInputFile file = new DefaultInputFile("", "");
    file.setLines(25);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(file);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(baseDir));

    MutableTestable testAbleFile = mock(MutableTestable.class);
    when(perspectives.as(eq(MutableTestable.class), eq(file))).thenReturn(testAbleFile);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn("test");
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.testCasesByName("testBoth")).thenReturn(newArrayList(testCase));
    when(testPlan.testCasesByName("testFoo")).thenReturn(newArrayList(testCase));

    when(perspectives.as(eq(MutableTestPlan.class), eq(file))).thenReturn(testPlan);

    sensor.execute(context);
    verify(testCase).setCoverageBlock(testAbleFile, linesExpected);
  }

  @Test
  public void force_coverage_to_zero_when_no_report_before_6_2() {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(RUNTIME_5_6, SonarQubeSide.SCANNER));
    context.settings().setProperty(REPORT_MISSING_FORCE_ZERO, "true");
    JaCoCoSensor sensor_force_coverage = new JaCoCoSensor(perspectives, javaResourceLocator, javaClasspath);
    DefaultInputFile file = new DefaultInputFile(context.module().key(), "some/path/Foo.java");
    file.setLines(25);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(file);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(baseDir));
    sensor_force_coverage.execute(context);
    int[] zeroHitlines = new int[] {6, 7, 8, 11, 15, 16, 18};
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(file.key(), zeroHitline)).isEqualTo(0);
    }
  }

}
