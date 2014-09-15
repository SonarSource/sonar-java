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
package org.sonar.plugins.java;

import org.junit.Test;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultJavaResourceLocatorTest {

  @Test
  public void test() throws Exception {
    Project project = mock(Project.class);
    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    File baseDir = new File("src/test/java");
    when(project.getFileSystem()).thenReturn(pfs);
    when(pfs.getBasedir()).thenReturn(baseDir);
    DefaultJavaResourceLocator javaResourceLocator = new DefaultJavaResourceLocator(project, null, mock(NoSonarFilter.class));
    JavaAstScanner.scanSingleFile(new File("src/test/java/org/sonar/plugins/java/DefaultJavaResourceLocatorTest.java"), new VisitorsBridge(javaResourceLocator));

    assertThat(javaResourceLocator.resourcesCache.keySet()).hasSize(5);
    assertThat(javaResourceLocator.resourcesCache.keySet()).contains("org/sonar/plugins/java/DefaultJavaResourceLocatorTest");
    assertThat(javaResourceLocator.resourcesCache.keySet()).contains("org/sonar/plugins/java/DefaultJavaResourceLocatorTest$A");
    assertThat(javaResourceLocator.resourcesCache.keySet()).contains("org/sonar/plugins/java/DefaultJavaResourceLocatorTest$A$I");
    assertThat(javaResourceLocator.resourcesCache.keySet()).contains("org/sonar/plugins/java/DefaultJavaResourceLocatorTest$A$1B");
    assertThat(javaResourceLocator.resourcesCache.keySet()).contains("org/sonar/plugins/java/DefaultJavaResourceLocatorTest$A$1B$1");

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