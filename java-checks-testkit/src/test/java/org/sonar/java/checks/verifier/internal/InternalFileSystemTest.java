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
package org.sonar.java.checks.verifier.internal;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.sonar.api.batch.fs.FileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalFileSystemTest {

  @Test
  void methods() {
    FileSystem fs = new InternalFileSystem();

    assertThat(fs.baseDir()).isNotNull();
    assertThat(fs.encoding()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(fs.files(null)).isEmpty();
    assertThat(fs.hasFiles(null)).isFalse();
    assertThat(fs.languages()).containsOnly("java");

    assertMethodNotSupported(() -> fs.inputDir(null), "InternalFileSystem::inputDir(File)");
    assertMethodNotSupported(() -> fs.inputFile(null), "InternalFileSystem::inputFile(FilePredicate)");
    assertMethodNotSupported(() -> fs.inputFiles(null), "InternalFileSystem::inputFiles(FilePredicate)");
    assertMethodNotSupported(() -> fs.predicates(), "InternalFileSystem::predicates()");
    assertMethodNotSupported(() -> fs.resolvePath(null), "InternalFileSystem::resolvePath(String)");
    assertMethodNotSupported(() -> fs.workDir(), "InternalFileSystem::workDir()");
  }

  private static void assertMethodNotSupported(Executable executable, String expectedMessage) {
    InternalMockedSonarAPI.NotSupportedException e = assertThrows(InternalMockedSonarAPI.NotSupportedException.class, executable);
    assertThat(e).hasMessage(String.format("Method unsuported by the rule verifier framework: '%s'", expectedMessage));
  }
}
