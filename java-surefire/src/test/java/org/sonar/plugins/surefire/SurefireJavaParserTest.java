/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
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
package org.sonar.plugins.surefire;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.test.IsResource;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.net.URISyntaxException;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    when(javaResourceLocator.findResourceByClassName(anyString())).thenAnswer(new Answer<Resource>() {
      @Override
      public Resource answer(InvocationOnMock invocation) throws Throwable {
        return File.create((String) invocation.getArguments()[0]);
      }
    });

    parser = new SurefireJavaParser(perspectives, javaResourceLocator);
  }

  @Test
  public void should_register_tests() throws URISyntaxException {
    SensorContext context = mockContext();

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.setDurationInMs(anyLong())).thenReturn(testCase);
    when(testCase.setStatus(any(TestCase.Status.class))).thenReturn(testCase);
    when(testCase.setMessage(anyString())).thenReturn(testCase);
    when(testCase.setStackTrace(anyString())).thenReturn(testCase);
    when(testCase.setType(anyString())).thenReturn(testCase);
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.addTestCase(anyString())).thenReturn(testCase);
    when(perspectives.as(eq(MutableTestPlan.class),
        argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")))).thenReturn(testPlan);

    parser.collect(context, getDir("multipleReports"));

    verify(testPlan).addTestCase("testGetUnKnownCollector");
    verify(testPlan).addTestCase("testGetJDependsCollector");
  }


  @Test
  public void should_store_zero_tests_when_directory_is_null_or_non_existing_or_a_file() throws Exception {
    Project project = mock(Project.class);

    SensorContext context = mockContext();
    parser.collect(context, null);
    verify(context, never()).saveMeasure(eq(CoreMetrics.TESTS), anyDouble());

    context = mockContext();
    parser.collect(context, getDir("nonExistingReportsDirectory"));
    verify(context, never()).saveMeasure(eq(CoreMetrics.TESTS), anyDouble());

    context = mockContext();
    parser.collect(context, getDir("file.txt"));
    verify(context, never()).saveMeasure(eq(CoreMetrics.TESTS), anyDouble());
  }

  @Test
  public void shouldAggregateReports() throws URISyntaxException {
    SensorContext context = mockContext();

    parser.collect(context, getDir("multipleReports"));

    // Only 6 tests measures should be stored, no more: the TESTS-AllTests.xml must not be read as there's 1 file result per unit test
    // (SONAR-2841).
    verify(context, times(6)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.SKIPPED_TESTS), eq(0.0));
    verify(context, times(6)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(6)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.TEST_ERRORS), anyDouble());
  }

  // SONAR-2841: if there's only a test suite report, then it should be read.
  @Test
  public void shouldUseTestSuiteReportIfAlone() throws URISyntaxException {
    SensorContext context = mockContext();

    parser.collect(context, getDir("onlyTestSuiteReport"));

    verify(context, times(2)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(2)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.TEST_ERRORS), anyDouble());
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  public void shouldInsertZeroWhenNoReports() throws URISyntaxException {
    SensorContext context = mockContext();
    Project project = mock(Project.class);

    parser.collect(context, getDir("noReports"));
    verify(context, never()).saveMeasure(eq(CoreMetrics.TESTS), anyDouble());

  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2371
   */
  @Test
  public void shouldNotInsertZeroWhenNoReports() throws URISyntaxException {
    SensorContext context = mockContext();
    Project project = mock(Project.class);
    when(project.getModules()).thenReturn(Arrays.asList(new Project("foo")));

    parser.collect(context, getDir("noReports"));

    verify(context, never()).saveMeasure(CoreMetrics.TESTS, 0.0);
  }

  @Test
  public void shouldNotInsertZeroOnFiles() throws URISyntaxException {
    SensorContext context = mockContext();

    parser.collect(context, getDir("noTests"));

    verify(context, never()).saveMeasure(any(Resource.class), any(Metric.class), anyDouble());
  }

  @Test
  public void shouldMergeInnerClasses() throws URISyntaxException {

    SensorContext context = mock(SensorContext.class);
    when(context.isIndexed(argThat(new ArgumentMatcher<Resource>() {
      @Override
      public boolean matches(Object o) {
        return !((Resource) o).getName().contains("$");
      }
    }), eq(false))).thenReturn(true);

    parser.collect(context, getDir("innerClasses"));

    verify(context)
        .saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "org.apache.commons.collections.bidimap.AbstractTestBidiMap")), eq(CoreMetrics.TESTS), eq(7.0));
    verify(context).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "org.apache.commons.collections.bidimap.AbstractTestBidiMap")), eq(CoreMetrics.TEST_ERRORS),
        eq(1.0));
    verify(context, never()).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet")),
        any(Metric.class), anyDouble());
  }

  @Test
  public void shouldMergeNestedInnerClasses() throws URISyntaxException {

    SensorContext context = mockContext();
    parser.collect(context, getDir("nestedInnerClasses"));

    verify(context).saveMeasure(
        argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "org.sonar.plugins.surefire.NestedInnerTest")),
        eq(CoreMetrics.TESTS),
        eq(3.0));
  }

  @Test
  public void should_not_count_negative_tests() throws URISyntaxException {
    SensorContext context = mockContext();

    parser.collect(context, getDir("negativeTestTime"));
    //Test times : -1.120, 0.644, 0.015 -> computed time : 0.659, ignore negative time.

    verify(context, times(1)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.SKIPPED_TESTS), eq(0.0));
    verify(context, times(1)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(1)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.TEST_ERRORS), anyDouble());
    verify(context, times(1)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.TEST_FAILURES), anyDouble());
    verify(context, times(1)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.FILE)), eq(CoreMetrics.TEST_EXECUTION_TIME), eq(659.0));
  }

  private java.io.File getDir(String dirname) throws URISyntaxException {
    return new java.io.File("src/test/resources/org/sonar/plugins/surefire/api/SurefireParserTest/" + dirname);
  }

  private SensorContext mockContext() {
    SensorContext context = mock(SensorContext.class);
    when(context.isIndexed(any(Resource.class), eq(false))).thenReturn(true);
    return context;
  }
}
