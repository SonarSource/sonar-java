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
import static org.sonar.java.model.JParserConfig.shouldEnablePreviewFlag;

class JParserConfigTest {
  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.INFO);

  @TempDir
  Path temp;

  @Test
  void should_enable_preview() {
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl())).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(8))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(11))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(16))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(17))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(18))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(19))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(20))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(42))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(42, true))).isTrue();

    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromString("1.8"))).isFalse();
    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromString("1.8", "True"))).isTrue();
  }

  @Test
  void a_debug_message_is_logged_when_shouldIgnoreUnnamedModuleForSplitPackage_is_set() {
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList());
    assertThat(logTester.logs()).isEmpty();
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), false);
    assertThat(logTester.logs()).isEmpty();
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), true);
    assertThat(logTester.logs()).containsExactly("The Java analyzer will ignore the unnamed module for split packages.");
  }

  @Test
  void runtime_image_of_a_jdk_is_replaced_by_its_release_api_when_the_release_is_older() throws IOException {
    File runtimeImage = runtimeImageOf(fakeJdk("jdk21", "21.0.2", true));
    File library = new File("lib.jar");
    Map<String, String> options = releaseOptions("11");

    List<File> environment = config("11", List.of(library, runtimeImage)).useReleaseApiOfConfiguredJdk(options);

    assertThat(environment).containsExactly(library);
    assertThat(options).containsEntry(SystemReleaseClasspath.OPTION_JAVA_HOME, runtimeImage.getParentFile().getParent());
  }

  @Test
  void the_other_entries_of_the_classpath_are_kept_in_order() throws IOException {
    File runtimeImage = runtimeImageOf(fakeJdk("jdk21", "21.0.2", true));
    List<File> classpath = List.of(new File("a.jar"), runtimeImage, new File("b.jar"), new File("target/classes"));

    List<File> environment = config("11", classpath).useReleaseApiOfConfiguredJdk(releaseOptions("11"));

    assertThat(environment).containsExactly(new File("a.jar"), new File("b.jar"), new File("target/classes"));
  }

  @Test
  void runtime_image_is_kept_when_the_release_is_not_older_than_the_jdk() throws IOException {
    File runtimeImage = runtimeImageOf(fakeJdk("jdk21", "21.0.2", true));
    List<File> classpath = List.of(runtimeImage);

    assertKept("21", classpath);
    assertKept("22", classpath);
    assertKept("26", classpath);
  }

  @Test
  void runtime_image_is_kept_when_the_release_is_below_9() throws IOException {
    List<File> classpath = List.of(runtimeImageOf(fakeJdk("jdk21", "21.0.2", true)));

    assertKept("8", classpath);
    assertKept("7", classpath);
  }

  @Test
  void runtime_image_is_kept_when_the_jdk_cannot_serve_the_release() throws IOException {
    assertKept("11", List.of(runtimeImageOf(fakeJdk("without-ct-sym", "21.0.2", false))));

    Path withoutReleaseFile = Files.createDirectories(temp.resolve("without-release-file/lib"));
    Files.createFile(withoutReleaseFile.resolve("ct.sym"));
    assertKept("11", List.of(runtimeImageOf(withoutReleaseFile.getParent())));
  }

  @Test
  void a_debug_message_explains_why_the_release_api_is_not_used() throws IOException {
    logTester.setLevel(Level.DEBUG);
    File runtimeImage = runtimeImageOf(fakeJdk("jdk21", "21.0.2", true));

    config("21", List.of(runtimeImage)).useReleaseApiOfConfiguredJdk(releaseOptions("21"));

    assertThat(logTester.logs(Level.DEBUG)).hasSize(1);
    assertThat(logTester.logs(Level.DEBUG).get(0)).contains("Java 21").contains(runtimeImage.getParentFile().getParent());
  }

  @Test
  void runtime_image_is_kept_when_a_platform_jar_is_on_the_classpath() throws IOException {
    File runtimeImage = runtimeImageOf(fakeJdk("jdk21", "21.0.2", true));

    assertKept("11", List.of(new File("jdk8/jre/lib/rt.jar"), runtimeImage));
    assertKept("11", List.of(runtimeImage, new File("android-sdk/platforms/android-34/android.jar")));
  }

  @Test
  void rt_jar_alone_is_not_modified() {
    assertKept("11", List.of(new File("jdk8/jre/lib/rt.jar")));
  }

  @Test
  void jar_named_as_runtime_image_outside_of_a_lib_folder_is_not_considered_as_a_jdk() throws IOException {
    Path home = Files.createDirectories(temp.resolve("not-a-jdk"));
    Files.createFile(home.resolve("jrt-fs.jar"));

    assertKept("11", List.of(home.resolve("jrt-fs.jar").toFile()));
  }

  @Test
  void classpath_without_jdk_is_not_modified() {
    Map<String, String> options = releaseOptions("11");
    List<File> classpath = List.of(new File("a.jar"), new File("b.jar"));

    assertThat(config("11", classpath).useReleaseApiOfConfiguredJdk(options)).isSameAs(classpath);
    assertThat(options).doesNotContainKey(SystemReleaseClasspath.OPTION_JAVA_HOME);
  }

  @Test
  void empty_classpath_is_not_modified() {
    assertKept("11", List.of());
  }

  private void assertKept(String release, List<File> classpath) {
    Map<String, String> options = releaseOptions(release);

    List<File> environment = config(release, classpath).useReleaseApiOfConfiguredJdk(options);

    assertThat(environment).isSameAs(classpath);
    assertThat(options).doesNotContainKey(SystemReleaseClasspath.OPTION_JAVA_HOME);
  }

  private static JParserConfig config(String release, List<File> classpath) {
    return JParserConfig.Mode.FILE_BY_FILE.create(JavaVersionImpl.fromString(release), classpath);
  }

  private static Map<String, String> releaseOptions(String release) {
    Map<String, String> options = new HashMap<>();
    options.put(SystemReleaseClasspath.OPTION_RELEASE, release);
    return options;
  }

  private static File runtimeImageOf(Path jdkHome) throws IOException {
    Path image = jdkHome.resolve("lib/jrt-fs.jar");
    Files.createDirectories(image.getParent());
    if (!Files.exists(image)) {
      Files.createFile(image);
    }
    return image.toFile();
  }

  private Path fakeJdk(String name, String version, boolean withCtSym) throws IOException {
    Path home = Files.createDirectories(temp.resolve(name));
    Path lib = Files.createDirectories(home.resolve("lib"));
    Files.writeString(home.resolve("release"), "JAVA_VERSION=\"" + version + "\"\n");
    if (withCtSym) {
      Files.createFile(lib.resolve("ct.sym"));
    }
    return home;
  }
}
