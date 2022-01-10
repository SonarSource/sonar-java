/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.verifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.sonar.java.annotations.VisibleForTesting;

public final class FilesUtils {

  private FilesUtils() {
  }

  /**
   * Default location of the jars/zips to be taken into account when performing the analysis.
   */
  public static final String DEFAULT_TEST_JARS_DIRECTORY = "../java-checks-test-sources/target/test-jars";
  public static final String DEFAULT_TEST_CLASSES_DIRECTORY = "../java-checks-test-sources/target/classes";

  public static List<File> getClassPath(String jarsDirectory) {
    List<File> classpath = new LinkedList<>();
    Path testJars = Paths.get(jarsDirectory);
    if (testJars.toFile().exists()) {
      classpath = getFilesRecursively(testJars, "jar", "zip");
    } else if (!DEFAULT_TEST_JARS_DIRECTORY.equals(jarsDirectory)) {
      throw new AssertionError("The directory to be used to extend class path does not exists (" + testJars.toAbsolutePath() + ").");
    }
    return classpath;
  }

  @VisibleForTesting
  public static List<File> getFilesRecursively(Path root, String... extensions) {
    final List<File> files = new ArrayList<>();

    FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
        for (String extension : extensions) {
          if (filePath.toString().endsWith("." + extension)) {
            files.add(filePath.toFile());
            break;
          }
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
      }
    };

    try {
      Files.walkFileTree(root, visitor);
    } catch (IOException e) {
      // we already ignore errors in the visitor
    }

    return files;
  }
}
