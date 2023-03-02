/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.classpath;

import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ClasspathForTestTest {

  private DefaultFileSystem fs;
  private MapSettings settings;
  private ClasspathForTest javaTestClasspath;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @BeforeEach
  public void setUp() throws Exception {
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("foo.java", InputFile.Type.TEST));
    settings = new MapSettings();
  }

  /**
   * See SONARJAVA-1764
   * The fileSystem should not be used in initialization phase, as it will fail the analysis if other plugins are used.
   * Accessing the filesystem before the Sensor phase is not supported by SonarQube.
   */
  @Test
  void no_interaction_with_FileSystem_at_initialization() {
    fs = Mockito.spy(new DefaultFileSystem(new File("src/test/files/classpath/")));
    javaTestClasspath = new ClasspathForTest(settings.asConfig(), fs);
    Mockito.verifyZeroInteractions(fs);
  }

  @Test
  void display_warning_for_missing_bytecode_when_libraries_empty_and_have_java_sources() {
    javaTestClasspath = createJavaClasspath();
    javaTestClasspath.init();
    assertThat(javaTestClasspath.getFilesFromProperty(ClasspathProperties.SONAR_JAVA_TEST_LIBRARIES)).isEmpty();
    assertThat(javaTestClasspath.hasJavaSources()).isTrue();

    javaTestClasspath.logSuspiciousEmptyLibraries();

    String warning = "Dependencies/libraries were not provided for analysis of TEST files. The 'sonar.java.test.libraries' property is empty. "
      + "Verify your configuration, as you might end up with less precise results.";
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactly(warning);
  }

  @Test
  void only_display_once_warning_for_missing_bytecode_when_libraries_empty_and_have_java_sources() {
    javaTestClasspath = createJavaClasspath();
    javaTestClasspath.init();
    assertThat(javaTestClasspath.getFilesFromProperty(ClasspathProperties.SONAR_JAVA_TEST_LIBRARIES)).isEmpty();
    assertThat(javaTestClasspath.hasJavaSources()).isTrue();

    javaTestClasspath.logSuspiciousEmptyLibraries();

    //re-trigger logs
    javaTestClasspath.logSuspiciousEmptyLibraries();

    String warning = "Dependencies/libraries were not provided for analysis of TEST files. The 'sonar.java.test.libraries' property is empty. "
      + "Verify your configuration, as you might end up with less precise results.";
    assertThat(logTester.logs(LoggerLevel.WARN)).containsExactly(warning);
  }

  @Test
  void no_warning_for_missing_bytecode_when_libraries_empty_and_have_no_java_sources() {
    javaTestClasspath = new ClasspathForTest(settings.asConfig(), new DefaultFileSystem(new File("src/test/files/classpath/")));
    javaTestClasspath.init();
    assertThat(javaTestClasspath.getFilesFromProperty(ClasspathProperties.SONAR_JAVA_TEST_LIBRARIES)).isEmpty();
    assertThat(javaTestClasspath.hasJavaSources()).isFalse();

    javaTestClasspath.logSuspiciousEmptyLibraries();

    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  void libraries_should_accept_path_ending_with_wildcard() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_TEST_LIBRARIES, "lib/*");
    javaTestClasspath = createJavaClasspath();
    assertThat(javaTestClasspath.getElements()).hasSize(3);
    assertThat(javaTestClasspath.getElements().get(0)).exists();
    assertThat(javaTestClasspath.getElements().get(1)).exists();
    assertThat(javaTestClasspath.getElements().get(2)).exists();
    assertThat(javaTestClasspath.getElements()).extracting("name").contains("hello.jar", "world.jar", "target");
  }

  @Test
  void empty_libraries() throws Exception {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_TEST_LIBRARIES, "");
    javaTestClasspath = createJavaClasspath();
    assertThat(javaTestClasspath.getElements()).isEmpty();
  }

  @Test
  void empty_libraries_if_only_main_files() throws Exception {
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("plop.java"));
    javaTestClasspath = createJavaClasspath();
    assertThat(javaTestClasspath.getElements()).isEmpty();
  }

  @Test
  void libraries_without_dir() throws Exception {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_TEST_BINARIES, "bin");
    settings.setProperty(ClasspathProperties.SONAR_JAVA_TEST_LIBRARIES, "hello.jar");
    checkIllegalStateException("No files nor directories matching 'hello.jar'");
  }

  private void checkIllegalStateException(String message) {
    javaTestClasspath = createJavaClasspath();
    try {
      javaTestClasspath.getElements();
      fail("Exception should have been raised");
    }catch (IllegalStateException ise) {
      assertThat(ise.getMessage()).isEqualTo(message);
    }
  }


  private ClasspathForTest createJavaClasspath() {
    return new ClasspathForTest(settings.asConfig(), fs);
  }


}
