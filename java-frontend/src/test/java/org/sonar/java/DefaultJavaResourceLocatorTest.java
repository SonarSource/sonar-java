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
package org.sonar.java;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.model.VisitorsBridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultJavaResourceLocatorTest {

  private static DefaultJavaResourceLocator javaResourceLocator;

  private static final String BINARY_DIRS = "target/test-classes";
  private static final String TEST_BINARY_DIRS = "target/test/test-classes";

  @BeforeAll
  public static void setup() {
    ClasspathForMain javaClasspath = mock(ClasspathForMain.class);
    when(javaClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(new File(BINARY_DIRS)));
    when(javaClasspath.getElements()).thenReturn(Collections.singletonList(new File(BINARY_DIRS)));
    
    ClasspathForTest javaTestClasspath = mock(ClasspathForTest.class);
    when(javaTestClasspath.getBinaryDirs()).thenReturn(Collections.singletonList(new File(TEST_BINARY_DIRS)));
    when(javaTestClasspath.getElements()).thenReturn(Collections.singletonList(new File(TEST_BINARY_DIRS)));
    
    InputFile inputFile = TestUtils.inputFile("src/test/java/org/sonar/java/DefaultJavaResourceLocatorTest.java");
    DefaultJavaResourceLocator jrl = new DefaultJavaResourceLocator(javaClasspath, javaTestClasspath);
    JavaAstScanner.scanSingleFileForTests(inputFile, new VisitorsBridge(jrl));
    javaResourceLocator = jrl;
  }

  @Test
  void resource_by_class() throws Exception {
    Set<String> classNames = javaResourceLocator.resourcesByClass.keySet();
    assertThat(classNames)
      .hasSize(5)
      .contains(
        "org/sonar/java/DefaultJavaResourceLocatorTest",
        "org/sonar/java/DefaultJavaResourceLocatorTest$A",
        "org/sonar/java/DefaultJavaResourceLocatorTest$A$I",
        "org/sonar/java/DefaultJavaResourceLocatorTest$A$1B",
        "org/sonar/java/DefaultJavaResourceLocatorTest$A$1B$1");
  }

  @Test
  void resource_by_class_name() throws Exception {
    assertThat(javaResourceLocator.findResourceByClassName("org.sonar.java.DefaultJavaResourceLocatorTest")).isNotNull();
    assertThat(javaResourceLocator.findResourceByClassName("org.sonar.java.DumbClassName")).isNull();
  }

  @Test
  void classpath() throws Exception {
    var classpath = javaResourceLocator.classpath();
    assertThat(classpath).hasSize(1);

    var file = new File("");
    assertThatThrownBy(() -> classpath.add(file))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void testClasspath() throws Exception {
    var classpath = javaResourceLocator.testClasspath();
    assertThat(classpath).containsExactly(new File(TEST_BINARY_DIRS));

    var file = new File("");
    assertThatThrownBy(() -> classpath.add(file))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void binaryDirs() {
    var binaryDirs = javaResourceLocator.binaryDirs();
    assertThat(binaryDirs).containsExactly(new File(BINARY_DIRS));

    var file = new File("");
    assertThatThrownBy(() -> binaryDirs.add(file))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void testBinaryDirs() {
    var binaryDirs = javaResourceLocator.testBinaryDirs();
    assertThat(binaryDirs).containsExactly(new File(TEST_BINARY_DIRS));

    var file = new File("");
    assertThatThrownBy(() -> binaryDirs.add(file))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void classFilesToAnalyze() throws Exception {
    assertThat(javaResourceLocator.classFilesToAnalyze()).hasSize(5);
  }

  static class A { // NOSONAR

    interface I {
      void foo();
    }

    private void method() {
      class B {
        Object obj = new I() {
          @Override
          public void foo() {

          }
        };
      }
    }
  }
}
