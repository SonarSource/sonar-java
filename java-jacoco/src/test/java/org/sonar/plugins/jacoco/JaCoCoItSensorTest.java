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
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JaCoCoItSensorTest {
  private static File outputDir;
  private static File jacocoExecutionData;

  private JacocoConfiguration configuration;
  private PathResolver pathResolver;
  private JaCoCoItSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath = mock(JavaClasspath.class);

  @BeforeClass
  public static void setUpOutputDir() throws IOException {
    outputDir = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/");
    jacocoExecutionData = new File(outputDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(jacocoExecutionData.getParentFile(), "Hello.class"));
  }

  @Before
  public void setUp() {
    configuration = mock(JacocoConfiguration.class);
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    FileSystem fileSystem = new DefaultFileSystem((File)null);
    pathResolver = mock(PathResolver.class);
    sensor = new JaCoCoItSensor(configuration, perspectives, fileSystem, pathResolver, javaResourceLocator, javaClasspath);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoItSensor");
  }

  @Test
  public void shouldExecuteIfReportPathIsDefined() {
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.exec"));
    assertThat(sensor.shouldExecuteOnProject()).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("it.exec"))).thenReturn(new File(outputDir, "it.not.found.exec"));
    assertThat(sensor.shouldExecuteOnProject()).isFalse();
  }

  @Test
  public void testReadExecutionData() {
    DefaultInputFile resource = new DefaultInputFile("", "org/sonar/plugins/jacoco/tests/Hello");
    resource.setLines(19);
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(javaResourceLocator.findResourceByClassName("org/sonar/plugins/jacoco/tests/Hello")).thenReturn(resource);
    SensorContextTester context = SensorContextTester.create(new File(""));
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));
    when(pathResolver.relativeFile(any(File.class), any(String.class))).thenReturn(jacocoExecutionData);

    sensor.execute(context);

    int[] oneHitlines = new int[] {6, 7, 8, 11};
    int[] zeroHitlines = new int[] {15, 16, 18};
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(":org/sonar/plugins/jacoco/tests/Hello", zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(":org/sonar/plugins/jacoco/tests/Hello", oneHitline)).isEqualTo(1);
    }
    assertThat(context.conditions(":org/sonar/plugins/jacoco/tests/Hello", 15)).isEqualTo(2);
    assertThat(context.coveredConditions(":org/sonar/plugins/jacoco/tests/Hello", 15)).isEqualTo(0);
  }

  @Test
  public void doNotSaveMeasureOnResourceWhichDoesntExistInTheContext() {
    SensorContext context = mock(SensorContext.class);
    when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(jacocoExecutionData);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(outputDir));

    sensor.execute(context);

    verify(context, never()).newCoverage();
  }

}
