/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
  private TestUtils() {
    // utility class, forbidden constructor
  }

  private static final String TEST_SOURCES_DIR = "../java-checks-test-sources/src/main/java/";
  private static final String NON_COMPILING_TEST_SOURCES_DIR = "../java-checks-test-sources/src/main/files/non-compiling/";

  public static String testSourcesPath(String path) {
    return getFileFrom(path, TEST_SOURCES_DIR);
  }

  public static String nonCompilingTestSourcesPath(String path) {
    return getFileFrom(path, NON_COMPILING_TEST_SOURCES_DIR);
  }

  private static String getFileFrom(String path, String relocated) {
    File file = new File((relocated + path).replace('/', File.separatorChar));
    assertTrue(file.exists(), "Path '" + path + "' should exist.");
    try {
      return file.getCanonicalPath();
    } catch (IOException e) {
      throw new IllegalStateException("Invalid canonical path for '" + path + "'.", e);
    }
  }

  public static int[] computeLineEndOffsets(int[] lineStartOffsets, int lastValidOffset) {
    int[] lineEndOffsets = new int[lineStartOffsets.length];
    for (int i = 0; i < lineStartOffsets.length - 1; i++) {
      lineEndOffsets[i] = lineStartOffsets[i + 1] - 1;
    }
    lineEndOffsets[lineEndOffsets.length - 1] = lastValidOffset - 1;
    return lineEndOffsets;
  }

  public static InputFile emptyInputFile(String filename) {
    return emptyInputFile(filename, InputFile.Type.MAIN);
  }

  public static InputFile emptyInputFile(String filename, InputFile.Type type) {
    return new TestInputFileBuilder("", filename)
      .setCharset(UTF_8)
      .setLanguage("java")
      .setType(type)
      .build();
  }

  public static InputFile inputFile(String filepath) {
    return inputFile("", new File(filepath));
  }

  public static InputFile inputFile(File file) {
    return inputFile("", file);
  }

  public static InputFile inputFile(String moduleKey, File file) {
    return inputFile(moduleKey, file, InputFile.Type.MAIN);
  }

  public static InputFile inputFile(String moduleKey, File file, InputFile.Type type) {
    // Try to use the current directory as the module directory if the file is a descendant.
    // Else use the parent directory of the file as fallback.
    // Because TestInputFileBuilder requires the file to be a descendant of the module directory.
    File moduleDir = file.getParentFile();
    if (file.exists()) {
      try {
        File currentDir = new File(".").getCanonicalFile();
        File canonicalFile = file.getCanonicalFile();
        if (canonicalFile.getPath().startsWith(currentDir.getPath() + File.separator)) {
          moduleDir = currentDir;
          file = canonicalFile;
        }
      } catch (IOException e) {
        throw new IllegalStateException("Unable create input file '" + file.getAbsolutePath() + "'", e);
      }
    }
    return inputFile(moduleKey, moduleDir, file, type);
  }

  public static InputFile inputFile(String moduleKey, File moduleBaseDir, File file, InputFile.Type type) {
    try {
      return new TestInputFileBuilder(moduleKey, moduleBaseDir, file)
        .setContents(new String(Files.readAllBytes(file.toPath()), UTF_8))
        .setCharset(UTF_8)
        .setLanguage("java")
        .setType(type)
        .build();
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Unable to read file '%s", file.getAbsoluteFile()));
    }
  }

  private static List<String> filterOutAnalysisProgressLogLines(Stream<String> logs) {
    return logs.filter(log -> !log.matches("[0-9]+% analyzed")).toList();
  }

  public static List<String> filterOutAnalysisProgressLogLines(List<String> logs) {
    return filterOutAnalysisProgressLogLines(logs.stream());
  }

  /**
   * Creates a Mockito test double for {@link SonarComponents}, pre-configured to avoid
   * nulls in unit tests and remove the need for null checks in production code.
   */
  public static SonarComponents mockSonarComponents() {
    SonarComponents mock = mock(SonarComponents.class);
    when(mock.isSonarLintContext()).thenReturn(true);
    when(mock.getBatchModeSizeInKB()).thenReturn(-1L);
    when(mock.getJavaClasspath()).thenReturn(List.of());
    when(mock.getJavaTestClasspath()).thenReturn(List.of());
    when(mock.getJspClasspath()).thenReturn(List.of());
    when(mock.testChecks()).thenReturn(List.of());
    when(mock.jspChecks()).thenReturn(List.of());
    return mock;
  }
}
