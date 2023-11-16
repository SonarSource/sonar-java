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
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonar.api.batch.fs.InputFile.Type.MAIN;
import static org.sonar.api.batch.fs.InputFile.Type.TEST;

class TestUtilsTest {

  private static final String DUMMY_FILE = "src/test/files/dummy-module/dummy.java";

  @Test
  void checkTestResourcesPath() {
    assertThat(TestUtils.mainCodeSourcesPath("Dummy.java"))
      .endsWith("Dummy.java");
  }

  @Test
  void checkMainCodeSourcesPath() {
    assertThat(TestUtils.mainCodeSourcesPath("Dummy.java"))
      .matches("(.+)java-checks-test-sources.default.src.main.java.Dummy\\.java");
  }

  @Test
  void checkTestCodeSourcesPath() {
    assertThat(TestUtils.testCodeSourcesPath("DummyTest.java"))
      .matches("(.+)java-checks-test-sources.default.src.test.java.DummyTest\\.java");
  }

  @Test
  void checkNonCompilingTestResourcesPath() {
    assertThat(TestUtils.nonCompilingTestSourcesPath("Dummy.java"))
      .matches("(.+)java-checks-test-sources.default.src.main.files.non-compiling.Dummy\\.java");
  }

  @Test
  void checkNonExistingTestResourcesPath() {
    assertThrows(IllegalStateException.class, () -> TestUtils.nonCompilingTestSourcesPath("NonExisting.java"),
      "Path 'NonExisting.java' should exist.");
    assertThrows(IllegalStateException.class, () -> TestUtils.mainCodeSourcesPath("NonExisting.java"),
      "Path 'NonExisting.java' should exist.");
  }

  @Test
  void checkInputFile() throws IOException {
    InputFile inputFile = TestUtils.inputFile("dummy-module", new File(DUMMY_FILE));

    assertThat(inputFile.contents()).isEmpty();
    assertThat(inputFile.charset()).isEqualTo(UTF_8);
    assertThat(inputFile.language()).isEqualTo("java");
  }

  @Test
  void checkDirInputFile() {
    File file = new File("src/test/files/dummy-module");
    assertThrows(
      IllegalStateException.class,
      () -> TestUtils.inputFile("dummy-module", file));
  }

  @Test
  void checkWrongInputFile() {
    File file = new File("non-existing-file.java");
    assertThrows(
      IllegalStateException.class,
      () -> TestUtils.inputFile("dummy-module", file));
  }

  @Test
  void checkInputFileWithoutModule() throws IOException {
    InputFile inputFile = TestUtils.inputFile(new File(DUMMY_FILE));

    assertThat(inputFile.contents()).isEmpty();
    assertThat(inputFile.charset()).isEqualTo(UTF_8);
    assertThat(inputFile.language()).isEqualTo("java");
  }

  @Test
  void checkInputPathWithoutModule() throws IOException {
    InputFile inputFile = TestUtils.inputFile(DUMMY_FILE);

    assertThat(inputFile.contents()).isEmpty();
    assertThat(inputFile.charset()).isEqualTo(UTF_8);
    assertThat(inputFile.language()).isEqualTo("java");
  }

  @Test
  void checkEmptyInputFile() throws IOException {
    InputFile inputFile = TestUtils.emptyInputFile(DUMMY_FILE);

    assertThat(inputFile.contents()).isEmpty();
    assertThat(inputFile.charset()).isEqualTo(UTF_8);
    assertThat(inputFile.type()).isEqualTo(MAIN);
    assertThat(inputFile.language()).isEqualTo("java");
  }

  @Test
  void checkEmptyTestInputFile() throws IOException {
    InputFile inputFile = TestUtils.emptyInputFile(DUMMY_FILE, TEST);

    assertThat(inputFile.contents()).isEmpty();
    assertThat(inputFile.charset()).isEqualTo(UTF_8);
    assertThat(inputFile.type()).isEqualTo(TEST);
    assertThat(inputFile.language()).isEqualTo("java");
  }
}
