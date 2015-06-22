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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaAstScannerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SensorContext context;
  Project sonarProject;

  @Before
  public void setUp() throws Exception {
    context = mock(SensorContext.class);
    sonarProject = mock(Project.class);
    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    File baseDir = new File("src/test/files/metrics");
    when(sonarProject.getFileSystem()).thenReturn(pfs);
    when(pfs.getBasedir()).thenReturn(baseDir);
  }

  @Test
  public void comments() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/Comments.java"), new VisitorsBridge(new Measurer(sonarProject, context, false)));
    assertThat(file.getNoSonarTagLines()).contains(15).hasSize(1);
  }

  @Test
  public void noSonarLines() throws Exception {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/NoSonar.java"), new VisitorsBridge(new Measurer(sonarProject, context, false)));
    assertThat(file.getNoSonarTagLines()).hasSize(1);
    assertThat(file.getNoSonarTagLines()).contains(8);
  }

  @Test
  public void scan_single_file_with_dumb_file_should_fail() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    String filename = "!!dummy";
    expectedException.expectMessage(filename);
    JavaAstScanner.scanSingleFile(new File(filename), new VisitorsBridge(null));

  }
}
