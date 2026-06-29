/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.MultivalueProperty;
import org.sonar.api.utils.System2;
import org.sonar.java.AnalysisWarningsWrapper;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.testing.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

class ClasspathForMainTest {

  private static final Path RELATIVE_BASE_PATH = Paths.get("src", "test", "files", "classpath");
  private static final Path RELATIVE_JDK_PATH = Paths.get("src", "test", "jdk");
  private Path baseDir;
  private Path jdkPath;
  private MapSettings settings;
  private DefaultFileSystem fs;
  private AnalysisWarningsWrapper analysisWarnings;

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.DEBUG);
  public List<String> analysisWarningsLogged = new ArrayList<>();

  @BeforeEach
  void setup() throws IOException {
    baseDir = RELATIVE_BASE_PATH.toRealPath();
    jdkPath = RELATIVE_JDK_PATH.toRealPath();
    Files.createDirectories(baseDir.resolve("empty"));
    fs = new DefaultFileSystem(baseDir);
    fs.add(TestUtils.emptyInputFile("foo.java"));
    PropertyDefinitions propertyDefinitions = new PropertyDefinitions(System2.INSTANCE);
    ClasspathProperties.getProperties().forEach(propertyDefinitions::addComponent);
    settings = new MapSettings(propertyDefinitions) {
      /**
       * MapSettings doesn't support CSV encoded properties, but real scanner component does (see org/sonar/scanner/config/DefaultConfiguration)
       */
      @Override
      public String[] getStringArray(@NonNull String key) {
        return get(key)
          .map(v -> MultivalueProperty.parseAsCsv(key, v))
          .orElse(new String[0]);
      }
    };
    settings.setProperty(ClasspathProperties.SONAR_JAVA_BINARIES, "empty");
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "empty");
    analysisWarnings = new AnalysisWarningsWrapper(text -> analysisWarningsLogged.add(text));
  }

  /**
   * See SONARJAVA-1764
   * The fileSystem should not be used in initialization phase, as it will fail the analysis if other plugins are used.
   * Accessing the filesystem before the Sensor phase is not supported by SonarQube.
   */
  @Test
  void no_interaction_with_FileSystem_at_initialization() {
    fs = spy(new DefaultFileSystem(new File("src/test/files/classpath/")));
    createJavaClasspath();
    verifyNoInteractions(fs);
    assertThat(logTester.logs()).isEmpty();
    assertThat(analysisWarningsLogged).isEmpty();
  }

  @Test
  void properties() {
    assertThat(ClasspathProperties.getProperties()).hasSize(5);
  }

  @Test
  void when_binaries_and_libraries_not_defined_classpath_getElements_should_be_empty_with_a_warning() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).isEmpty();
    String warning = "Missing 'sonar.java.binaries' and 'sonar.java.libraries' properties. You might end up with less precise analysis results.";
    assertThat(actual.projectWarnings()).containsExactly(warning);
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: []",
      "DEBUG Property 'sonar.java.libraries' resolved with: []",
      "WARN " + warning
    );
  }

  @Test
  void when_libraries_property_not_defined_classpath_log_a_warning() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly("<PROJECT>/empty");
    String warning = "Missing 'sonar.java.libraries' property. You might end up with less precise analysis results.";
    assertThat(actual.projectWarnings()).containsExactly(warning);
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: [<PROJECT>/empty]",
      "DEBUG Property 'sonar.java.libraries' resolved with: []",
      "WARN " + warning
    );
  }

  @Test
  void when_binaries_property_not_defined_classpath_log_a_warning_with_more_than_one_java_file() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly("<PROJECT>/empty");
    String warning = "Missing 'sonar.java.binaries' property. You might end up with less precise analysis results.";
    assertThat(actual.projectWarnings()).containsExactly(warning);
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: []",
      "DEBUG Property 'sonar.java.libraries' resolved with: [<PROJECT>/empty]",
      "WARN " + warning
    );
  }

  @Test
  void when_binaries_and_libraries_not_defined_classpath_log_no_warning_for_autoscan() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES);
    settings.setProperty(SonarComponents.SONAR_AUTOSCAN, true);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).isEmpty();
    assertThat(actual.projectWarnings()).isEmpty();
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: []",
      "DEBUG Property 'sonar.java.libraries' resolved with: []"
    );
  }

  @Test
  void when_binaries_property_not_defined_classpath_does_not_log_a_warning_if_only_one_java_file() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly("<PROJECT>/empty");
    assertThat(actual.projectWarnings()).isEmpty();
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: []",
      "DEBUG Property 'sonar.java.libraries' resolved with: [<PROJECT>/empty]"
    );
  }

  @Test
  void when_binaries_property_not_defined_classpath_does_not_log_a_warning_if_no_java_file() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly("<PROJECT>/empty");
    assertThat(actual.projectWarnings()).isEmpty();
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: []",
      "DEBUG Property 'sonar.java.libraries' resolved with: [<PROJECT>/empty]"
    );
  }

  @Test
  void only_display_once_warning_for_missing_bytecode_when_libraries_empty_and_have_java_sources() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClasspathForMain classpath = createJavaClasspath();
    classpath.getElements();
    classpath.getElements();
    classpath.logClasspathWarnings();
    classpath.logClasspathWarnings();
    classpath.logClasspathWarnings();
    String warning = "Missing 'sonar.java.binaries' property. You might end up with less precise analysis results.";
    assertThat((logTester.logs(Level.WARN))).containsExactly(warning);
    assertThat(analysisWarningsLogged).containsExactly(warning);
  }

  @Test
  void do_not_register_warning_for_missing_bytecode_when_wrapper_not_injected() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    var javaClasspath = new ClasspathForMain(settings.asConfig(), fs);
    javaClasspath.getElements();
    javaClasspath.logClasspathWarnings();
    String warning = "Missing 'sonar.java.binaries' property. You might end up with less precise analysis results.";
    assertThat((logTester.logs(Level.WARN))).containsExactly(warning);
    assertThat(analysisWarningsLogged).isEmpty();
  }

  @Test
  void setting_binary_prop_should_fill_elements() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_BINARIES, "bin");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.classpath().getBinaryDirs()).hasSize(1);
    assertThat(actual.elements()).containsExactly("<PROJECT>/bin");
    assertThat(actual.classpath().getElements().get(0)).exists();
  }

  @Test
  void setting_binary_dir_prop_should_fill_elements() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_BINARIES, "bin/");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.classpath().getBinaryDirs()).hasSize(1);
    assertThat(actual.elements()).containsExactly("<PROJECT>/bin");
    assertThat(actual.classpath().getElements().get(0)).exists();
  }

  @Test
  void setting_library_prop_should_fill_elements() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/hello.jar");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.classpath().getBinaryDirs()).isEmpty();
    assertThat(actual.elements()).containsExactly("<PROJECT>/lib/hello.jar");
    assertThat(actual.classpath().getElements().get(0)).exists();
  }

  @Test
  void absolute_file_name_should_be_resolved() {
    var absolutePath = new File("src/test/files/classpath/lib/hello.jar").getAbsolutePath();
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, absolutePath);
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.classpath().getBinaryDirs()).isEmpty();
    assertThat(actual.elements()).containsExactly("<PROJECT>/lib/hello.jar");
    assertThat(actual.classpath().getElements().get(0)).exists();
  }

  @Test
  void absolute_aar_file_name_should_be_resolved() {
    var absolutePath = new File("src/test/files/classpath/lib/oklog-1.0.1.aar").getAbsolutePath();
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, absolutePath);
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.classpath().getBinaryDirs()).isEmpty();
    assertThat(actual.elements()).containsExactly("<PROJECT>/lib/oklog-1.0.1.aar");
    assertThat(actual.classpath().getElements().get(0)).exists();
  }

  @Test
  void directory_specified_for_library_should_find_jars() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactlyInAnyOrder(
      "<PROJECT>/lib/world.jar",
      "<PROJECT>/lib/target/classes/foo.jar",
      "<PROJECT>/lib/hello.jar",
      "<PROJECT>/lib"
    );
    assertThat(actual.classpath().getElements().get(0)).exists();
    assertThat(actual.classpath().getElements().get(1)).exists();
    assertThat(actual.classpath().getElements().get(2)).exists();
    assertThat(actual.classpath().getElements().get(3)).exists();
  }

  @Test
  void libraries_should_accept_path_ending_with_wildcard() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/*");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactlyInAnyOrder(
      "<PROJECT>/lib/target", "<PROJECT>/lib/world.jar", "<PROJECT>/lib/hello.jar"
    );
    assertThat(actual.classpath().getElements().get(0)).exists();
    assertThat(actual.classpath().getElements().get(1)).exists();
    assertThat(actual.classpath().getElements().get(2)).exists();
  }

  @Test
  void libraries_should_keep_order() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/world.jar,lib/hello.jar,lib/target/classes/*");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly(
      "<PROJECT>/lib/world.jar",
      "<PROJECT>/lib/hello.jar",
      "<PROJECT>/lib/target/classes/foo.jar"
    );
    assertThat(actual.classpath().getElements().get(0)).exists();
    assertThat(actual.classpath().getElements().get(1)).exists();
    assertThat(actual.classpath().getElements().get(2)).exists();
  }

  @Test
  void libraries_should_accept_relative_paths() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "../../files/classpath/lib/*.jar");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactlyInAnyOrder(
      "<PROJECT>/lib/world.jar",
      "<PROJECT>/lib/hello.jar"
    );
    assertThat(actual.classpath().getElements().get(0)).exists();
    assertThat(actual.classpath().getElements().get(1)).exists();
  }

  @Test
  void libraries_should_accept_relative_paths_with_wildcard() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "../../files/**/lib");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactlyInAnyOrder(
      "<PROJECT>/lib/world.jar",
      "<PROJECT>/lib/hello.jar",
      "<PROJECT>/lib/oklog-1.0.1.aar",
      "src/test/files/other/lib/likeJdkJar.jar",
      "src/test/files/other/lib/emptyArchive.jar",
      "src/test/files/other/lib/hello.jar",
      "src/test/files/other/lib/emptyFile.jar",
      "<PROJECT>/lib",
      "src/test/files/other/lib"
    );
  }

  @Test
  void should_not_scan_target_classes() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "../../files/classpath/lib/target/classes");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly("<PROJECT>/lib/target/classes");
  }


  @Test
  void libraries_should_accept_path_ending_with_wildcard_jar() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/h*.jar");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly("<PROJECT>/lib/hello.jar");
  }

  @Test
  void directory_wildcard_should_be_resolved() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "**/*.jar");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactlyInAnyOrder(
      "<PROJECT>/android/android.jar",
      "<PROJECT>/lib/world.jar",
      "<PROJECT>/lib/target/classes/foo.jar",
      "<PROJECT>/lib/hello.jar"
    );
  }

  @Test
  void wildcard_directory_should_resolve_libs_in_that_dir() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/**/*.jar");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactlyInAnyOrder(
      "<PROJECT>/lib/target/classes/foo.jar",
      "<PROJECT>/lib/world.jar",
      "<PROJECT>/lib/hello.jar"
    );
  }

  @Test
  void windows_path_separator_should_be_supported() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "**\\*.jar");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactlyInAnyOrder(
      "<PROJECT>/android/android.jar",
      "<PROJECT>/lib/world.jar",
      "<PROJECT>/lib/target/classes/foo.jar",
      "<PROJECT>/lib/hello.jar"
    );
  }

  @Test
  void non_existing_libraries_should_log_a_warning() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "toto/**/hello.jar");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    // only one file, no 'sonar.java.binaries' warning
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).isEmpty();
    String warning = "Invalid value for 'sonar.java.libraries', no files nor directories matching 'toto/**/hello.jar'.";
    assertThat(actual.projectWarnings()).containsExactly(warning);
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: []",
      "DEBUG Property 'sonar.java.libraries' resolved with: []",
      "WARN " + warning
    );
  }

  @Test
  void deprecated_properties_are_not_supported_anymore() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES);
    settings.setProperty("sonar.binaries", "bin");
    settings.setProperty("sonar.libraries", "hello.jar");
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).isEmpty();
    String warning = "Missing 'sonar.java.binaries' and 'sonar.java.libraries' properties. You might end up with less precise analysis results.";
    assertThat(actual.projectWarnings()).containsExactly(warning);
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: []",
      "DEBUG Property 'sonar.java.libraries' resolved with: []",
      "WARN " + warning
    );
  }

  @Test
  void parent_module_should_not_validate_sonar_libraries() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "non-existing.jar");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).isEmpty();
    String warning = "Invalid value for 'sonar.java.libraries', no files nor directories matching 'non-existing.jar'.";
    assertThat(actual.projectWarnings()).containsExactly(warning);
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: []",
      "DEBUG Property 'sonar.java.libraries' resolved with: []",
      "WARN " + warning
    );
  }

  private void createTwoFilesInFileSystem() {
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("plop.java"));
    fs.add(TestUtils.emptyInputFile("bar.java"));
  }

  @Test
  void classpath_empty_if_only_test_files() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES);
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("plop.java", InputFile.Type.TEST));
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).isEmpty();
  }

  @Test
  void validate_libraries_only_if_not_filtered_out() {
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES);
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, new File("src/test/files/classpath/lib/lib.so").getAbsolutePath());
    fs = new DefaultFileSystem(new File("src/test/files/classpath/"));
    fs.add(TestUtils.emptyInputFile("plop.java"));
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).isEmpty();
  }

  @Test
  void invalid_sonar_java_binaries_should_fail_log_a_warning() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_BINARIES, "dummyDir");
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly("<PROJECT>/empty");
    String warning = "Invalid value for 'sonar.java.binaries', no files nor directories matching 'dummyDir'.";
    assertThat(actual.projectWarnings()).containsExactly(warning);
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: []",
      "DEBUG Property 'sonar.java.libraries' resolved with: [<PROJECT>/empty]",
      "WARN " + warning
    );
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void by_default_no_jdk_is_set(boolean debugEnabled) {
    logTester.setLevel(debugEnabled ? Level.DEBUG : Level.INFO);
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.projectWarnings()).isEmpty();
    if (debugEnabled) {
      assertThat(actual.logs()).containsExactly(
        "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
        "DEBUG Property 'sonar.java.binaries' resolved with: [<PROJECT>/empty]",
        "DEBUG Property 'sonar.java.libraries' resolved with: [<PROJECT>/empty]"
      );
    } else {
      assertThat(actual.logs()).isEmpty();
    }
  }

  @Test
  void sdk_not_found_path_does_not_make_classpath_init_fail() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_JDK_HOME, "src/test/jdk/do-not-exists");
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly("<PROJECT>/empty");
    String warning = "Invalid value '<JDKS>/do-not-exists' for 'sonar.java.jdkHome' property, defaulting to runtime JDK.";
    assertThat(actual.projectWarnings()).containsExactly(warning);
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' set with: <JDKS>/do-not-exists",
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: [<PROJECT>/empty]",
      "DEBUG Property 'sonar.java.libraries' resolved with: [<PROJECT>/empty]",
      "WARN " + warning
    );
  }

  @Test
  void invalid_sdk_path_does_not_make_classpath_init_fail() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_JDK_HOME, "src/test/jdk/README.txt");
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly("<PROJECT>/empty");
    String warning = "Invalid value '<JDKS>/README.txt' for 'sonar.java.jdkHome' property, defaulting to runtime JDK.";
    assertThat(actual.projectWarnings()).containsExactly(warning);
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' set with: <JDKS>/README.txt",
      "DEBUG Property 'sonar.java.jdkHome' resolved with: []",
      "DEBUG Property 'sonar.java.binaries' resolved with: [<PROJECT>/empty]",
      "DEBUG Property 'sonar.java.libraries' resolved with: [<PROJECT>/empty]",
      "WARN " + warning
    );
  }

  @ParameterizedTest
  @CsvSource(value = {"jdk_classic,rt.jar", "jdk_modular,jrt-fs.jar"})
  void should_include_jdk_in_libraries_when_specified(String jdkFolder, String expectedJar) {
    logTester.setLevel(Level.DEBUG);
    String pathToJdk = "src/test/jdk/" + jdkFolder;
    settings.setProperty(ClasspathProperties.SONAR_JAVA_JDK_HOME, pathToJdk);
    ClassPathResult actual = createAndInitClasspath();
    String libFolder = jdkFolder.equals("jdk_classic") ? "<JDKS>/jdk_classic/jre/lib/" : "<JDKS>/jdk_modular/lib/";
    assertThat(actual.elements()).containsExactly("<PROJECT>/empty", libFolder + expectedJar);
    assertThat(actual.projectWarnings()).isEmpty();
    assertThat(actual.logs()).containsExactly(
      "DEBUG Property 'sonar.java.jdkHome' set with: <JDKS>/" + jdkFolder,
      "DEBUG Property 'sonar.java.jdkHome' resolved with: [" + libFolder + expectedJar + "]",
      "DEBUG Property 'sonar.java.binaries' resolved with: [<PROJECT>/empty]",
      "DEBUG Property 'sonar.java.libraries' resolved with: [" + libFolder + expectedJar + ",<PROJECT>/empty]"
    );
  }

  @Test
  void should_not_be_in_android_context_by_default() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/hello.jar");
    var javaClasspath = createJavaClasspath();
    javaClasspath.init();
    assertThat(javaClasspath.inAndroidContext()).isFalse();
  }

  @Test
  void should_set_in_android_context() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "android/android.jar");
    var javaClasspath = createJavaClasspath();
    javaClasspath.init();
    assertThat(javaClasspath.inAndroidContext()).isTrue();
  }

  @Test
  void should_set_in_android_context_indirect() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "android/*.jar");
    var javaClasspath = createJavaClasspath();
    javaClasspath.init();
    assertThat(javaClasspath.inAndroidContext()).isTrue();
  }

  @Test
  void libraries_should_accept_paths_with_comma_csv_escaped() {
    settings.setProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES, "lib/hello.jar,\"../classpath_with_comma/hello,world.jar\"");
    settings.removeProperty(ClasspathProperties.SONAR_JAVA_BINARIES);
    createTwoFilesInFileSystem();
    ClassPathResult actual = createAndInitClasspath();
    assertThat(actual.elements()).containsExactly(
      "<PROJECT>/lib/hello.jar",
      "src/test/files/classpath_with_comma/hello,world.jar"
    );
  }

  private ClasspathForMain createJavaClasspath() {
    return new ClasspathForMain(settings.asConfig(), fs, analysisWarnings);
  }

  private ClassPathResult createAndInitClasspath() {
    ClasspathForMain classpath = createJavaClasspath();
    List<String> elements = classpath.getElements().stream().map(File::getPath).map(this::replaceProjectPath).toList();
    classpath.logClasspathWarnings();
    List<String> logs = new ArrayList<>();
    for(var level : List.of(Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)) {
      logTester.logs(level).forEach(log -> logs.add(level + " " + replaceProjectPath(log)));
    }
    return new ClassPathResult(classpath,
      elements,
      logs,
      replaceProjectPath(analysisWarningsLogged)
    );
  }

  record ClassPathResult(
    ClasspathForMain classpath,
    List<String> elements,
    List<String> logs,
    List<String> projectWarnings
  ) {
  }

  private String replaceProjectPath(String text) {
    return normalizePath(text)
      .replace(normalizePath(baseDir) + "/", "<PROJECT>/")
      .replace(normalizePath(RELATIVE_BASE_PATH) + "/", "<PROJECT>/")
      .replace(normalizePath(jdkPath) + "/", "<JDKS>/")
      .replace(normalizePath(RELATIVE_JDK_PATH) + "/", "<JDKS>/");

  }

  private static String normalizePath(Path path) {
    return normalizePath(path.toString());
  }

  private static String normalizePath(String path) {
    return path.replace('\\', '/');
  }

  private List<String> replaceProjectPath(List<String> list) {
    return list.stream().map(this::replaceProjectPath).toList();
  }

}
