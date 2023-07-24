/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
  private TestUtils() {
    // utility class, forbidden constructor
  }

  private static final String PROJECT_LOCATION = "../java-checks-test-sources/";
  private static final String MAIN_CODE_SOURCES_DIR = PROJECT_LOCATION + "src/main/java/";
  private static final String TEST_CODE_SOURCES_DIR = PROJECT_LOCATION + "src/test/java/";
  private static final String NON_COMPILING_TEST_SOURCES_DIR = PROJECT_LOCATION + "src/main/files/non-compiling/";

  /**
   * To be used when testing rules targeting MAIN code.
   */
  public static String mainCodeSourcesPath(String path) {
    return getFileFrom(path, MAIN_CODE_SOURCES_DIR);
  }

  /**
   * To be used when testing rules targeting TEST code.
   */
  public static String testCodeSourcesPath(String path) {
    return getFileFrom(path, TEST_CODE_SOURCES_DIR);
  }

  /**
   * To be used when testing rules behavior when bytecode is missing, partial, or code does not compile.
   */
  public static String nonCompilingTestSourcesPath(String path) {
    return getFileFrom(path, NON_COMPILING_TEST_SOURCES_DIR);
  }

  private static String getFileFrom(String path, String relocated) {
    File file = new File((relocated + path).replace('/', File.separatorChar));
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
