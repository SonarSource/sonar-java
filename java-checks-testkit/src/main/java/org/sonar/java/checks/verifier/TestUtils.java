/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.verifier;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.checks.verifier.internal.InternalInputFile;
import org.sonar.java.test.classpath.TestClasspathUtils.Module;

import static org.sonar.java.test.classpath.TestClasspathUtils.DEFAULT_MODULE;
import static org.sonar.java.test.classpath.TestClasspathUtils.fixSeparator;

public class TestUtils {

  private TestUtils() {
    // utility class, forbidden constructor
  }

  /**
   * To be used when testing rules targeting MAIN code.
   */
  public static String mainCodeSourcesPath(String path) {
    return mainCodeSourcesPathInModule(DEFAULT_MODULE, path);
  }

  private static String sourcePathInModule(Module module, String sourceDir, String relativePath) {
    Path resolvedPath = Path.of(module.getPath()).resolve(Path.of(fixSeparator(sourceDir), fixSeparator(relativePath)));
    if (!Files.exists(resolvedPath)) {
      throw new IllegalStateException("Path '" + resolvedPath + "' should exist.");
    }
    return resolvedPath.toString();
  }

  /**
   * To be used when testing rules targeting MAIN code from a non-default module.
   */
  public static String mainCodeSourcesPathInModule(Module module, String path) {
    return sourcePathInModule(module, "src/main/java", path);
  }

  /**
   * To be used when testing rules targeting TEST code.
   */
  public static String testCodeSourcesPath(String path) {
    return testCodeSourcesPathInModule(DEFAULT_MODULE, path);
  }

  /**
   * To be used when testing rules targeting TEST code from a non-default module.
   */
  public static String testCodeSourcesPathInModule(Module module, String path) {
    return sourcePathInModule(module, "src/test/java", path);
  }

  /**
   * To be used when testing rules behavior when bytecode is missing, partial, or code does not compile.
   */
  public static String nonCompilingTestSourcesPath(String path) {
    return nonCompilingTestSourcesPathInModule(DEFAULT_MODULE, path);
  }

  /**
   * To be used when testing rules behavior when bytecode is missing, partial, or code does not compile.
   * And the file is in a non-default module.
   *
   */
  public static String nonCompilingTestSourcesPathInModule(Module module, String path) {
    return sourcePathInModule(module, "src/main/files/non-compiling", path);
  }

  public static InputFile emptyInputFile(String filename) {
    return InternalInputFile.emptyInputFile(filename, InputFile.Type.MAIN);
  }

  public static InputFile emptyInputFile(String filename, InputFile.Type type) {
    return InternalInputFile.emptyInputFile(filename, type);
  }

  public static InputFile inputFile(String filepath) {
    return InternalInputFile.inputFile("", new File(filepath));
  }

  public static InputFile inputFile(File file) {
    return InternalInputFile.inputFile("", file);
  }

  public static InputFile inputFile(String moduleKey, File file) {
    return InternalInputFile.inputFile(moduleKey, file);
  }
}
