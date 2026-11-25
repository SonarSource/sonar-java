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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectRootTest {

  private static final Path BASE = Path.of("project").toAbsolutePath();

  @Test
  void single_file_root_is_its_directory() {
    assertThat(ProjectRoot.commonAncestor(List.of(BASE.resolve("src/p/A.java")))).contains(BASE.resolve("src/p"));
  }

  @Test
  void root_is_the_deepest_shared_directory() {
    List<Path> files = List.of(BASE.resolve("src/p/A.java"), BASE.resolve("src/p/q/B.java"), BASE.resolve("src/r/C.java"));

    assertThat(ProjectRoot.commonAncestor(files)).contains(BASE.resolve("src"));
  }

  @Test
  void dots_in_paths_are_resolved() {
    List<Path> files = List.of(BASE.resolve("src/p/../p/A.java"), BASE.resolve("src/./p/B.java"));

    assertThat(ProjectRoot.commonAncestor(files)).contains(BASE.resolve("src/p"));
  }

  @Test
  void relative_paths_are_made_absolute() {
    assertThat(ProjectRoot.commonAncestor(List.of(Path.of("src/A.java")))).contains(Path.of("src").toAbsolutePath());
  }

  @Test
  void no_file_has_no_root() {
    assertThat(ProjectRoot.commonAncestor(List.of())).isEmpty();
  }

  @Test
  void files_sharing_no_directory_have_no_root(@TempDir Path temp) throws IOException {
    try (FileSystem otherDrive = FileSystems.newFileSystem(temp.resolve("drive.zip"), Map.of("create", "true"))) {
      List<Path> files = List.of(BASE.resolve("src/A.java"), otherDrive.getPath("/src/B.java"));

      assertThat(ProjectRoot.commonAncestor(files)).isEmpty();
    }
  }

  @Test
  void relative_path_is_written_with_slashes_below_the_root() {
    assertThat(ProjectRoot.relative(BASE, BASE.resolve("src").resolve("p").resolve("A.java"))).isEqualTo("src/p/A.java");
  }
}
