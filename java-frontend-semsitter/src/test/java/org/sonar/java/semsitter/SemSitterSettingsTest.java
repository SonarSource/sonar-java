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
package org.sonar.java.semsitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.model.JavaVersionImpl;

import static org.assertj.core.api.Assertions.assertThat;

class SemSitterSettingsTest {

  @TempDir
  Path javaHome;

  @Test
  void libraries_are_the_classpath_when_the_jdk_has_no_runtime_image() {
    List<File> classpath = List.of(new File("a.jar"), new File("b.jar"));

    assertThat(SemSitterSettings.of(new JavaVersionImpl(), classpath, javaHome))
      .containsOnlyKeys(SemSitterSettings.LIBRARIES)
      .containsEntry(SemSitterSettings.LIBRARIES, List.of(new File("a.jar").getAbsolutePath(), new File("b.jar").getAbsolutePath()));
  }

  @Test
  void runtime_image_of_the_jdk_comes_before_the_classpath() throws IOException {
    Path image = Files.createDirectories(javaHome.resolve("lib")).resolve("modules");
    Files.writeString(image, "image");

    assertThat(SemSitterSettings.of(new JavaVersionImpl(), List.of(new File("a.jar")), javaHome).get(SemSitterSettings.LIBRARIES))
      .containsExactly(image.toString(), new File("a.jar").getAbsolutePath());
  }

  @Test
  void language_level_is_given_only_when_the_project_sets_it() {
    assertThat(SemSitterSettings.of(new JavaVersionImpl(), List.of(), javaHome)).doesNotContainKey(SemSitterSettings.JAVA_VERSION);
    assertThat(SemSitterSettings.of(new JavaVersionImpl(17), List.of(), javaHome)).containsEntry(SemSitterSettings.JAVA_VERSION, List.of("17"));
  }

  @Test
  void preview_flag_is_given_only_when_preview_features_are_enabled() {
    assertThat(SemSitterSettings.of(new JavaVersionImpl(25), List.of(), javaHome)).doesNotContainKey(SemSitterSettings.ENABLE_PREVIEW);
    assertThat(SemSitterSettings.of(new JavaVersionImpl(25, true), List.of(), javaHome)).containsEntry(SemSitterSettings.ENABLE_PREVIEW, List.of("true"));
  }
}
