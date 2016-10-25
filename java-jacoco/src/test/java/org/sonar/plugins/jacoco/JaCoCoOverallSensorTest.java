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
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.Version;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.test.TestUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.jacoco.JacocoConstants.IT_REPORT_PATH_PROPERTY;
import static org.sonar.plugins.jacoco.JacocoConstants.REPORT_PATH_PROPERTY;

public class JaCoCoOverallSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SensorContextTester context;
  private ResourcePerspectives perspectives;
  private JaCoCoOverallSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath = mock(JavaClasspath.class);

  @Before
  public void before() throws IOException {
    context = SensorContextTester.create(temp.newFolder())
      .setSettings(new MapSettings(new PropertyDefinitions(JacocoConstants.getPropertyDefinitions(Version.create(5, 6)))));
    context.fileSystem().setWorkDir(new File(context.fileSystem().baseDir(), "work"));
    perspectives = mock(ResourcePerspectives.class);
    sensor = new JaCoCoOverallSensor(perspectives, javaResourceLocator, javaClasspath);
  }

  @Test
  public void testSensorDescriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("JaCoCoOverall");
  }

  @Test
  public void should_save_measures() throws IOException {
    InputFile resource = analyseReports("ut.exec", "it.exec");
    int[] oneHitlines = new int[] {3, 6, 7, 10, 11, 14, 15, 17, 18, 20};
    int[] zeroHitlines = new int[] {23, 24};
    verifyOverallMetrics(resource, zeroHitlines, oneHitlines, 2);
  }

  @Test
  public void should_save_measures_when_it_report_is_not_found() throws IOException {
    InputFile resource = analyseReports("ut.exec", "it.not.found.exec");
    int[] oneHitlines = new int[] {3, 6, 7, 14, 15, 20};
    int[] zeroHitlines = new int[] {10, 11, 17, 18, 23, 24};
    verifyOverallMetrics(resource, zeroHitlines, oneHitlines, 1);
  }

  @Test
  public void should_save_measures_when_ut_report_is_not_found() throws IOException {
    InputFile resource = analyseReports("ut.not.found.exec", "it.exec");
    int[] oneHitlines = new int[] {3, 10, 11, 14, 17, 18, 20};
    int[] zeroHitlines = new int[] {6, 7, 15, 23, 24};
    verifyOverallMetrics(resource, zeroHitlines, oneHitlines, 1);
  }

  @Test
  public void should_save_measures_when_no_reports_and_force_property() throws IOException {
    context.settings().setProperty(JacocoConstants.REPORT_MISSING_FORCE_ZERO, "true");
    InputFile resource = analyseReports("ut.not.found.exec", "it.not.found.exec");
    int[] oneHitlines = new int[] {};
    int[] zeroHitlines = new int[] {3, 6, 7, 10, 11, 14, 15, 17, 18, 20, 23, 24};
    verifyOverallMetrics(resource, zeroHitlines, oneHitlines, 0);
  }

  private void verifyOverallMetrics(InputFile resource, int[] zeroHitlines, int[] oneHitlines, int coveredConditions) {
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(resource.key(), zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(resource.key(), oneHitline)).isEqualTo(1);
    }
    assertThat(context.conditions(resource.key(), 14)).isEqualTo(2);
    assertThat(context.coveredConditions(resource.key(), 14)).isEqualTo(coveredConditions);
  }

  private InputFile analyseReports(String utReport, String itReport) throws IOException {
    File utReportFile = TestUtils.getResource(JaCoCoOverallSensorTest.class, utReport);
    if (utReportFile != null) {
      Files.copy(utReportFile, new File(context.fileSystem().baseDir(), utReport));
    }
    File itReportFile = TestUtils.getResource(JaCoCoOverallSensorTest.class, itReport);
    if (itReportFile != null) {
      Files.copy(itReportFile, new File(context.fileSystem().baseDir(), itReport));
    }
    File to = new File(context.fileSystem().baseDir(), "HelloWorld.class");
    Files.copy(TestUtils.getResource("HelloWorld.class.toCopy"), to);
    DefaultInputFile resource = new DefaultInputFile("", "");
    resource.setLines(25);

    when(javaResourceLocator.findResourceByClassName("com/sonar/coverages/HelloWorld")).thenReturn(resource);
    context.settings().setProperty(REPORT_PATH_PROPERTY, utReport);
    context.settings().setProperty(IT_REPORT_PATH_PROPERTY, itReport);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(context.fileSystem().baseDir()));

    sensor.execute(context);
    return resource;
  }
}
