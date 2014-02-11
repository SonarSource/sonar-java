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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenSurefireUtils;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;
import org.sonar.plugins.java.api.JavaSettings;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JaCoCoMavenPluginHandlerTest {

  private JacocoConfiguration configuration;
  private JaCoCoMavenPluginHandler handler;

  @Before
  public void setUp() throws Exception {
    JaCoCoAgentDownloader downloader = mock(JaCoCoAgentDownloader.class);
    when(downloader.getAgentJarFile()).thenReturn(new File("jacocoagent.jar"));
    Settings settings = new Settings(new PropertyDefinitions().addComponents(JacocoConfiguration.getPropertyDefinitions()));
    configuration = spy(new JacocoConfiguration(settings, downloader, new JavaSettings(settings)));

    handler = new JaCoCoMavenPluginHandler(configuration);
  }

  @Test
  public void testMavenPluginDefinition() {
    assertThat(handler.getGroupId()).isEqualTo(MavenSurefireUtils.GROUP_ID);
    assertThat(handler.getArtifactId()).isEqualTo(MavenSurefireUtils.ARTIFACT_ID);
    assertThat(handler.getVersion()).isEqualTo(MavenSurefireUtils.VERSION);
    assertThat(handler.getGoals()).isEqualTo(new String[] { "test" });
    assertThat(handler.isFixedVersion()).isFalse();
  }

  @Test
  public void testConfigureMavenPlugin() {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "pom.xml");
    MavenPlugin plugin = new MavenPlugin(handler.getGroupId(), handler.getArtifactId(), handler.getVersion());

    handler.configure(project, plugin);

    verify(configuration).getJvmArgument();
    assertThat(plugin.getParameter("argLine")).isEqualTo("-javaagent:jacocoagent.jar=destfile=target/jacoco.exec,excludes=*_javassist_*");
    assertThat(plugin.getParameter("testFailureIgnore")).isEqualTo("true");
  }

  @Test
  public void testReconfigureMavenPlugin() {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "pom2.xml");
    MavenPlugin plugin = MavenPlugin.getPlugin(project.getPom(), handler.getGroupId(), handler.getArtifactId());

    handler.configure(project, plugin);

    verify(configuration).getJvmArgument();
    assertThat(plugin.getParameter("argLine")).isEqualTo("-javaagent:jacocoagent.jar=destfile=target/jacoco.exec,excludes=*_javassist_* -esa");
    assertThat(plugin.getParameter("testFailureIgnore")).isEqualTo("true");
  }

}
