/*
 * SonarQube Java
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
package org.sonar.java;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.Settings;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaClasspathTest {

  private DefaultFileSystem fs;
  private Settings settings;
  private JavaClasspath javaClasspath;

  @Before
  public void setUp() throws Exception {
    fs = new DefaultFileSystem();
    settings = new Settings();
  }

  @Test
  public void properties() throws Exception {
    assertThat(JavaClasspath.getProperties()).hasSize(2);
  }

  @Test
  public void when_property_not_defined_project_classpath_null_getElements_should_be_empty() {
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).isEmpty();
  }

  @Test
  public void when_property_not_defined_getElements_should_be_list_of_project_classpath() {
    ProjectClasspath projectClasspath = mock(ProjectClasspath.class);
    List<File> elements = Lists.newArrayList(new File("plop"));
    when(projectClasspath.getElements()).thenReturn(elements);
    javaClasspath = new JavaClasspath(settings, fs, projectClasspath);
    assertThat(javaClasspath.getElements()).isEqualTo(elements);
  }

  @Test
  public void setting_binary_prop_should_fill_elements() {
    fs.setBaseDir(new File("src/test/files/classpath/"));
    settings.setProperty(JavaClasspath.SONAR_JAVA_BINARIES, "bin");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getBinaryDirs()).hasSize(1);
    assertThat(javaClasspath.getLibraries()).isEmpty();
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  public void setting_library_prop_should_fill_elements() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspath.SONAR_JAVA_LIBRARIES, "lib/hello.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getBinaryDirs()).isEmpty();
    assertThat(javaClasspath.getLibraries()).hasSize(1);
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  public void absolute_file_name_should_be_resolved() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspath.SONAR_JAVA_LIBRARIES, new File("src/test/files/bytecode/lib/hello.jar").getAbsolutePath());
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  public void libraries_should_accept_path_ending_with_wildcard() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspath.SONAR_JAVA_LIBRARIES, "lib/*");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    assertThat(javaClasspath.getElements().get(0)).exists();
    assertThat(javaClasspath.getElements().get(1)).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");
  }

  @Test
  public void libraries_should_accept_path_ending_with_wildcard_jar() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspath.SONAR_JAVA_LIBRARIES, "lib/*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");

    settings.setProperty(JavaClasspath.SONAR_JAVA_LIBRARIES, "lib/h*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(1);
    jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(jar.getName()).isEqualTo("hello.jar");
  }

  @Test
  public void directory_wildcard_should_be_resolved() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspath.SONAR_JAVA_LIBRARIES, "**/*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");
  }

  @Test
  public void both_path_separator_should_be_supported_on_one_JVM() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspath.SONAR_JAVA_LIBRARIES, "**/*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");
    settings.setProperty(JavaClasspath.SONAR_JAVA_LIBRARIES, "**\\*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");
  }

}
