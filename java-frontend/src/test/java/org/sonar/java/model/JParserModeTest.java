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
package org.sonar.java.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.SystemReleaseClasspath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.java.testing.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;

class JParserModeTest {
  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.INFO);

  @TempDir
  Path temp;

  @Test
  void a_debug_message_is_logged_when_shouldIgnoreUnnamedModuleForSplitPackage_is_set() {
    JParser.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList());
    assertThat(logTester.logs()).isEmpty();
    JParser.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), false);
    assertThat(logTester.logs()).isEmpty();
    JParser.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), true);
    assertThat(logTester.logs()).containsExactly("The Java analyzer will ignore the unnamed module for split packages.");
  }

  @Test
  void runtime_image_of_a_modular_jdk_is_replaced_by_the_java_home_of_that_jdk() throws IOException {
    Path jdkHome = Files.createDirectories(temp.resolve("jdk21"));
    File runtimeImage = runtimeImageOf(jdkHome);
    File library = new File("lib.jar");
    Map<String, String> options = new HashMap<>();

    List<File> environment = config(List.of(library, runtimeImage)).removeJdkLibsAndProvideJdtWithJavaHome(options);

    assertThat(environment).containsExactly(library);
    assertThat(options).containsExactly(Map.entry(SystemReleaseClasspath.OPTION_JAVA_HOME, jdkHome.toString()));
  }

  @Test
  void the_other_entries_of_the_classpath_are_kept_in_order() throws IOException {
    File runtimeImage = runtimeImageOf(Files.createDirectories(temp.resolve("jdk21")));
    List<File> classpath = List.of(new File("a.jar"), runtimeImage, new File("b.jar"), new File("target/classes"));

    List<File> environment = config(classpath).removeJdkLibsAndProvideJdtWithJavaHome(new HashMap<>());

    assertThat(environment).containsExactly(new File("a.jar"), new File("b.jar"), new File("target/classes"));
  }

  @Test
  void only_the_first_runtime_image_is_replaced() throws IOException {
    File first = runtimeImageOf(Files.createDirectories(temp.resolve("jdk17")));
    File second = runtimeImageOf(Files.createDirectories(temp.resolve("jdk21")));
    Map<String, String> options = new HashMap<>();

    List<File> environment = config(List.of(first, second)).removeJdkLibsAndProvideJdtWithJavaHome(options);

    assertThat(environment).containsExactly(second);
    assertThat(options).containsEntry(SystemReleaseClasspath.OPTION_JAVA_HOME, temp.resolve("jdk17").toString());
  }

  @Test
  void jar_named_as_runtime_image_outside_of_a_lib_folder_is_not_considered_as_a_jdk() throws IOException {
    Path home = Files.createDirectories(temp.resolve("not-a-jdk"));
    Files.createFile(home.resolve("jrt-fs.jar"));

    assertKept(List.of(home.resolve("jrt-fs.jar").toFile()));
  }

  @Test
  void classpath_without_runtime_image_is_not_modified() {
    assertKept(List.of(new File("a.jar"), new File("b.jar")));
  }

  @Test
  void rt_jar_is_not_considered_as_a_runtime_image() {
    assertKept(List.of(new File("jdk8/jre/lib/rt.jar")));
  }

  @Test
  void android_jar_is_not_considered_as_a_runtime_image() {
    assertKept(List.of(new File("sdk/platforms/android-30/android.jar")));
  }

  @Test
  void empty_classpath_is_not_modified() {
    assertKept(List.of());
  }

  private static void assertKept(List<File> classpath) {
    Map<String, String> options = new HashMap<>();

    List<File> environment = config(classpath).removeJdkLibsAndProvideJdtWithJavaHome(options);

    assertThat(environment).isSameAs(classpath);
    assertThat(options).isEmpty();
  }

  private static JParserConfig config(List<File> classpath) {
    return JParserConfig.Mode.FILE_BY_FILE.create(new JavaVersionImpl(11), classpath, false, true);
  }

  private static File runtimeImageOf(Path jdkHome) throws IOException {
    Path image = jdkHome.resolve("lib/jrt-fs.jar");
    Files.createDirectories(image.getParent());
    Files.createFile(image);
    return image.toFile();
  }
}
