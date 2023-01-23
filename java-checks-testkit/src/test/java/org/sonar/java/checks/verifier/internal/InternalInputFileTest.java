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
package org.sonar.java.checks.verifier.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalInputFileTest {

  private static final InternalTextPointer TEXT_POINTER = new InternalTextPointer(0, 0);

  @Test
  void emptyInputFile() throws Exception {
    InputFile inputFile = InternalInputFile.emptyInputFile("foo.java", InputFile.Type.TEST);

    assertThat(inputFile.filename()).isEqualTo("foo.java");
    assertThat(inputFile.file()).isNotNull();
    assertThat(inputFile.isFile()).isFalse();
    assertThat(inputFile.key()).isEqualTo(":foo.java");
    assertThat(inputFile.type()).isEqualTo(InputFile.Type.TEST);
    assertThat(inputFile.charset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(inputFile.contents()).isEmpty();
    assertThat(inputFile.isEmpty()).isTrue();
    assertThat(inputFile.language()).isEqualTo("java");
    assertThat(inputFile.lines()).isEqualTo(-1);

    assertThat(inputFile.path()).hasToString("foo.java");
    assertThat(inputFile.relativePath()).isEqualTo("foo.java");
    assertThat(inputFile.absolutePath()).endsWith("foo.java");
    assertThat(inputFile.uri().toString())
      .startsWith("file:")
      .endsWith("foo.java");

    assertThat(inputFile.newPointer(0, 0)).isNotNull();
    assertThat(inputFile.newRange(TEXT_POINTER, TEXT_POINTER)).isNotNull();
    assertThat(inputFile.newRange(0, 0, 0, 0)).isNotNull();

    assertThat(inputFile).hasToString("foo.java");
    assertThatThrownBy(inputFile::inputStream)
      .isInstanceOf(FileNotFoundException.class)
      .hasMessageContaining("foo.java");

    assertMethodNotSupported(() -> inputFile.selectLine(0), "InternalInputFile::selectLine(int)");
    assertThat(inputFile.status()).isEqualTo(InputFile.Status.SAME);
  }

  @Test
  void nonEmptyInputFile() throws Exception {
    InputFile inputFile = InternalInputFile.inputFile("module", new File("src/test/files/internal/bar.java"));

    assertThat(inputFile.filename()).isEqualTo("bar.java");
    assertThat(inputFile.file()).isNotNull();
    assertThat(inputFile.isFile()).isTrue();
    assertThat(inputFile.key()).isEqualTo("module:src/test/files/internal/bar.java");
    assertThat(inputFile.type()).isEqualTo(InputFile.Type.MAIN);
    assertThat(inputFile.charset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(inputFile.contents()).startsWith("class A { }");
    try (InputStream is = inputFile.inputStream()) {
      assertThat(is).isNotNull();
    }
    assertThat(inputFile.isEmpty()).isFalse();
    assertThat(inputFile.language()).isEqualTo("java");
    assertThat(inputFile.lines()).isEqualTo(1);

    assertThat(inputFile.path().toString()).endsWith("bar.java");
    assertThat(inputFile.relativePath()).endsWith("bar.java");
    assertThat(inputFile.absolutePath()).endsWith("bar.java");
    assertThat(inputFile.uri()
      .toString())
        .startsWith("file:")
        .endsWith("bar.java");

    assertThat(inputFile.newPointer(0, 0)).isNotNull();
    assertThat(inputFile.newRange(TEXT_POINTER, TEXT_POINTER)).isNotNull();
    assertThat(inputFile.newRange(0, 0, 0, 0)).isNotNull();

    assertMethodNotSupported(() -> inputFile.selectLine(0), "InternalInputFile::selectLine(int)");
    assertThat(inputFile.status()).isEqualTo(InputFile.Status.SAME);
  }

  @Test
  void wrongInputFile() {
    File wrongFile = new File("src/test/files/internal/missing.java");

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> InternalInputFile.inputFile("module", wrongFile));
    assertThat(e)
      .hasMessageStartingWith("Unable to read file '")
      .hasMessageEndingWith("missing.java'");
  }

  private static void assertMethodNotSupported(Executable executable, String expectedMessage) {
    InternalMockedSonarAPI.NotSupportedException e = assertThrows(InternalMockedSonarAPI.NotSupportedException.class, executable);
    assertThat(e).hasMessage(String.format("Method unsuported by the rule verifier framework: '%s'", expectedMessage));
  }

}
