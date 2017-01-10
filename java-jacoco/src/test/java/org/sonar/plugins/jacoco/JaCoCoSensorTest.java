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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.scan.filesystem.PathResolver;
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
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.jacoco.JacocoConfiguration.IT_REPORT_PATH_PROPERTY;
import static org.sonar.plugins.jacoco.JacocoConfiguration.REPORT_PATHS_PROPERTY;
import static org.sonar.plugins.jacoco.JacocoConfiguration.REPORT_PATH_PROPERTY;
import static org.sonar.plugins.jacoco.JacocoConfiguration.SQ_6_2;

public class JaCoCoSensorTest {

  private static final Version SQ_5_6 = Version.create(5, 6);
  private File jacocoExecutionData;
  private File outputDir;
  private JacocoConfiguration configuration;
  private ResourcePerspectives perspectives;
  private SensorContextTester context;
  private PathResolver pathResolver;
  private JaCoCoSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath;
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void setUp() throws Exception {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(jacocoExecutionData.getParentFile(), "Hello.class"));

    context = SensorContextTester.create(outputDir);
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_5_6, SonarQubeSide.SCANNER));
    context.fileSystem().setWorkDir(temp.newFolder());
    pathResolver = mock(PathResolver.class);

    Settings settings = new MapSettings();
    settings.setProperty(REPORT_PATH_PROPERTY, JacocoConfiguration.REPORT_PATH_DEFAULT_VALUE);
    context.settings().setProperty(REPORT_PATH_PROPERTY, JacocoConfiguration.REPORT_PATH_DEFAULT_VALUE);
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
  public void logIfInvalidReportPath() {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_6_2, SonarQubeSide.SCANNER));
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
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_5_6, SonarQubeSide.SCANNER));
    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");
    runAnalysis();
  }

  @Test
  public void test_read_execution_data_after_6_2_using_deprecated_prop() throws Exception {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_6_2, SonarQubeSide.SCANNER));
    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");
    runAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Property 'sonar.jacoco.reportPath' is deprecated. Please use 'sonar.jacoco.reportPaths' instead.");
  }

  @Test
  public void test_read_execution_data_after_6_2_using_deprecated_it_prop() throws Exception {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_6_2, SonarQubeSide.SCANNER));
    context.settings().setProperty(IT_REPORT_PATH_PROPERTY, "jacoco.exec");
    runAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Property 'sonar.jacoco.itReportPath' is deprecated. Please use 'sonar.jacoco.reportPaths' instead.");
  }

  @Test
  public void test_read_execution_data_after_6_2_using_correct_prop() throws Exception {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_6_2, SonarQubeSide.SCANNER));
    context.settings().setProperty(REPORT_PATHS_PROPERTY, "jacoco.exec");
    runAnalysis();
  }

  @Test
  public void test_read_execution_data_after_6_2_should_merge_reports() throws Exception {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_6_2, SonarQubeSide.SCANNER));
    String path1 = TestUtils.getResource("org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/jacoco-0.7.5.exec").getPath();
    String path2 = TestUtils.getResource("org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/jacoco-it-0.7.5.exec").getPath();
    context.settings().setProperty(REPORT_PATHS_PROPERTY, path1+","+path2);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    sensor.execute(context);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Analysing "+path1);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Analysing "+path2);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Analysing "+new File(context.fileSystem().workDir(), "jacoco-merged.exec").getAbsolutePath());
  }

  @Test
  public void should_execute_if_report_exists() {
    JacocoConfiguration configuration = mock(JacocoConfiguration.class);
    JaCoCoSensor sensor = new JaCoCoSensor(configuration, perspectives, context.fileSystem(), pathResolver, javaResourceLocator, javaClasspath);
    context.settings().setProperty(REPORT_PATH_PROPERTY, "ut.exec");
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.exec"));
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    sensor.execute(context);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("No JaCoCo analysis of project coverage can be done since there is no class files.");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.not.found.exec"));
    sensor.execute(context);
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(1)).startsWith("JaCoCoSensor: JaCoCo report not found :");
  }

  private void runAnalysis() throws IOException {
    DefaultInputFile resource = new DefaultInputFile("", "org/sonar/plugins/jacoco/tests/Hello");
    resource.setLines(19);
    when(javaResourceLocator.findResourceByClassName("org/sonar/plugins/jacoco/tests/Hello")).thenReturn(resource);

    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    sensor.execute(context);
    int[] oneHitlines = new int[] {6, 7, 8, 11};
    int[] zeroHitlines = new int[] {15, 16, 18};
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(resource.key(), zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(resource.key(), oneHitline)).isEqualTo(1);
    }
    assertThat(context.conditions(resource.key(), 15)).isEqualTo(2);
    assertThat(context.coveredConditions(resource.key(), 15)).isEqualTo(0);
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
  public void force_coverage_to_zero_when_no_report_lts() {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_5_6, SonarQubeSide.SCANNER));
    Map<String, String> props = ImmutableMap.of(JacocoConfiguration.REPORT_MISSING_FORCE_ZERO, "true", REPORT_PATH_PROPERTY, "foo");
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
    sensor_force_coverage.execute(context);
    int[] zeroHitlines = new int[] {6, 7, 8, 11, 15, 16, 18};
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(":", zeroHitline)).isEqualTo(0);
    }
  }

  @Test
  public void force_coverage_to_zero_is_deprecated_on_6_2() {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_6_2, SonarQubeSide.SCANNER));
    Map<String, String> props = ImmutableMap.of(JacocoConfiguration.REPORT_MISSING_FORCE_ZERO, "true", REPORT_PATHS_PROPERTY, "foo");
    DefaultFileSystem fileSystem = new DefaultFileSystem((File)null);
    fileSystem.add(new DefaultInputFile("","foo").setLanguage("java"));
    JacocoConfiguration configuration = new JacocoConfiguration(new MapSettings().addProperties(props));
    JaCoCoSensor sensor_force_coverage = new JaCoCoSensor(configuration, perspectives, fileSystem, pathResolver, javaResourceLocator, javaClasspath);
    context.settings().addProperties(props);
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    DefaultInputFile resource = new DefaultInputFile("", "");
    resource.setLines(25);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(new File("foo"));
    sensor_force_coverage.execute(context);
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Property 'sonar.jacoco.reportMissing.force.zero' is deprecated and its value will be ignored.");
  }

  @Test
  public void do_not_save_measure_on_resource_which_doesnt_exist_in_the_context() {
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(jacocoExecutionData);
    SensorContextTester context = spy(SensorContextTester.create(new File("")));
    sensor.execute(context);
    verify(context, never()).newCoverage();
  }

  @Test
  public void should_do_nothing_if_output_dir_does_not_exists() {
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(new File("nowhere")));
    when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(jacocoExecutionData);
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_5_6, SonarQubeSide.SCANNER));
    sensor.execute(context);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("No JaCoCo analysis of project coverage can be done since there is no class files.");
  }

}
