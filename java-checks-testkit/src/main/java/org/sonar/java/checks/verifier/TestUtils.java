/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.checks.verifier.internal.InternalInputFile;

public class TestUtils {

  public static final String DEFAULT_MODULE = "default";
  public static final String JAVA_17_MODULE = "java-17";

  private static final String PROJECT_LOCATION = "../java-checks-test-sources/";

  private TestUtils() {
    // utility class, forbidden constructor
  }

  /**
   * To be used when testing rules targeting MAIN code.
   */
  public static String mainCodeSourcesPath(String path) {
    return mainCodeSourcesPathInModule(DEFAULT_MODULE, path);
  }

  /**
   * To be used when testing rules targeting MAIN code from a non-default module.
   */
  public static String mainCodeSourcesPathInModule(String module, String path) {
    return getFileFrom(path, PROJECT_LOCATION + module + "/src/main/java/");
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
  public static String testCodeSourcesPathInModule(String module, String path) {
    return getFileFrom(path, PROJECT_LOCATION + module + "/src/test/java/");
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
  public static String nonCompilingTestSourcesPathInModule(String module, String path) {
    return getFileFrom(path, PROJECT_LOCATION + module + "/src/main/files/non-compiling/");
  }

  private static String getFileFrom(String path, String relocated) {
    var file = new File((relocated + path).replace('/', File.separatorChar));
    if (!file.exists()) {
      throw new IllegalStateException("Path '" + path + "' should exist.");
    }
    try {
      return file.getCanonicalPath();
    } catch (IOException e) {
      throw new IllegalStateException("Invalid canonical path for '" + path + "'.", e);
    }
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
