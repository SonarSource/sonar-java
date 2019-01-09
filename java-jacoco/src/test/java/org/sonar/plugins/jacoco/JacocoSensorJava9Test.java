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

import org.junit.Test;

import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.JavaClasspath;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.jacoco.JaCoCoExtensions.REPORT_PATHS_PROPERTY;

public class JacocoSensorJava9Test {

  @Test
  public void test_jacoco_java9_coverage() throws Exception {
    Path baseDir = Paths.get(getClass().getResource("/org/sonar/plugins/jacoco/JaCoCoJava9Test/").toURI());
    Files.copy(baseDir.resolve("Hello9.class.toCopy"), baseDir.resolve("Hello9.class"), StandardCopyOption.REPLACE_EXISTING);

    DefaultInputFile resource = new TestInputFileBuilder("", "").setLines(10).build();
    JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenReturn(resource);

    JavaClasspath javaClasspath = mock(JavaClasspath.class);
    when(javaClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(baseDir.toFile()));

    SensorContextTester context = SensorContextTester.create(baseDir);
    context.settings().setProperty(REPORT_PATHS_PROPERTY, "jacoco.exec");

    JaCoCoSensor sensor = new JaCoCoSensor(null, javaResourceLocator, javaClasspath);

    sensor.execute(context);
    assertThat(context.lineHits(resource.key(), 5)).isEqualTo(1);
  }
}
