/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import java.nio.file.Files;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils {
  private TestUtils() {
    // utility class, forbidden constructor
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
    try {
      return new TestInputFileBuilder(moduleKey, file.getPath())
        .setContents(new String(Files.readAllBytes(file.toPath()), UTF_8))
        .setCharset(UTF_8)
        .setLanguage("java")
        .build();
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Unable to read file '%s", file.getAbsolutePath()));
    }
  }
}
