/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;

public final class CheckTestUtils {
  
  private static final String TEST_SOURCES_DIR = "../java-checks-test-sources/src/main/java/";

  private CheckTestUtils() {
    // Utility class
  }

  public static String testSourcesPath(String path) {
    File file = new File((TEST_SOURCES_DIR + path).replace('/', File.separatorChar));
    assertTrue("Path '" + path + "' should exist.", file.exists());
    try {
      return file.getCanonicalPath();
    } catch (IOException e) {
      throw new IllegalStateException("Invalid canonical path for '" + path + "'.", e);
    }
  }

  public static DefaultInputFile inputFile(String filename) {
    File file = new File(filename);
    try {
      return new TestInputFileBuilder("", file.getPath())
        .setContents(new String(Files.readAllBytes(file.toPath()), UTF_8))
        .setCharset(UTF_8)
        .setLanguage("java")
        .build();
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Unable to lead file '%s", file.getAbsolutePath()));
    }
  }
}
