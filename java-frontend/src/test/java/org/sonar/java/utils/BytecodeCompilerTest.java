/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaVersion;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class BytecodeCompilerTest {

  @TempDir
  Path temporaryFolder;

  @Test
  void compile_valid_source() throws Exception {
    Path sourceFile = createSourceFile("Foo.java", "public class Foo {}");
    Path outputDir = temporaryFolder.resolve("output");

    boolean result = BytecodeCompiler.compile(List.of(sourceFile), outputDir, javaVersion(11), javaVersion(11));

    assertThat(result).isTrue();
    assertThat(outputDir.resolve("Foo.class")).exists();
  }

  @Test
  void compile_source_with_package() throws Exception {
    Path sourceFile = createSourceFile("Bar.java",
      "package com.example;\npublic class Bar {}");
    Path outputDir = temporaryFolder.resolve("output");

    boolean result = BytecodeCompiler.compile(List.of(sourceFile), outputDir, javaVersion(11), javaVersion(11));

    assertThat(result).isTrue();
    assertThat(outputDir.resolve("com/example/Bar.class")).exists();
  }

  @Test
  void compile_source_with_syntax_error() throws Exception {
    Path sourceFile = createSourceFile("Invalid.java",
      "public class Invalid { invalid syntax }");
    Path outputDir = temporaryFolder.resolve("output");

    boolean result = BytecodeCompiler.compile(List.of(sourceFile), outputDir, javaVersion(11), javaVersion(11));

    assertThat(result).isFalse();
  }

  @Test
  void compile_empty_source_list() throws Exception {
    Path outputDir = temporaryFolder.resolve("output");

    boolean result = BytecodeCompiler.compile(List.of(), outputDir, javaVersion(11), javaVersion(11));

    assertThat(result).isTrue();
    assertThat(outputDir).doesNotExist();
  }

  @Test
  void compile_multiple_sources() throws Exception {
    Path sourceFile1 = createSourceFile("Foo.java", "public class Foo {}");
    Path sourceFile2 = createSourceFile("Bar.java", "public class Bar {}");
    Path outputDir = temporaryFolder.resolve("output");

    boolean result = BytecodeCompiler.compile(List.of(sourceFile1, sourceFile2), outputDir, javaVersion(11), javaVersion(11));

    assertThat(result).isTrue();
    assertThat(outputDir.resolve("Foo.class")).exists();
    assertThat(outputDir.resolve("Bar.class")).exists();
  }

  @Test
  void compile_with_different_source_and_target_versions() throws Exception {
    Path sourceFile = createSourceFile("Foo.java", "public class Foo {}");
    Path outputDir = temporaryFolder.resolve("output");

    boolean result = BytecodeCompiler.compile(List.of(sourceFile), outputDir, javaVersion(17), javaVersion(11));

    assertThat(result).isTrue();
    assertThat(outputDir.resolve("Foo.class")).exists();
  }

  private JavaVersion javaVersion(int version) {
    return new JavaVersionImpl(version);
  }

  private Path createSourceFile(String filename, String content) throws Exception {
    Path sourceFile = temporaryFolder.resolve(filename);
    Files.write(sourceFile, content.getBytes(UTF_8));
    return sourceFile;
  }

}
