/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import org.junit.Test;
import org.sonar.java.model.VisitorsBridge;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class JavaFilesCacheTest {

  @Test
  public void test() throws Exception {
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    JavaAstScanner.scanSingleFile(new File("src/test/java/org/sonar/java/JavaFilesCacheTest.java"), new VisitorsBridge(javaFilesCache));

    assertThat(javaFilesCache.resourcesCache.keySet()).hasSize(5);
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A$I");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A$1B");
    assertThat(javaFilesCache.resourcesCache.keySet()).contains("org/sonar/java/JavaFilesCacheTest$A$1B$1");

  }
  static class A {
    interface I{
      void foo();
    }
    private void method() {
      class B{
        Object obj = new I() {
          @Override
          public void foo() {

          }
        };
      }
    }
  }

}