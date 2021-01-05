/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.SystemUtils;
import org.sonar.java.annotations.VisibleForTesting;

/**
 * Adapted from https://github.com/JetBrains/intellij-community/blob/203.5981/jps/model-impl/src/org/jetbrains/jps/model/java/impl/JavaSdkUtil.java
 */
public class JavaSdkUtil {
  private static final String LIB_JRT_FS_JAR = "lib/jrt-fs.jar";
  private static final String ENDORSED = "endorsed";

  private JavaSdkUtil() {
    // utility class
  }

  public static List<File> getJdkClassesRoots(Path home) {
    return getJdkClassesRoots(home, SystemUtils.IS_OS_MAC);
  }

  @VisibleForTesting
  static List<File> getJdkClassesRoots(Path home, boolean isMac) {
    if (isModularRuntime(home)) {
      return Collections.singletonList(home.resolve(LIB_JRT_FS_JAR).toFile());
    }

    List<File> rootFiles = new ArrayList<>();

    rootFiles.addAll(collectJars(home, isMac));

    return rootFiles;
  }

  private static List<File> collectJars(Path home, boolean isMac) {
    List<File> rootFiles = new ArrayList<>();
    Set<Path> duplicatePathFilter = new HashSet<>();
    for (Path jarDir : collectJarDirs(home, isMac)) {
      if (!Files.isDirectory(jarDir)) {
        continue;
      }
      listFiles(jarDir, JavaSdkUtil::isJarFile).stream()
        // filter out alternative implementations
        .filter(JavaSdkUtil::isNotAlternativeImplementation)
        // filter out duplicate (symbolically linked) .jar files commonly found in OS X JDK distributions
        .map(JavaSdkUtil::toRealPath).filter(Optional::isPresent).map(Optional::get)
        // make sure there is no duplicates
        .forEach(jarFile -> {
          if (duplicatePathFilter.add(jarFile)) {
            rootFiles.add(jarFile.toFile());
          }
        });
    }

    return rootFiles;
  }

  private static boolean isJarFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar");
  }

  private static boolean isNotAlternativeImplementation(Path jarPath) {
    String jarFileName = jarPath.getFileName().toString();
    return !jarFileName.equals("alt-rt.jar") && !jarFileName.equals("alt-string.jar");
  }

  private static Path[] collectJarDirs(Path home, boolean isMac) {
    Path[] jarDirs;
    if (isMac) {
      Path openJdkRtJar = home.resolve("jre/lib/rt.jar");
      if (Files.isRegularFile(openJdkRtJar)) {
        Path libDir = home.resolve("lib");
        Path classesDir = openJdkRtJar.getParent();
        Path libExtDir = openJdkRtJar.getParent().resolve("ext");
        Path libEndorsedDir = libDir.resolve(ENDORSED);
        jarDirs = new Path[] {libEndorsedDir, libDir, classesDir, libExtDir};
      } else {
        Path libDir = home.resolve("lib");
        Path classesDir = home.getParent().resolve("Classes");
        Path libExtDir = libDir.resolve("ext");
        Path libEndorsedDir = libDir.resolve(ENDORSED);
        jarDirs = new Path[] {libEndorsedDir, libDir, classesDir, libExtDir};
      }
    } else {
      Path libDir = home.resolve("jre/lib");
      if (!Files.isDirectory(libDir)) {
        libDir = home.resolve("lib");
      }
      Path libExtDir = libDir.resolve("ext");
      Path libEndorsedDir = libDir.resolve(ENDORSED);
      jarDirs = new Path[] {libEndorsedDir, libDir, libExtDir};
    }
    return jarDirs;
  }

  private static boolean isModularRuntime(Path home) {
    return Files.exists(home.resolve(LIB_JRT_FS_JAR));
  }

  private static Set<Path> listFiles(Path dir, Predicate<Path> filter) {
    try (Stream<Path> stream = Files.walk(dir, 1)) {
      return stream.filter(filter).collect(Collectors.toSet());
    } catch (IOException e) {
      return Collections.emptySet();
    }
  }

  private static Optional<Path> toRealPath(Path file) {
    try {
      return Optional.of(file.toRealPath());
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
