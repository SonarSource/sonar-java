/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisException;
import org.sonar.java.AnalysisWarningsWrapper;
import org.sonar.java.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@EnableRuleMigrationSupport
class ClasspathForMainTest {

  private MapSettings settings;
  private DefaultFileSystem fs;
  private AnalysisWarningsWrapper analysisWarnings;

  private ClasspathForMain javaClasspath;

  @Rule
  public LogTester logTester = new LogTester();

  @BeforeEach
  void setup() throws Exception {
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("foo.java"));
    settings = new MapSettings();
    analysisWarnings = mock(AnalysisWarningsWrapper.class);
  }

  /**
   * See SONARJAVA-1764
   * The fileSystem should not be used in initialization phase, as it will fail the analysis if other plugins are used.
   * Accessing the filesystem before the Sensor phase is not supported by SonarQube.
   */
  @Test
  void no_interaction_with_FileSystem_at_initialization() {
    fs = Mockito.spy(new DefaultFileSystem(new File("src/test/files/classpath/")));
    javaClasspath = createJavaClasspath();
    Mockito.verifyZeroInteractions(fs);
    Mockito.verifyZeroInteractions(analysisWarnings);
  }

  @Test
  void properties() throws Exception {
    assertThat(ClasspathProperties.getProperties()).hasSize(5);
  }

  @Test
  void when_property_not_defined_project_classpath_null_getElements_should_be_empty() {
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).isEmpty();
  }

  @Test
  void register_warning_for_missing_bytecode_when_libraries_empty_and_have_java_sources() {
    javaClasspath = createJavaClasspath();
    javaClasspath.init();
    assertThat(javaClasspath.getFilesFromProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES)).isEmpty();
    assertThat(javaClasspath.hasJavaSources()).isTrue();
    String warning = "Bytecode of dependencies was not provided for analysis of source files, " +
      "you might end up with less precise results. Bytecode can be provided using sonar.java.libraries property.";
    verify(analysisWarnings).addUnique(eq(warning));
  }

  @Test
  void do_not_register_warning_for_missing_bytecode_when_wrapper_not_injected() {
    javaClasspath = new ClasspathForMain(settings.asConfig(), fs);
    javaClasspath.init();
    assertThat(javaClasspath.getFilesFromProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES)).isEmpty();
    assertThat(javaClasspath.hasJavaSources()).isTrue();
    verifyZeroInteractions(analysisWarnings);
  }

  @Test
  void setting_binary_prop_should_fill_elements() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_BINARIES, "bin");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getBinaryDirs()).hasSize(1);
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  void setting_binary_dir_prop_should_fill_elements() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_BINARIES, "bin/");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getBinaryDirs()).hasSize(1);
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  void setting_library_prop_should_fill_elements() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/hello.jar");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getBinaryDirs()).isEmpty();
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  void absolute_file_name_should_be_resolved() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, new File("src/test/files/classpath/lib/hello.jar").getAbsolutePath());
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  void absolute_aar_file_name_should_be_resolved() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, new File("src/test/files/classpath/lib/oklog-1.0.1.aar").getAbsolutePath());
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(1);
    assertThat(javaClasspath.getElements().get(0)).exists();
  }

  @Test
  void directory_specified_for_library_should_find_jars() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(4);
    assertThat(javaClasspath.getElements().get(0)).exists();
    assertThat(javaClasspath.getElements().get(1)).exists();
    assertThat(javaClasspath.getElements().get(2)).exists();
    assertThat(javaClasspath.getElements().get(3)).exists();
    assertThat(javaClasspath.getElements()).extracting("name").contains("lib", "hello.jar", "world.jar", "foo.jar");
  }

  @Test
  void libraries_should_accept_path_ending_with_wildcard() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/*");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(3);
    assertThat(javaClasspath.getElements().get(0)).exists();
    assertThat(javaClasspath.getElements().get(1)).exists();
    assertThat(javaClasspath.getElements().get(2)).exists();
    assertThat(javaClasspath.getElements()).extracting("name").contains("hello.jar", "world.jar", "target");
  }

  @Test
  void libraries_should_keep_order() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/world.jar,lib/hello.jar,lib/target/classes/*");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(3);
    assertThat(javaClasspath.getElements().get(0)).exists();
    assertThat(javaClasspath.getElements().get(1)).exists();
    assertThat(javaClasspath.getElements().get(2)).exists();
    assertThat(javaClasspath.getElements()).extracting("name").containsExactly("world.jar", "hello.jar", "foo.jar");
  }

  @Test
  void libraries_should_accept_relative_paths() throws Exception {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "../../files/classpath/lib/*.jar");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(2);
    assertThat(javaClasspath.getElements().get(0)).exists();
    assertThat(javaClasspath.getElements().get(1)).exists();
    assertThat(javaClasspath.getElements()).extracting("name").contains("hello.jar", "world.jar");
  }

  @Test
  void libraries_should_accept_relative_paths_with_wildcard() throws Exception {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "../../files/**/lib");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(9);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).extracting("name").containsExactlyInAnyOrder(
      "hello.jar",
      "hello.jar",
      "world.jar",
      "emptyFile.jar",
      "likeJdkJar.jar",
      "emptyArchive.jar",
      "lib",
      "lib",
      "oklog-1.0.1.aar");
  }

  @Test
  void should_not_scan_target_classes() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "../../files/classpath/lib/target/classes");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(1);
    File classes = javaClasspath.getElements().get(0);
    assertThat(classes).exists();
    assertThat(javaClasspath.getElements()).extracting("name").contains("classes");
  }

  @Test
  void libraries_should_accept_path_ending_with_wildcard_jar() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/h*.jar");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(1);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar)
      .exists()
      .hasName("hello.jar");

    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/*.jar");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(2);
    jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).extracting("name").contains("hello.jar", "world.jar");

  }

  @Test
  void directory_wildcard_should_be_resolved() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "**/*.jar");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(3);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).extracting("name").contains("hello.jar", "world.jar", "foo.jar");
  }

  @Test
  void wildcard_directory_should_resolve_libs_in_that_dir() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/**/*.jar");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(3);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).extracting("name").contains("hello.jar", "world.jar", "foo.jar");
  }

  @Test
  void both_path_separator_should_be_supported_on_one_JVM() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "**/*.jar");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(3);
    File jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).extracting("name").contains("hello.jar", "world.jar", "foo.jar");
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "**\\*.jar");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(3);
    jar = javaClasspath.getElements().get(0);
    assertThat(jar).exists();
    assertThat(javaClasspath.getElements()).extracting("name").contains("hello.jar", "world.jar", "foo.jar");
  }

  @Test
  void non_existing_resources_should_fail() throws Exception {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "toto/**/hello.jar");
    checkIllegalStateException("No files nor directories matching 'toto/**/hello.jar'");
  }

  @Test
  void deprecated_properties_set_should_fail_the_analysis() throws Exception {
    settings.setProperty("sonar.binaries", "bin");
    settings.setProperty("sonar.libraries", "hello.jar");
    javaClasspath = createJavaClasspath();
    try {
      javaClasspath.getElements();
      fail("Exception should have been raised");
    } catch (AnalysisException ise) {
      assertThat(ise.getMessage())
        .isEqualTo("sonar.binaries and sonar.libraries are not supported since version 4.0 of the SonarSource Java Analyzer, please use sonar.java.binaries and sonar.java.libraries instead");
    }
  }

  @Test
  void libraries_should_read_dir_of_class_files() {
    fs = new DefaultFileSystem(new File("src/test/files/"));
    fs.add(TestUtils.emptyInputFile("foo.java"));
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "classpath");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(4);
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "classpath/");
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).hasSize(4);
  }

  @Test
  void parent_module_should_not_validate_sonar_libraries() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "non-existing.jar");
    javaClasspath = createJavaClasspath();
    checkIllegalStateException("No files nor directories matching 'non-existing.jar'");
  }

  @Test
  void sonar_binaries_should_not_check_for_existence_of_files_when_no_sources() throws Exception {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_BINARIES, "toto/**/hello.jar");
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("plop.java", InputFile.Type.TEST));
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).isEmpty();
  }

  @Test
  void empty_binaries_on_project_with_more_than_one_source_should_fail() throws Exception {
    createTwoFilesInFileSystem();
    javaClasspath = createJavaClasspath();
    try {
      javaClasspath.getElements();
      fail("Exception should have been raised");
    } catch (AnalysisException ise) {
      assertThat(ise.getMessage())
        .isEqualTo("Your project contains .java files, please provide compiled classes with sonar.java.binaries property,"
          + " or exclude them from the analysis with sonar.exclusions property.");
    }
  }

  @Test
  void empty_binaries_on_project_with_more_than_one_source_should_fail_on_sonarqube() throws Exception {
    createTwoFilesInFileSystem();
    javaClasspath = createJavaClasspath();
    try {
      javaClasspath.getElements();
      fail("Exception should have been raised");
    } catch (AnalysisException ise) {
      assertThat(ise.getMessage())
        .isEqualTo("Your project contains .java files, please provide compiled classes with sonar.java.binaries property,"
          + " or exclude them from the analysis with sonar.exclusions property.");
    }
  }

  @Test
  void empty_binaries_on_project_with_more_than_one_source_should_not_fail_on_sonarlint() throws Exception {
    createTwoFilesInFileSystem();
    try {
      javaClasspath = new ClasspathForMainForSonarLint(settings.asConfig(), fs);
      javaClasspath.getElements();

      logTester.logs(LoggerLevel.WARN).contains("sonar.java.binaries is empty, please double check your configuration");
    } catch (AnalysisException ise) {
      fail("Analysis exception was raised but analysis should not fail");
    }
  }

  private void createTwoFilesInFileSystem() {
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("plop.java"));
    fs.add(TestUtils.emptyInputFile("bar.java"));
  }

  @Test
  void classpath_empty_if_only_test_files() throws Exception {
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("plop.java", InputFile.Type.TEST));
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).isEmpty();
  }

  @Test
  void validate_libraries_only_if_not_filtered_out() throws Exception {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, new File("src/test/files/classpath/lib/lib.so").getAbsolutePath());
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("plop.java"));
    javaClasspath = createJavaClasspath();
    assertThat(javaClasspath.getElements()).isEmpty();
  }

  @Test
  void invalid_sonar_java_binaries_should_fail_analysis() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_BINARIES, "dummyDir");
    checkIllegalStateException("No files nor directories matching 'dummyDir'");
  }

  private void checkIllegalStateException(String message) {
    javaClasspath = createJavaClasspath();
    try {
      javaClasspath.getElements();
      fail("Exception should have been raised");
    } catch (IllegalStateException ise) {
      assertThat(ise.getMessage()).isEqualTo(message);
    }
  }

  private ClasspathForMain createJavaClasspath() {
    return new ClasspathForMain(settings.asConfig(), fs, analysisWarnings);
  }
}
