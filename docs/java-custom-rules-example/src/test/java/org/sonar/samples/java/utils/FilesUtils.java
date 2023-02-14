/*
 * Copyright (C) 2012-2023 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.utils;

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

/**
 * Duplicates org.sonar.java.checks.verifier.FilesUtils to locate test jars within the custom-rules plugin
 */
public class FilesUtils {

  private FilesUtils() {
  }

  /**
   * Default location of the jars/zips to be taken into account when performing the analysis.
   */
  private static final String DEFAULT_TEST_JARS_DIRECTORY = "target/test-jars";

  public static List<File> getClassPath(String jarsDirectory) {
    List<File> classpath = new LinkedList<>();
    Path testJars = Paths.get(jarsDirectory);
    if (testJars.toFile().exists()) {
      classpath = getFilesRecursively(testJars, "jar", "zip");
    } else if (!DEFAULT_TEST_JARS_DIRECTORY.equals(jarsDirectory)) {
      throw new AssertionError("The directory to be used to extend class path does not exists ("
        + testJars.toAbsolutePath()
        + ").");
    }
    classpath.add(new File("target/test-classes"));
    return classpath;
  }

  private static List<File> getFilesRecursively(Path root, String... extensions) {
    final List<File> files = new ArrayList<>();

    FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
        for (String extension : extensions) {
          if (filePath.toString().endsWith("."
            + extension)) {
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
