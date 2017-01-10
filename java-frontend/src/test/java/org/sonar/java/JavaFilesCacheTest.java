/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaFilesCacheTest {

  @Test
  public void resource_file_mapping() {
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    JavaAstScanner.scanSingleFileForTests(new File("src/test/resources/JavaFilesCacheTestFile.java"), new VisitorsBridge(javaFilesCache));

    assertThat(javaFilesCache.resourcesCache.keySet()).hasSize(8);
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$plop");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A$I");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A$1B");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A$1B$1");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A$2");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A$3");
  }

}
