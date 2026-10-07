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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

public record SourcePath(List<File> roots, List<String> encodings) {

  public static final SourcePath EMPTY = new SourcePath(List.of(), List.of());

  public SourcePath {
    roots = List.copyOf(roots);
    encodings = List.copyOf(encodings);
    if (roots.size() != encodings.size()) {
      throw new IllegalArgumentException("Each source root must have an encoding.");
    }
  }

  public static SourcePath resolve(Configuration settings, FileSystem fs, String property) {
    String[] paths = settings.getStringArray(property);
    if (paths.length == 0) {
      return EMPTY;
    }
    Set<File> roots = new LinkedHashSet<>();
    for (String path : paths) {
      File root = fs.baseDir().toPath().resolve(path).toAbsolutePath().normalize().toFile();
      if (!root.isDirectory()) {
        throw new IllegalArgumentException("Invalid value for '" + property + "': source root is not a directory: " + root);
      }
      roots.add(root);
    }
    String encoding = Objects.requireNonNullElse(fs.encoding(), StandardCharsets.UTF_8).name();
    return new SourcePath(List.copyOf(roots), roots.stream().map(root -> encoding).toList());
  }

  public static SourcePath combine(SourcePath main, SourcePath test) {
    var roots = new LinkedHashMap<File, String>();
    for (SourcePath sourcePath : List.of(main, test)) {
      for (int i = 0; i < sourcePath.roots.size(); i++) {
        roots.putIfAbsent(sourcePath.roots.get(i), sourcePath.encodings.get(i));
      }
    }
    return new SourcePath(List.copyOf(roots.keySet()), List.copyOf(roots.values()));
  }
}
