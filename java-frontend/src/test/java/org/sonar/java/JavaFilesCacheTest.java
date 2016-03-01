/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

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

  @Test
  public void method_start_lines_mapping() {
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    JavaAstScanner.scanSingleFileForTests(new File("src/test/resources/JavaFilesCacheTestFile.java"), new VisitorsBridge(javaFilesCache));
    assertThat(javaFilesCache.methodStartLines.keySet()).hasSize(8);
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A$I#foo()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A$1B$1#foo()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile#foo()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A#method()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile#bar()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A$2#foo()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A#foo(I)V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTestFile$A$3#foo()V");
  }

  @Test
  public void suppressWarning_lines_mapping() {
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    JavaAstScanner.scanSingleFileForTests(new File("src/test/resources/JavaFilesCacheTestFile.java"), new VisitorsBridge(javaFilesCache));
    assertThat(javaFilesCache.suppressWarningLines.keySet()).hasSize(28);
    for (Integer line : Lists.newArrayList(14, 15, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29)) {
      assertThat(javaFilesCache.suppressWarningLines.get(line)).contains("all");
    }
    for (Integer line : Lists.newArrayList(23, 24, 25, 26, 27)) {
      assertThat(javaFilesCache.suppressWarningLines.get(line)).contains("foo", "bar");
    }

    for (Integer line : Lists.newArrayList(10, 11, 32, 33, 34, 35, 36, 37)) {
      assertThat(javaFilesCache.suppressWarningLines.get(line)).containsOnly("qix");
    }

    for (Integer line : Lists.newArrayList(39, 41, 42, 43, 44, 45, 46)) {
      assertThat(javaFilesCache.suppressWarningLines.get(line)).containsOnly("gul");
    }
  }

  @Test
  public void suppressWarning_external() {
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    JavaAstScanner.scanSingleFileForTests(new File("src/test/resources/JavaSuppressWarning.java"), new VisitorsBridge(javaFilesCache));
    assertThat(javaFilesCache.suppressWarningLines.keySet()).hasSize(3);
    for (Integer line : Lists.newArrayList(10, 11)) {
      assertThat(javaFilesCache.suppressWarningLines.get(line)).as("on line " + line).contains("foo");
    }
  }
}
