/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SurefireJavaParserTest {

  private ResourcePerspectives perspectives;
  private JavaResourceLocator javaResourceLocator;
  private SurefireJavaParser parser;

  @Before
  public void before() {
    perspectives = mock(ResourcePerspectives.class);

    javaResourceLocator = mock(JavaResourceLocator.class);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenAnswer(invocation -> new DefaultInputFile("", (String) invocation.getArguments()[0]));

    parser = new SurefireJavaParser(perspectives, javaResourceLocator);
  }

  @Test
  public void should_register_tests() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.setDurationInMs(anyLong())).thenReturn(testCase);
    when(testCase.setStatus(any(TestCase.Status.class))).thenReturn(testCase);
    when(testCase.setMessage(anyString())).thenReturn(testCase);
    when(testCase.setStackTrace(anyString())).thenReturn(testCase);
    when(testCase.setType(anyString())).thenReturn(testCase);
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.addTestCase(anyString())).thenReturn(testCase);
    when(perspectives.as(eq(MutableTestPlan.class), argThat(new ArgumentMatcher<InputFile>() {
      @Override
      public boolean matches(Object o) {
        if(o instanceof InputFile) {
          return ":ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest".equals(((InputFile) o).key());
        }
        return false;
      }
    }))).thenReturn(testPlan);

    parser.collect(context, getDir("multipleReports"), true);

    verify(testPlan).addTestCase("testGetUnKnownCollector");
    verify(testPlan).addTestCase("testGetJDependsCollector");
  }


  @Test
  public void should_store_zero_tests_when_directory_is_null_or_non_existing_or_a_file() throws Exception {

    SensorContext context = mock(SensorContext.class);
    parser.collect(context, null, false);
    verify(context, never()).newMeasure();

    context = mock(SensorContext.class);
    parser.collect(context, getDir("nonExistingReportsDirectory"), false);
    verify(context, never()).newMeasure();

    context = mock(SensorContext.class);
    parser.collect(context, getDir("file.txt"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  public void shouldAggregateReports() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDir("multipleReports"), true);
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

    parser.collect(context, getDir("onlyTestSuiteReport"), true);
    assertThat(context.measures(":org.sonar.SecondTest")).hasSize(5);
    assertThat(context.measures(":org.sonar.JavaNCSSCollectorTest")).hasSize(5);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  public void shouldInsertZeroWhenNoReports() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDir("noReports"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  public void shouldNotInsertZeroOnFiles() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    parser.collect(context, getDir("noTests"), true);
    verify(context, never()).newMeasure();
  }

  @Test
  public void shouldMergeInnerClasses() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDir("innerClasses"), true);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TESTS).value()).isEqualTo(7);
    assertThat(context.measure(":org.apache.commons.collections.bidimap.AbstractTestBidiMap", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
    assertThat(context.measures(":org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet")).isEmpty();
  }

  @Test
  public void shouldMergeNestedInnerClasses() throws URISyntaxException {
    SensorContextTester context = mockContext();
    parser.collect(context, getDir("nestedInnerClasses"), true);
    assertThat(context.measure(":org.sonar.plugins.surefire.NestedInnerTest", CoreMetrics.TESTS).value()).isEqualTo(3);
  }

  @Test
  public void should_not_count_negative_tests() throws URISyntaxException {
    SensorContextTester context = mockContext();

    parser.collect(context, getDir("negativeTestTime"), true);
    //Test times : -1.120, 0.644, 0.015 -> computed time : 0.659, ignore negative time.
    assertThat(context.measure(":java.Foo", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(0);
    assertThat(context.measure(":java.Foo", CoreMetrics.TESTS).value()).isEqualTo(6);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_ERRORS).value()).isEqualTo(0);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(0);
    assertThat(context.measure(":java.Foo", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(659);
  }

  private java.io.File getDir(String dirname) throws URISyntaxException {
    return new java.io.File("src/test/resources/org/sonar/plugins/surefire/api/SurefireParserTest/" + dirname);
  }

  private SensorContextTester mockContext() {
    return SensorContextTester.create(new File(""));
  }
}
