/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
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
package org.sonar.plugins.surefire;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsResource;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;

import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SurefireJavaParserTest {

  private ResourcePerspectives perspectives;
  private SurefireJavaParser parser;

  @Before
  public void before() {
    perspectives = mock(ResourcePerspectives.class);
    parser = new SurefireJavaParser(perspectives);
  }

  @Test
  public void should_aggregate_reports() throws URISyntaxException {
    SensorContext context = mockContext();

    parser.collect(new Project("foo"), context, getDir("multipleReports"));

    // Only 6 tests measures should be stored, no more: the TESTS-AllTests.xml must not be read as there's 1 file result per unit test
    // (SONAR-2841).
    verify(context, times(6)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.UNIT_TEST_FILE)), eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(6)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.UNIT_TEST_FILE)), eq(CoreMetrics.TEST_ERRORS), anyDouble());
    verify(context, times(6)).saveMeasure(argThat(new IsResource(Scopes.FILE, Qualifiers.UNIT_TEST_FILE)), argThat(new IsMeasure(CoreMetrics.TEST_DATA)));
  }

  @Test
  public void should_register_tests() throws URISyntaxException {
    SensorContext context = mockContext();

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.setDurationInMs(anyLong())).thenReturn(testCase);
    when(testCase.setStatus(anyString())).thenReturn(testCase);
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.addTestCase(anyString())).thenReturn(testCase);
    when(perspectives.as(argThat(new IsResource(Scopes.FILE, Qualifiers.UNIT_TEST_FILE, "ch.hortis.sonar.mvn.mc.MetricsCollectorRegistryTest")),
        eq(MutableTestPlan.class))).thenReturn(testPlan);

    parser.collect(new Project("foo"), context, getDir("multipleReports"));

    verify(testPlan).addTestCase("testGetUnKnownCollector");
    verify(testPlan).addTestCase("testGetJDependsCollector");
  }

  private java.io.File getDir(String dirname) throws URISyntaxException {
    return new java.io.File(getClass().getResource("/org/sonar/plugins/surefire/api/AbstractSurefireParserTest/" + dirname).toURI());
  }

  private SensorContext mockContext() {
    SensorContext context = mock(SensorContext.class);
    when(context.isIndexed(any(Resource.class), eq(false))).thenReturn(true);
    return context;
  }

}
