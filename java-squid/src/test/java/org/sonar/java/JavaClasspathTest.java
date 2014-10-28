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
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.Settings;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
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
    assertThat(JavaClasspathProperties.getProperties()).hasSize(2);
  }

  @Test
  public void when_property_not_defined_project_classpath_null_getElements_should_be_empty() {
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).isEmpty();
  }

  @Test
  public void new_properties_not_set_should_fall_back_on_old_ones() throws Exception {
    settings.setProperty("sonar.binaries", "bin");
    settings.setProperty("sonar.libraries", "lib/hello.jar");
    fs.setBaseDir(new File("src/test/files/classpath/"));
    javaClasspath = new JavaClasspath(settings, fs, mock(MavenProject.class));
    assertThat(javaClasspath.getElements()).hasSize(2);
    assertThat(javaClasspath.getElements()).onProperty("name").contains("bin", "hello.jar");
    assertThat(javaClasspath.getBinaryDirs()).hasSize(1);
  }

  @Test
  public void old_maven_mojo_binaries_filled_but_not_libraries() throws Exception {
    settings.setProperty("sonar.binaries", "bin");
    fs.setBaseDir(new File("src/test/files/classpath/"));
    MavenProject pom = mock(MavenProject.class);
    when(pom.getCompileClasspathElements()).thenReturn(Lists.newArrayList("hello.jar"));
    Build build = mock(Build.class);
    when(build.getOutputDirectory()).thenReturn("src/test/files/classpath/bin");
    when(pom.getBuild()).thenReturn(build);
    javaClasspath = new JavaClasspath(settings, fs, pom);
    assertThat(javaClasspath.getElements()).hasSize(2);
    assertThat(javaClasspath.getElements()).onProperty("name").contains("bin", "hello.jar");
    assertThat(javaClasspath.getBinaryDirs()).hasSize(1);
  }

  @Test
  public void setting_binary_prop_should_fill_elements() {
    fs.setBaseDir(new File("src/test/files/classpath/"));
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_BINARIES, "bin");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getBinaryDirs()).hasSize(1);
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  public void setting_library_prop_should_fill_elements() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/hello.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getBinaryDirs()).isEmpty();
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  public void absolute_file_name_should_be_resolved() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, new File("src/test/files/bytecode/lib/hello.jar").getAbsolutePath());
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  public void libraries_should_accept_path_ending_with_wildcard() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/*");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    assertThat(javaClasspath.getElements().get(0)).exists();
    assertThat(javaClasspath.getElements().get(1)).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");
  }

  @Test
  public void libraries_should_accept_relative_paths() throws Exception {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "../../files/classpath/lib/*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");
  }

  @Test
  public void libraries_should_accept_path_ending_with_wildcard_jar() {
    fs.setBaseDir(new File("src/test/files/classpath"));

    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/h*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(1);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(jar.getName()).isEqualTo("hello.jar");

    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");

  }

  @Test
  public void directory_wildcard_should_be_resolved() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "**/*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");
  }

  @Test
  public void both_path_separator_should_be_supported_on_one_JVM() {
    fs.setBaseDir(new File("src/test/files/classpath"));
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "**/*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "**\\*.jar");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(2);
    jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).onProperty("name").contains("hello.jar","world.jar");
  }

  @Test
  public void non_existing_resources_should_fail() throws Exception {
    File baseDir = new File("src/test/files/classpath");
    fs.setBaseDir(baseDir);
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "toto/**/hello.jar");
    checkIllegalStateException("No files nor directories matching 'toto/**/hello.jar'");
  }

  @Test
  public void libraries_without_dir() throws Exception {
    settings.setProperty("sonar.binaries", "bin");
    settings.setProperty("sonar.libraries", "hello.jar");
    fs.setBaseDir(new File("src/test/files/classpath/"));
    checkIllegalStateException("No files nor directories matching 'hello.jar'");
  }

  @Test
  public void libraries_should_read_dir_of_class_files() {
    File baseDir = new File("src/test/files/");
    fs.setBaseDir(baseDir);
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "classpath");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(1);
    settings.setProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, "classpath/");
    javaClasspath = new JavaClasspath(settings, fs);
    assertThat(javaClasspath.getElements()).hasSize(1);
  }

  private void checkIllegalStateException(String message) {
    try {
      javaClasspath = new JavaClasspath(settings, fs);
      fail("Exception should have been raised");
    }catch (IllegalStateException ise) {
      assertThat(ise.getMessage()).isEqualTo(message);
    }
  }
}
