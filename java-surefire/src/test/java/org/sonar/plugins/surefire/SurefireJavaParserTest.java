/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.surefire;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SurefireJavaParserTest {

  private JavaResourceLocator javaResourceLocator;
  private SurefireJavaParser parser;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @BeforeEach
  public void before() {
    javaResourceLocator = mock(JavaResourceLocator.class);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenAnswer(invocation -> new TestInputFileBuilder("", (String) invocation.getArguments()[0]).build());

    parser = new SurefireJavaParser(javaResourceLocator);
  }

  @Test
  void should_store_zero_tests_when_directory_is_null_or_non_existing_or_a_file() throws Exception {

    SensorContext context = mock(SensorContext.class);

    context = mock(SensorContext.class);
    parser.collect(context, getDirs("nonExistingReportsDirectory"), false);
    verify(context, never()).newMeasure();

    context = mock(SensorContext.class);
    parser.collect(context, getDirs("file.txt"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  void shouldAggregateReports() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("multipleReports"), true);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CloverCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CheckstyleCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.SonarMojoTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JDependsCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JavaNCSSCollectorTest")).hasSize(5);
  }

  @Test
  void shouldAggregateReportsFromMultipleDirectories() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("multipleDirectories/dir1", "multipleDirectories/dir2"), true);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CloverCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.CheckstyleCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.SonarMojoTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JDependsCollectorTest")).hasSize(5);
    assertThat(context.measures(":ch.hortis.sonar.mvn.mc.JavaNCSSCollectorTest")).hasSize(5);
  }

  // SONAR-2841: if there's only a test suite report, then it should be read.
  @Test
  void shouldUseTestSuiteReportIfAlone() throws URISyntaxException {
    SensorContextTester context = mockContext();

    parser.collect(context, getDirs("onlyTestSuiteReport"), true);
    assertThat(context.measures(":org.sonar.SecondTest")).hasSize(5);
    assertThat(context.measures(":org.sonar.JavaNCSSCollectorTest")).hasSize(5);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  void shouldInsertZeroWhenNoReports() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("noReports"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  void shouldNotInsertZeroOnFiles() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("noTests"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  void shouldMergeInnerClasses() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("innerClasses"), true);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TESTS).value()).isEqualTo(7);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
    assertThat(context.measures(":org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet")).isEmpty();
  }

  @Test
  void shouldMergeNestedInnerClasses() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("nestedInnerClasses"), true);
    assertThat(context.measure(":org.sonar.plugins.surefire.NestedInnerTest", CoreMetrics.TESTS).value()).isEqualTo(3);
  }

  @Test
  void shouldMergeInnerClassReportInExtraFile() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("innerClassExtraFile"), true);
    assertThat(context.measure(":com.example.project.CalculatorTests", CoreMetrics.TESTS).value()).isEqualTo(6);
  }

  @Test
  void should_not_count_negative_tests() throws URISyntaxException {
    SensorContextTester context = mockContext();

    parser.collect(context, getDirs("negativeTestTime"), true);
    //Test times : -1.120, 0.644, 0.015 -> computed time : 0.659, ignore negative time.
    assertThat(context.measure(":java.Foo", CoreMetrics.SKIPPED_TESTS).value()).isZero();
    assertThat(context.measure(":java.Foo", CoreMetrics.TESTS).value()).isEqualTo(6);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_ERRORS).value()).isZero();
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_FAILURES).value()).isZero();
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(659);
  }

  @Test
  void should_handle_parameterized_tests() throws URISyntaxException {
    SensorContextTester context = mockContext();
    when(javaResourceLocator.findResourceByClassName(anyString()))
      .thenAnswer(invocation -> {
        String className = (String) invocation.getArguments()[0];
        if (className.equals("org.foo.Junit4ParameterizedTest")
          || className.startsWith("org.foo.Junit5_0ParameterizedTest")
          || className.startsWith("org.foo.Junit5_7ParameterizedTest")) {
          return new TestInputFileBuilder("", className).build();
        }
        return null;
      });

    parser.collect(context, getDirs("junitParameterizedTests"), true);

    // class names are wrong in JUnit 4.X parameterized tests, with class name being the name of the test
    assertThat(context.measure(":org.foo.Junit4ParameterizedTest", CoreMetrics.TESTS).value()).isEqualTo(7);
    assertThat(context.measure(":org.foo.Junit4ParameterizedTest", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(1);

    // class names and test names are wrong in JUnit 5.0, resulting in repeated/parameterized tests sharing the same name,
    // with class name being the name of the test (cf. https://github.com/junit-team/junit5/issues/1182)
    assertThat(context.measure(":org.foo.Junit5_0ParameterizedTest", CoreMetrics.TESTS).value()).isEqualTo(13);
    assertThat(context.measure(":org.foo.Junit5_0ParameterizedTest", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(48);

    // test file with with JUnit 5.7.1
    assertThat(context.measure(":org.foo.Junit5_7ParameterizedTest", CoreMetrics.TESTS).value()).isEqualTo(12);
    assertThat(context.measure(":org.foo.Junit5_7ParameterizedTest", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(150);
  }

  @Test
  void should_log_missing_resource_with_debug_level() throws Exception {
    parser = new SurefireJavaParser(mock(JavaResourceLocator.class));
    parser.collect(mockContext(), getDirs("resourceNotFound"), true);
    assertThat(logTester.logs(Level.WARN)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains("Resource not found: org.sonar.Foo");
  }

  private static List<File> getDirs(String... directoryNames) throws URISyntaxException {
    return Stream.of(directoryNames)
      .map(directoryName -> new File("src/test/resources/org/sonar/plugins/surefire/api/SurefireParserTest/" + directoryName))
      .collect(Collectors.toList());
  }

  private static SensorContextTester mockContext() {
    return SensorContextTester.create(new File(""));
  }
}
