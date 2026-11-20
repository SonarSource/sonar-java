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

/**
 * The directory a SemSitter server is started on.
 */
final class ProjectRoot {

  private ProjectRoot() {
  }

  /**
   * @return the deepest directory that contains every file, or the file system root of the first file when they share none
   */
  static Path commonAncestor(Collection<Path> files) {
    Path common = null;
    for (Path file : files) {
      Path directory = file.toAbsolutePath().normalize().getParent();
      common = common == null ? directory : shared(common, directory);
    }
    if (common == null) {
      throw new IllegalArgumentException("No file to find a project root for");
    }
    return common;
  }

  private static Path shared(Path first, Path second) {
    Path candidate = first;
    while (candidate != null && !second.startsWith(candidate)) {
      candidate = candidate.getParent();
    }
    return candidate == null ? first.getRoot() : candidate;
  }

  /**
   * @return the path of {@code file} below {@code root}, written with {@code /} on every platform, as the server names files
   */
  static String relative(Path root, Path file) {
    return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
  }
}
