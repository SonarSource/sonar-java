/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;

import static org.assertj.core.api.Assertions.assertThat;

class JavaFilesCacheTest {

  @Test
  void resource_file_mapping() {
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    JavaAstScanner.scanSingleFileForTests(TestUtils.inputFile("src/test/resources/JavaFilesCacheTestFile.java"), new VisitorsBridge(javaFilesCache));

    Set<String> classNames = javaFilesCache.getClassNames();
    assertThat(classNames)
      .hasSize(8)
      .contains(
        "org/sonar/java/JavaFilesCacheTestFile",
        "org/sonar/java/JavaFilesCacheTestFile$A",
        "org/sonar/java/JavaFilesCacheTestFile$plop",
        "org/sonar/java/JavaFilesCacheTestFile$A$I",
        "org/sonar/java/JavaFilesCacheTestFile$A$1B",
        "org/sonar/java/JavaFilesCacheTestFile$A$1B$1",
        "org/sonar/java/JavaFilesCacheTestFile$A$2",
        "org/sonar/java/JavaFilesCacheTestFile$A$3");
  }

}
