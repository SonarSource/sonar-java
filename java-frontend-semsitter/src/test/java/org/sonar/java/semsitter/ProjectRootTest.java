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

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectRootTest {

  private static final Path BASE = Path.of("project").toAbsolutePath();

  @Test
  void single_file_root_is_its_directory() {
    assertThat(ProjectRoot.commonAncestor(List.of(BASE.resolve("src/p/A.java")))).isEqualTo(BASE.resolve("src/p"));
  }

  @Test
  void root_is_the_deepest_shared_directory() {
    List<Path> files = List.of(BASE.resolve("src/p/A.java"), BASE.resolve("src/p/q/B.java"), BASE.resolve("src/r/C.java"));

    assertThat(ProjectRoot.commonAncestor(files)).isEqualTo(BASE.resolve("src"));
  }

  @Test
  void dots_in_paths_are_resolved() {
    List<Path> files = List.of(BASE.resolve("src/p/../p/A.java"), BASE.resolve("src/./p/B.java"));

    assertThat(ProjectRoot.commonAncestor(files)).isEqualTo(BASE.resolve("src/p"));
  }

  @Test
  void relative_paths_are_made_absolute() {
    assertThat(ProjectRoot.commonAncestor(List.of(Path.of("src/A.java")))).isEqualTo(Path.of("src").toAbsolutePath());
  }

  @Test
  void no_file_has_no_root() {
    assertThatThrownBy(() -> ProjectRoot.commonAncestor(List.of())).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void relative_path_is_written_with_slashes_below_the_root() {
    assertThat(ProjectRoot.relative(BASE, BASE.resolve("src").resolve("p").resolve("A.java"))).isEqualTo("src/p/A.java");
  }
}
