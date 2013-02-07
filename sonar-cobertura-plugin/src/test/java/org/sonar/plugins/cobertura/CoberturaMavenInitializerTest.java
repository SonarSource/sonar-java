/*
 * Sonar Java
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
package org.sonar.plugins.cobertura;

import org.apache.commons.configuration.Configuration;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.test.MavenTestUtils;
import org.sonar.plugins.cobertura.base.CoberturaConstants;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoberturaMavenInitializerTest {

  private Project project;
  private CoberturaMavenInitializer initializer;
  private CoberturaMavenPluginHandler handler;
  private CoberturaSettings settings;

  @Before
  public void setUp() {
    project = mock(Project.class);
    handler = mock(CoberturaMavenPluginHandler.class);
    settings = mock(CoberturaSettings.class);
    initializer = new CoberturaMavenInitializer(handler, settings);
  }


  @Test
  public void doNotExecuteMavenPluginIfReuseReports() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(initializer.getMavenPluginHandler(project)).isNull();
  }

  @Test
  public void doNotExecuteMavenPluginIfStaticAnalysis() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(initializer.getMavenPluginHandler(project)).isNull();
  }

  @Test
  public void executeMavenPluginIfDynamicAnalysis() {
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(initializer.getMavenPluginHandler(project)).isSameAs(handler);
  }

  @Test
  public void doNotSetReportPathIfAlreadyConfigured() {
    Configuration configuration = mock(Configuration.class);
    when(configuration.containsKey(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY)).thenReturn(true);
    when(project.getConfiguration()).thenReturn(configuration);
    initializer.execute(project);
    verify(configuration, never()).setProperty(eq(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY), anyString());
  }

  @Test
  public void shouldSetReportPathFromPom() {
    Configuration configuration = mock(Configuration.class);
    when(project.getConfiguration()).thenReturn(configuration);
    MavenProject pom = MavenTestUtils.loadPom("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldGetReportPathFromPom/pom.xml");
    when(project.getPom()).thenReturn(pom);
    initializer.execute(project);
    verify(configuration).setProperty(eq(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY), eq("overridden/dir/coverage.xml"));
  }

  @Test
  public void shouldSetDefaultReportPath() {
    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    when(pfs.getReportOutputDir()).thenReturn(new File("reportOutputDir"));
    Configuration configuration = mock(Configuration.class);
    when(project.getConfiguration()).thenReturn(configuration);
    when(project.getFileSystem()).thenReturn(pfs);
    initializer.execute(project);
    verify(configuration).setProperty(eq(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY), eq("reportOutputDir/cobertura/coverage.xml"));
  }
}
