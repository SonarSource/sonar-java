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
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JaCoCoOverallSensorTest {

  private JacocoConfiguration configuration;
  private SensorContextTester context;
  private DefaultFileSystem fileSystem;
  private PathResolver pathResolver;
  private ResourcePerspectives perspectives;
  private JaCoCoOverallSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath = mock(JavaClasspath.class);

  @Before
  public void before() {
    configuration = mock(JacocoConfiguration.class);
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    context = SensorContextTester.create(new File(""));
    fileSystem = context.fileSystem();
    fileSystem.setWorkDir(new File("target/sonar"));
    pathResolver = mock(PathResolver.class);
    perspectives = mock(ResourcePerspectives.class);
    sensor = new JaCoCoOverallSensor(configuration, perspectives, fileSystem, pathResolver, javaResourceLocator, javaClasspath);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoOverallSensor");
  }

  @Test
  public void should_execute_if_both_report_exists() {
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.exec"));
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.exec"));
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(configuration.getReportPath()).thenReturn("ut.exec");

    assertThat(sensor.shouldExecuteOnProject()).isTrue();
  }

  @Test
  public void execute_when_it_report_does_not_exists() {
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.exec"));
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.not.found.exec"));
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(configuration.getReportPath()).thenReturn("ut.exec");
    assertThat(sensor.shouldExecuteOnProject()).isTrue();
  }

  @Test
  public void execute_when_ut_report_does_not_exists() {
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(new File(outputDir, "ut.not.found.exec"));
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.exec"));
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(configuration.getReportPath()).thenReturn("ut.exec");
    assertThat(sensor.shouldExecuteOnProject()).isTrue();
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
    when(configuration.shouldExecuteOnProject(false)).thenReturn(true);
    InputFile resource = analyseReports("ut.not.found.exec", "it.not.found.exec");
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    int[] oneHitlines = new int[] {};
    int[] zeroHitlines = new int[] {3, 6, 7, 10, 11, 14, 15, 17, 18, 20, 23, 24};
    verifyOverallMetrics(resource, zeroHitlines, oneHitlines, 0);
  }

  private void verifyOverallMetrics(InputFile resource,int[] zeroHitlines, int[] oneHitlines, int coveredConditions) {
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
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    File to = new File(outputDir, "HelloWorld.class");
    Files.copy(TestUtils.getResource("HelloWorld.class.toCopy"), to);
    DefaultInputFile resource = new DefaultInputFile("", "");
    resource.setLines(25);

    when(javaResourceLocator.findResourceByClassName("com/sonar/coverages/HelloWorld")).thenReturn(resource);
    when(configuration.getReportPath()).thenReturn(utReport);
    when(configuration.getItReportPath()).thenReturn(itReport);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), eq(utReport))).thenReturn(new File(outputDir, utReport));
    when(pathResolver.relativeFile(any(File.class), eq(itReport))).thenReturn(new File(outputDir, itReport));
    when(pathResolver.relativeFile(any(File.class), eq(new File("target/sonar/jacoco-overall.exec").getAbsolutePath()))).thenReturn(new File("target/sonar/jacoco-overall.exec"));

    sensor.execute(context);
    return resource;
  }
}
