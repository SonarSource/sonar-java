/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
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
    JavaAstScanner.scanSingleFile(new File("src/test/java/org/sonar/java/JavaFilesCacheTest.java"), new VisitorsBridge(javaFilesCache));

    assertThat(javaFilesCache.resourcesCache.keySet()).hasSize(6);
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest$plop");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A$I");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A$1B");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A$1B$1");
  }

  @Test
  public void method_start_lines_mapping() {
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    JavaAstScanner.scanSingleFile(new File("src/test/java/org/sonar/java/JavaFilesCacheTest.java"), new VisitorsBridge(javaFilesCache));
    assertThat(javaFilesCache.methodStartLines.keySet()).hasSize(5);
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A$I#foo()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A$1B$1#foo()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTest#method_start_lines_mapping()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A#method()V");
    assertThat(javaFilesCache.methodStartLines.keySet()).contains("org/sonar/java/JavaFilesCacheTest#resource_file_mapping()V");
    assertThat(javaFilesCache.suppressWarningLines.keySet()).hasSize(13);
    for (Integer line : Lists.newArrayList(68, 69, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83)) {
      assertThat(javaFilesCache.suppressWarningLines.get(line)).contains("all");
    }
    for (Integer line : Lists.newArrayList(77, 78, 79, 80, 81)) {
      assertThat(javaFilesCache.suppressWarningLines.get(line)).contains("foo", "bar");
    }
  }

  static class A {
    interface I {
      @SuppressWarnings("all")
      void foo();
    }

    private void method() {
      @SuppressWarnings("all")
      class B {
        Object obj = new I() {

          @SuppressWarnings({"foo", "bar"})
          @Override
          public void foo() {

          }
        };
      }
    }
  }
  @interface plop {
  }

}
