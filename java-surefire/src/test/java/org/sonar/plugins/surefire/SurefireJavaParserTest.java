/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SurefireJavaParserTest {

  private ResourcePerspectives perspectives;
  private JavaResourceLocator javaResourceLocator;
  private SurefireJavaParser parser;

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void before() {
    perspectives = mock(ResourcePerspectives.class);

    javaResourceLocator = mock(JavaResourceLocator.class);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenAnswer(invocation -> new TestInputFileBuilder("", (String) invocation.getArguments()[0]).build());

    parser = new SurefireJavaParser(perspectives, javaResourceLocator);
  }

  @Test
  public void should_register_tests() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.setDurationInMs(anyLong())).thenReturn(testCase);
    when(testCase.setStatus(any(TestCase.Status.class))).thenReturn(testCase);
    when(testCase.setMessage(isNull())).thenReturn(testCase);
    when(testCase.setStackTrace(anyString())).thenReturn(testCase);
    when(testCase.setType(anyString())).thenReturn(testCase);
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.addTestCase(anyString())).thenReturn(testCase);
    when(perspectives.as(eq(MutableTestPlan.class),
      argThat((ArgumentMatcher<InputFile>) o -> ":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest".equals(o.key())))).thenReturn(testPlan);

    parser.collect(context, getDirs("multipleReports"), true);

    verify(testPlan).addTestCase("testGetUnKnownCollector");
    verify(testPlan).addTestCase("testGetJDependsCollector");
  }


  @Test
  public void should_store_zero_tests_when_directory_is_null_or_non_existing_or_a_file() throws Exception {

    SensorContext context = mock(SensorContext.class);

    context = mock(SensorContext.class);
    parser.collect(context, getDirs("nonExistingReportsDirectory"), false);
    verify(context, never()).newMeasure();

    context = mock(SensorContext.class);
    parser.collect(context, getDirs("file.txt"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  public void shouldAggregateReports() throws URISyntaxException {
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
  public void shouldAggregateReportsFromMultipleDirectories() throws URISyntaxException {
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
  public void shouldUseTestSuiteReportIfAlone() throws URISyntaxException {
    SensorContextTester context = mockContext();

    parser.collect(context, getDirs("onlyTestSuiteReport"), true);
    assertThat(context.measures(":org.sonar.SecondTest")).hasSize(5);
    assertThat(context.measures(":org.sonar.JavaNCSSCollectorTest")).hasSize(5);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  public void shouldInsertZeroWhenNoReports() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("noReports"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  public void shouldNotInsertZeroOnFiles() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDirs("noTests"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  public void shouldMergeInnerClasses() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("innerClasses"), true);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TESTS).value()).isEqualTo(7);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
    assertThat(context.measures(":org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet")).isEmpty();
  }

  @Test
  public void shouldMergeNestedInnerClasses() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDirs("nestedInnerClasses"), true);
    assertThat(context.measure(":org.sonar.plugins.surefire.NestedInnerTest", CoreMetrics.TESTS).value()).isEqualTo(3);
  }

  @Test
  public void should_not_count_negative_tests() throws URISyntaxException {
    SensorContextTester context = mockContext();

    parser.collect(context, getDirs("negativeTestTime"), true);
    //Test times : -1.120, 0.644, 0.015 -> computed time : 0.659, ignore negative time.
    assertThat(context.measure(":java.Foo", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(0);
    assertThat(context.measure(":java.Foo", CoreMetrics.TESTS).value()).isEqualTo(6);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_ERRORS).value()).isEqualTo(0);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(0);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(659);
  }

  @Test
  public void should_handle_parameterized_tests() throws URISyntaxException {
    SensorContextTester context = mockContext();
    when(javaResourceLocator.findResourceByClassName(anyString()))
      .thenAnswer(invocation -> {
        String className = (String) invocation.getArguments()[0];
        if (className.equals("org.foo.Junit4ParameterizedTest")
          || className.startsWith("org.foo.Junit5_0ParameterizedTest")
          || className.startsWith("org.foo.Junit5_1ParameterizedTest")) {
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

    // test file with expected fix from JUnit 5.1 (TODO: to be confirmed once 5.1 released)
    assertThat(context.measure(":org.foo.Junit5_1ParameterizedTest", CoreMetrics.TESTS).value()).isEqualTo(13);
    assertThat(context.measure(":org.foo.Junit5_1ParameterizedTest", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(48);
  }

  @Test
  public void should_log_missing_resource_with_debug_level() throws Exception {
    SensorContextTester context = mockContext();
    parser =  new SurefireJavaParser(mock(ResourcePerspectives.class), mock(JavaResourceLocator.class));
    parser.collect(context, getDirs("resourceNotFound"), true);
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Resource not found: org.sonar.Foo");
  }

  private List<File> getDirs(String... directoryNames) throws URISyntaxException {
    return Stream.of(directoryNames)
      .map(directoryName -> new File("src/test/resources/org/sonar/plugins/surefire/api/SurefireParserTest/" + directoryName))
      .collect(Collectors.toList());
  }

  private SensorContextTester mockContext() {
    return SensorContextTester.create(new File(""));
  }
}
