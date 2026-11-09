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
package org.sonar.java.psi;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Effective frontend inputs. A null runtime selects the running VM only when no
 * rt.jar, android.jar or jrt-fs.jar entry suppresses the implicit boot classpath.
 * Source strings and charsets belong to the caller's decoded snapshots.
 */
public record JavaPsiConfiguration(Path runtime, List<Path> classpath, int sourceVersion, boolean previewEnabled, boolean ignoreUnnamedModuleForSplitPackage) {

  public JavaPsiConfiguration {
    runtime = (runtime == null ? Path.of(System.getProperty("java.home")) : runtime).toAbsolutePath().normalize();
    classpath = classpath.stream().map(path -> Objects.requireNonNull(path).toAbsolutePath().normalize()).toList();
  }

  public List<Path> runtimeArchives() {
    return classpath.stream().filter(path -> Set.of("rt.jar", "android.jar", "jrt-fs.jar").contains(path.getFileName().toString())).toList();
  }
}
