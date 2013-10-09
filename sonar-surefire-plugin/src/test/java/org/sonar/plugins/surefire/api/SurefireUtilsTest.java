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
package org.sonar.plugins.surefire.api;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SurefireUtilsTest {

  @Test
  public void shouldGetReportsFromProperty() {
    Settings settings = mock(Settings.class);
    when(settings.getString("sonar.junit.reportsPath")).thenReturn("target/surefire");

    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "shouldGetReportsFromProperty/pom.xml");
    assertThat(SurefireUtils.getReportsDirectory(settings, project).exists()).isTrue();
    assertThat(SurefireUtils.getReportsDirectory(settings, project).isDirectory()).isTrue();
  }

  @Test
  public void shouldGetReportsFromPluginConfiguration() {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "shouldGetReportsFromPluginConfiguration/pom.xml");
    assertThat(SurefireUtils.getReportsDirectory(mock(Settings.class), project).exists()).isTrue();
    assertThat(SurefireUtils.getReportsDirectory(mock(Settings.class), project).isDirectory()).isTrue();
  }

}
