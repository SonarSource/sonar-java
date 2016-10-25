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
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.test.TestUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.jacoco.JacocoConstants.IT_REPORT_PATH_PROPERTY;

public class JaCoCoItSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private static File jacocoExecutionData;

  private JaCoCoItSensor sensor;
  private JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
  private JavaClasspath javaClasspath = mock(JavaClasspath.class);

  private static File baseDir;

  private SensorContextTester context;

  @Before
  public void setUpOutputDir() throws IOException {
    baseDir = temp.newFolder();
    jacocoExecutionData = new File(baseDir, "jacoco.exec");

    Files.copy(TestUtils.getResource("Hello.class.toCopy"), new File(baseDir, "Hello.class"));
    Files.copy(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCoSensorTest/jacoco.exec"), jacocoExecutionData);

    context = SensorContextTester.create(baseDir)
      .setSettings(new MapSettings(new PropertyDefinitions(JacocoConstants.getPropertyDefinitions(Version.create(5, 2)))));
  }

  @Before
  public void setUp() {
    ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
    sensor = new JaCoCoItSensor(perspectives, javaResourceLocator, javaClasspath);
  }

  @Test
  public void testSensorDescriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("JaCoCoIt");
  }

  @Test
  public void logIfInvalidReportPath() {
    context.settings().setProperty(IT_REPORT_PATH_PROPERTY, "unknown.exec");
    sensor.execute(context);
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("JaCoCo IT report not found: 'unknown.exec'");
  }

  @Test
  public void testReadExecutionData() {
    context.settings().setProperty(IT_REPORT_PATH_PROPERTY, "jacoco.exec");
    DefaultInputFile file = new DefaultInputFile(context.module().key(), "org/sonar/plugins/jacoco/tests/Hello")
      .setLines(19);
    when(javaResourceLocator.findResourceByClassName("org/sonar/plugins/jacoco/tests/Hello")).thenReturn(file);
    when(javaClasspath.getBinaryDirs()).thenReturn(ImmutableList.of(baseDir));

    sensor.execute(context);

    int[] oneHitlines = new int[] {6, 7, 8, 11};
    int[] zeroHitlines = new int[] {15, 16, 18};
    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(file.key(), zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(file.key(), oneHitline)).isEqualTo(1);
    }
    assertThat(context.conditions(file.key(), 15)).isEqualTo(2);
    assertThat(context.coveredConditions(file.key(), 15)).isEqualTo(0);
  }

}
