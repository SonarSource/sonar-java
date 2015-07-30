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
package org.sonar.java;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class JavaTestClasspathTest {

  private Project project;
  private DefaultFileSystem fs;
  private Settings settings;
  private JavaTestClasspath javaTestClasspath;

  @Before
  public void setUp() throws Exception {
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    DefaultInputFile inputFile = new DefaultInputFile("foo.java");
    inputFile.setLanguage("java");
    inputFile.setType(InputFile.Type.TEST);
    fs.add(inputFile);
    settings = new Settings();
    project = mock(Project.class);
  }

  @Test
  public void libraries_should_accept_path_ending_with_wildcard() {
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_TEST_LIBRARIES, "lib/*");
    javaTestClasspath = createJavaClasspath();
    assertThat(javaTestClasspath.getElements()).hasSize(2);
    assertThat(javaTestClasspath.getElements().get(0)).exists();
    assertThat(javaTestClasspath.getElements().get(1)).exists();
    assertThat(javaTestClasspath.getElements()).onProperty("name").contains("hello.jar", "world.jar");
  }

  @Test
  public void libraries_without_dir() throws Exception {
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_TEST_BINARIES, "bin");
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_TEST_LIBRARIES, "hello.jar");
    fs.setBaseDir(new File("src/test/files/classpath/"));
    checkIllegalStateException("No files nor directories matching 'hello.jar'");
  }

  private void checkIllegalStateException(String message) {
    try {
      javaTestClasspath = createJavaClasspath();
      javaTestClasspath.getElements();
      fail("Exception should have been raised");
    }catch (IllegalStateException ise) {
      assertThat(ise.getMessage()).isEqualTo(message);
    }
  }


  private JavaTestClasspath createJavaClasspath() {
    return new JavaTestClasspath(project, settings, fs);
  }


}