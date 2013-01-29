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
package org.sonar.plugins.java;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.nio.charset.Charset;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JavaSourceImporterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private JavaSourceImporter sensor;

  @Before
  public void setUp() {
    sensor = new JavaSourceImporter(new BaseConfiguration());
  }

  @Test
  public void should_execute_on_java_project() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn("java");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    when(project.getLanguageKey()).thenReturn("py");
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("JavaSourceImporter");
  }

  @Test
  public void shouldImportSoure() {
    SensorContext context = mock(SensorContext.class);
    JavaFile javaFile = mock(JavaFile.class);
    InputFile inputFile = InputFileUtils.create(new File("."), new File("./src/test/java/org/sonar/plugins/java/JavaSourceImporterTest.java"));

    sensor.importSource(context, javaFile, inputFile, Charset.forName("UTF-8"));
    verify(context, times(1)).saveSource(any(JavaFile.class), anyString());
  }

  /**
   * SONAR-3804
   */
  @Test
  public void shouldThrowCleanMessageIfFailToImportSoure() {
    File file = new File("./src/test/java/org/sonar/plugins/java/JavaSourceImporterTest.java");

    SensorContext context = mock(SensorContext.class);
    JavaFile javaFile = mock(JavaFile.class);
    InputFile inputFile = InputFileUtils.create(new File("."), file);
    doThrow(IllegalArgumentException.class).when(context).saveSource(any(JavaFile.class), anyString());

    thrown.expect(SonarException.class);
    thrown.expectMessage("Unable to read and import the source file : '" + file.getAbsolutePath() + "' with the charset : 'UTF-8'.");

    sensor.importSource(context, javaFile, inputFile, Charset.forName("UTF-8"));
  }
}
