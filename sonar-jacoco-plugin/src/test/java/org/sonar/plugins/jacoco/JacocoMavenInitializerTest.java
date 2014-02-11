/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
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
package org.sonar.plugins.jacoco;

import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JacocoMavenInitializerTest {

  private final Project project = mockProject();
  private final JaCoCoMavenPluginHandler mavenPluginHandler = mock(JaCoCoMavenPluginHandler.class);
  private final JacocoConfiguration jacocoSettings = mock(JacocoConfiguration.class);

  private final JacocoMavenInitializer initializer = new JacocoMavenInitializer(mavenPluginHandler, jacocoSettings);

  @Test
  public void shouldDoNothing() {
    initializer.analyse(project, mock(SensorContext.class));
    verifyNoMoreInteractions(project);
    verifyNoMoreInteractions(mavenPluginHandler);
  }

  @Test
  public void should_not_execute_when_jacoco_not_enabled() {
    when(jacocoSettings.isEnabled(project)).thenReturn(false);

    assertThat(initializer.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_execute_when_reuse_reports() {
    when(jacocoSettings.isEnabled(project)).thenReturn(true);
    when(project.getFileSystem().testFiles(Java.KEY)).thenReturn(Collections.singletonList(mock(InputFile.class)));
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);

    assertThat(initializer.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_execute_when_no_tests() {
    when(jacocoSettings.isEnabled(project)).thenReturn(true);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);

    assertThat(initializer.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_execute() {
    when(jacocoSettings.isEnabled(project)).thenReturn(true);
    when(project.getFileSystem().testFiles(Java.KEY)).thenReturn(Collections.singletonList(mock(InputFile.class)));
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);

    assertThat(initializer.shouldExecuteOnProject(project)).isTrue();
    assertThat(initializer.getMavenPluginHandler(project)).isInstanceOf(JaCoCoMavenPluginHandler.class);
  }

  private Project mockProject() {
    Project project = mock(Project.class);
    ProjectFileSystem projectFileSystem = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(projectFileSystem);
    return project;
  }

}
