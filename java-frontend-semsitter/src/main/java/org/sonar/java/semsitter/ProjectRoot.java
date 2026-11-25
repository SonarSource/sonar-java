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
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * The directory a SemSitter server is started on.
 */
final class ProjectRoot {

  private ProjectRoot() {
  }

  /**
   * @return the deepest directory that contains every file, none when there is no file or the files share no directory,
   * like files on two Windows drives
   */
  static Optional<Path> commonAncestor(Collection<Path> files) {
    Iterator<Path> paths = files.iterator();
    if (!paths.hasNext()) {
      return Optional.empty();
    }
    Path common = directory(paths.next());
    while (common != null && paths.hasNext()) {
      common = shared(common, directory(paths.next()));
    }
    return Optional.ofNullable(common);
  }

  private static Path directory(Path file) {
    return file.toAbsolutePath().normalize().getParent();
  }

  @Nullable
  private static Path shared(Path first, Path second) {
    Path candidate = first;
    while (candidate != null && !second.startsWith(candidate)) {
      candidate = candidate.getParent();
    }
    return candidate;
  }

  /**
   * @return the path of {@code file} below {@code root}, written with {@code /} on every platform, as the server names files
   */
  static String relative(Path root, Path file) {
    return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
  }
}
