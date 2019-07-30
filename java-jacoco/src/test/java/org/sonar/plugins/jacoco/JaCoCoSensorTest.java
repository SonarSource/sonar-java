/*
 * SonarQube Java
 * Copyright (C) 2010-2019 SonarSource SA
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

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisWarningsWrapper;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonarsource.sonarlint.core.analyzer.sensor.SensorOptimizer;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.jacoco.JaCoCoExtensions.IT_REPORT_PATH_PROPERTY;
import static org.sonar.plugins.jacoco.JaCoCoExtensions.REPORT_MISSING_FORCE_ZERO;
import static org.sonar.plugins.jacoco.JaCoCoExtensions.REPORT_PATHS_PROPERTY;
import static org.sonar.plugins.jacoco.JaCoCoExtensions.REPORT_PATH_PROPERTY;

public class JaCoCoSensorTest {

  private static final Version SQ_6_7 = Version.create(6, 7);
  private static final String BINARY_FORMAT_IS_DEPRECATED = " is deprecated (JaCoCo binary format). 'sonar.coverage.jacoco.xmlReportPaths' should be used instead (JaCoCo XML format)." +
    " Please check that the JaCoCo plugin is installed on your SonarQube Instance.";
  private static final String REPORT_PATH_IS_DEPRECATED = "Property 'sonar.jacoco.reportPath'" + BINARY_FORMAT_IS_DEPRECATED;
  private static final String REPORT_PATHS_IS_DEPRECATED = "Property 'sonar.jacoco.reportPaths'" + BINARY_FORMAT_IS_DEPRECATED;
  private static final String IT_REPORT_PATH_IS_DEPRECATED = "Property 'sonar.jacoco.itReportPath'" + BINARY_FORMAT_IS_DEPRECATED;
  private static final String XML_REPORT_FOUND = "JaCoCo XML report found, skipping processing of binary JaCoCo exec report.";
  private File jacocoExecutionData;
  private File outputDir;
  private ResourcePerspectives perspectives;
  private SensorContextTester context;
  private JaCoCoSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath;
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private TestAnalysisWarnings analysisWarnings = new TestAnalysisWarnings();

  @Rule
  public LogTester logTester = new LogTester();
  private int[] oneHitlines = new int[]{6, 7, 8, 11};
  private int[] zeroHitlines = new int[]{15, 16, 18};
  private DefaultInputFile resource = new TestInputFileBuilder("", "org/sonar/plugins/jacoco/tests/Hello").setLines(19).build();

  @Before
  public void setUp() throws Exception {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(jacocoExecutionData.getParentFile(), "Hello.class"));

    context = SensorContextTester.create(outputDir);
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_6_7, SonarQubeSide.SCANNER));
    context.fileSystem().setWorkDir(temp.newFolder().toPath());
    DefaultInputFile inputFile = TestInputFileBuilder.create(context.module().key(), "Hello.java").setLanguage("java").build();
    context.fileSystem().add(inputFile);

    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");
    perspectives = mock(ResourcePerspectives.class);
    javaClasspath = mock(JavaClasspath.class);
    sensor = new JaCoCoSensor(perspectives, javaResourceLocator, javaClasspath, analysisWarnings);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoSensor");
  }

  @Test
  public void logIfInvalidReportPath() {
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
  public void test_read_execution_data_after_6_2_using_deprecated_prop() throws Exception {
    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");
    runAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(REPORT_PATH_IS_DEPRECATED);
  }

  @Test
  public void no_warning_with_xml_prop_set() throws Exception {
    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");
    context.settings().setProperty(IT_REPORT_PATH_PROPERTY, "jacoco.exec");
    context.settings().setProperty(REPORT_PATHS_PROPERTY, "jacoco.exec");
    context.settings().setProperty(JaCoCoSensor.JACOCO_XML_PROPERTY, "jacoco.xml");
    runAnalysisWithoutAssert();
    assertThat(logTester.logs(LoggerLevel.WARN)).doesNotContain(REPORT_PATH_IS_DEPRECATED);
    assertThat(logTester.logs(LoggerLevel.WARN)).doesNotContain(IT_REPORT_PATH_IS_DEPRECATED);
    assertThat(logTester.logs(LoggerLevel.WARN)).doesNotContain(REPORT_PATHS_IS_DEPRECATED);
  }

  @Test
  public void test_read_execution_data_after_6_2_using_deprecated_it_prop() throws Exception {
    context.settings().setProperty(IT_REPORT_PATH_PROPERTY, "jacoco.exec");
    runAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(IT_REPORT_PATH_IS_DEPRECATED);
  }

  @Test
  public void test_read_execution_data_after_6_2_using_correct_prop() throws Exception {
    context.settings().setProperty(REPORT_PATHS_PROPERTY, "jacoco.exec");
    runAnalysis();
  }

  @Test
  public void test_read_execution_data_after_6_2_should_merge_reports() throws Exception {
    String path1 = TestUtils.getResource("org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/jacoco-0.7.5.exec").getPath();
    String path2 = TestUtils.getResource("org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/jacoco-it-0.7.5.exec").getPath();
    context.settings().setProperty(REPORT_PATH_PROPERTY, "");
    context.settings().setProperty(REPORT_PATHS_PROPERTY, path1 + "," + path2);
    when(javaClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(outputDir));
    sensor.execute(context);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Analysing " + path1);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Analysing " + path2);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Analysing " + new File(context.fileSystem().workDir(), "jacoco-merged.exec").getAbsolutePath());
  }

  @Test
  public void should_execute_if_report_exists() {
    JaCoCoSensor sensor = new JaCoCoSensor(perspectives, javaResourceLocator, javaClasspath);
    context.settings().setProperty(REPORT_PATH_PROPERTY, "ut.exec");
    sensor.execute(context);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("No JaCoCo analysis of project coverage can be done since there are no class files.");
    context.settings().setProperty(REPORT_PATH_PROPERTY, "ut.notfound.exec");
    sensor.execute(context);
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(1)).startsWith("JaCoCo UT report not found:");
  }

  private void runAnalysis() throws IOException {
    runAnalysisWithoutAssert();
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(resource.key(), zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(resource.key(), oneHitline)).isEqualTo(1);
    }
    assertThat(context.conditions(resource.key(), 15)).isEqualTo(2);
    assertThat(context.coveredConditions(resource.key(), 15)).isEqualTo(0);
  }

  private void runAnalysisWithoutAssert() {
    when(javaResourceLocator.findResourceByClassName("org/sonar/plugins/jacoco/tests/Hello")).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(outputDir));
    sensor.execute(context);
  }

  @Test
  public void test_read_execution_data_for_lines_covered_by_tests() throws IOException {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest2/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");
    Files.copy(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest2/org/example/App.class.toCopy"),
      new File(jacocoExecutionData.getParentFile(), "/org/example/App.class"));
    DefaultInputFile resource = new TestInputFileBuilder("", "").setLines(10).build();
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(outputDir));

    MutableTestable testAbleFile = mock(MutableTestable.class);
    when(perspectives.as(eq(MutableTestable.class), eq(resource))).thenReturn(testAbleFile);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn("test");
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.testCasesByName("test")).thenReturn(newArrayList(testCase));

    when(perspectives.as(eq(MutableTestPlan.class), eq(resource))).thenReturn(testPlan);
    context.settings().setProperty(REPORT_PATH_PROPERTY, jacocoExecutionData.getAbsolutePath());
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
  public void test_read_execution_data_for_lines_covered_by_tests_v0_7_5_incompatible() throws IOException {
    testExecutionDataForLinesCoveredByTest("/org/sonar/plugins/jacoco/JaCoCov0_7_5_incompatible_coverage_per_test/", newArrayList(3, 4, 5, 8, 9, 10, 13, 16));
  }

  private void testExecutionDataForLinesCoveredByTest(String path, List<Integer> linesExpected) {
    outputDir = TestUtils.getResource(path);
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    DefaultInputFile resource = new TestInputFileBuilder("", "").setLines(25).build();
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(outputDir));

    MutableTestable testAbleFile = mock(MutableTestable.class);
    when(perspectives.as(eq(MutableTestable.class), eq(resource))).thenReturn(testAbleFile);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn("test");
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.testCasesByName("testBoth")).thenReturn(newArrayList(testCase));
    when(testPlan.testCasesByName("testFoo")).thenReturn(newArrayList(testCase));

    when(perspectives.as(eq(MutableTestPlan.class), eq(resource))).thenReturn(testPlan);
    context.settings().setProperty(REPORT_PATH_PROPERTY, jacocoExecutionData.getAbsolutePath());
    sensor.execute(context);
    verify(testCase).setCoverageBlock(testAbleFile, linesExpected);
  }

  @Test
  public void log_deprecation_for_coverage_per_test() throws IOException {
    testExecutionDataForLinesCoveredByTest("/org/sonar/plugins/jacoco/JaCoCov0_7_5_coverage_per_test/", newArrayList(3, 4, 5, 8, 12));
    String msg = "'Coverage per Test' feature is deprecated. Consider removing sonar-jacoco-listeners from your configuration.";
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(msg);
    assertThat(analysisWarnings.warnings).contains(msg);
  }

  @Test
  public void log_removal_for_coverage_per_test() throws IOException {
    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.create(7,7), SonarQubeSide.SCANNER));
    testExecutionDataForLinesCoveredByTest("/org/sonar/plugins/jacoco/JaCoCov0_7_5_coverage_per_test/", newArrayList(3, 4, 5, 8, 12));
    String msg = "'Coverage per Test' feature was removed from SonarQube. Remove sonar-jacoco-listeners listener configuration.";
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(msg);
    assertThat(analysisWarnings.warnings).contains(msg);
  }

  @Test
  public void do_not_save_measure_on_resource_which_doesnt_exist_in_the_context() throws Exception {
    when(javaClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(outputDir));
    SensorContextTester context = spy(SensorContextTester.create(temp.newFolder()));
    sensor.execute(context);
    verify(context, never()).newCoverage();
  }

  @Test
  public void should_do_nothing_if_output_dir_does_not_exists() throws Exception {
    when(javaClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(new File("nowhere")));
    SensorContextTester context = SensorContextTester.create(temp.newFolder());
    context.settings().setProperty(REPORT_PATHS_PROPERTY, jacocoExecutionData.getAbsolutePath());
    sensor.execute(context);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("No JaCoCo analysis of project coverage can be done since there are no class files.");
  }

  @Test
  public void should_log_when_class_are_not_matching_with_report() throws Exception {
    String testDir = "/org/sonar/plugins/jacoco/JaCoCoNoMatch/";
    outputDir = TestUtils.getResource(testDir);
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource(testDir + "org/foo/bar/Example2.class.toCopy"), new File(outputDir, "org/foo/bar/Example2.class"));
    Files.copy(TestUtils.getResource(testDir + "Example.class.toCopy"), new File(outputDir, "Example.class"));

    DefaultInputFile resource = new TestInputFileBuilder("", "").setLines(10).build();

    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(resource);
    when(javaClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(outputDir));

    SensorContextTester context = SensorContextTester.create(outputDir);
    context.setRuntime(SonarRuntimeImpl.forSonarQube(SQ_6_7, SonarQubeSide.SCANNER));
    context.fileSystem().setWorkDir(temp.newFolder().toPath());
    context.setSettings(new MapSettings().setProperty(REPORT_PATHS_PROPERTY, jacocoExecutionData.getAbsolutePath()));

    sensor.execute(context);

    List<String> warnLogs = logTester.logs(LoggerLevel.WARN);
    assertThat(warnLogs).contains(
      "The following class(es) did not match with execution data:",
      "> 'org/foo/bar/Example2'",
      "> 'Example'",
      "In order to have accurate coverage measures, the same class files must be used as at runtime for report generation.");
  }

  @Test
  public void should_log_warning_for_deprecated_properties() throws Exception {
    context.settings().setProperty(REPORT_PATHS_PROPERTY, "jacoco.exec");
    runAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(REPORT_PATHS_IS_DEPRECATED);
    assertThat(analysisWarnings.warnings).contains(REPORT_PATHS_IS_DEPRECATED);
  }

  @Test
  public void should_log_warning_for_force_coverage() throws Exception {
    context.settings().setProperty(REPORT_MISSING_FORCE_ZERO, "true");
    runAnalysis();
    String msg = "Property 'sonar.jacoco.reportMissing.force.zero' is deprecated and its value will be ignored.";
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(msg);
    assertThat(analysisWarnings.warnings).contains(msg);
  }

  @Test
  public void should_log_info_when_both_xml_and_exec_properties_set() throws Exception {
    context.settings().setProperty(REPORT_PATHS_PROPERTY, "jacoco.exec");
    context.settings().setProperty(JaCoCoSensor.JACOCO_XML_PROPERTY, "jacoco.xml");
    runAnalysisWithoutAssert();
    List<Integer> coverage = IntStream.range(1, resource.lines()).mapToObj(line -> context.lineHits(resource.key(), line)).collect(Collectors.toList());
    assertThat(coverage).allMatch(Objects::isNull);

    String msg = "Both 'sonar.jacoco.reportPaths' and 'sonar.coverage.jacoco.xmlReportPaths' were set. 'sonar.jacoco.reportPaths' is deprecated" +
      " therefore, only 'sonar.coverage.jacoco.xmlReportPaths' will be taken into account." +
      " Please check that the JaCoCo plugin is installed on your SonarQube Instance.";
    assertThat(logTester.logs(LoggerLevel.INFO)).contains(msg);
    assertThat(analysisWarnings.warnings).isEmpty();
  }

  @Test
  public void test_sensor_descriptor() {
    SensorOptimizer sensorOptimizer = new SensorOptimizer(context.fileSystem(), context.activeRules(), context.config());
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(sensorDescriptor);
    assertThat(sensorOptimizer.shouldExecute(sensorDescriptor)).isTrue();
  }

  @Test
  public void log_skip_processing() throws Exception {
    context.settings().setProperty(REPORT_PATH_PROPERTY, "jacoco.exec");
    context.settings().setProperty("sonar.coverage.jacoco.xmlReportPaths", "jacoco.xml");
    runAnalysisWithoutAssert();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains(XML_REPORT_FOUND);
  }

  @Test
  public void log_skip_processing_only_if_binary_report_found() throws Exception {
    context = SensorContextTester.create(temp.newFolder());
    context.settings().setProperty("sonar.coverage.jacoco.xmlReportPaths", "jacoco.xml");
    sensor = new JaCoCoSensor(perspectives, javaResourceLocator, javaClasspath, analysisWarnings);
    sensor.execute(context);
    assertThat(logTester.logs()).doesNotContain(XML_REPORT_FOUND);
  }

  @Test
  public void log_warning_when_using_default_location() throws Exception {
    useDefaultSettings();
    makeDefaultJacocoBinary();
    runAnalysisWithoutAssert();
    assertThat(logTester.logs()).contains(REPORT_PATHS_IS_DEPRECATED);
  }

  @Test
  public void do_not_log_warning_when_using_default_location_with_xml() throws Exception {
    outputDir = temp.newFolder();
    context = SensorContextTester.create(outputDir);
    useDefaultSettings();
    makeDefaultJacocoXmlReport();
    makeDefaultJacocoBinary();
    sensor.execute(context);
    assertThat(logTester.logs()).contains(XML_REPORT_FOUND);
    assertThat(logTester.logs()).doesNotContain(REPORT_PATHS_IS_DEPRECATED);
  }

  private void useDefaultSettings() {
    context.setSettings(new MapSettings(new PropertyDefinitions(JaCoCoExtensions.getExtensions())));
  }

  private void makeDefaultJacocoXmlReport() throws IOException {
    File defaultJaCoCoXmlLocation = new File(outputDir, "target/site/jacoco/jacoco.xml");
    defaultJaCoCoXmlLocation.getParentFile().mkdirs();
    defaultJaCoCoXmlLocation.createNewFile();
  }

  private void makeDefaultJacocoBinary() throws IOException {
    File defaultJaCoCoLocation = new File(outputDir, "target/jacoco.exec");
    defaultJaCoCoLocation.getParentFile().mkdirs();
    Files.copy(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/jacoco.exec"), defaultJaCoCoLocation);
  }

  static class TestAnalysisWarnings extends AnalysisWarningsWrapper {

    List<String> warnings = new ArrayList<>();

    TestAnalysisWarnings() {
      super(null);
    }

    @Override
    public void addUnique(String text) {
      warnings.add(text);
    }
  }

}
