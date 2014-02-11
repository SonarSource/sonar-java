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
package org.sonar.plugins.findbugs;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.test.MavenTestUtils;

import java.util.ArrayList;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FindbugsMavenInitializerTest {

  private final Project project = mock(Project.class);
  private final FindbugsMavenInitializer initializer = new FindbugsMavenInitializer();

  @Test
  public void should_analyse() {
    assertThat(initializer.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void doNotSetExcludesFiltersIfAlreadyConfigured() {
    Configuration configuration = mock(Configuration.class);
    when(configuration.containsKey(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY)).thenReturn(true);
    when(project.getConfiguration()).thenReturn(configuration);
    initializer.execute(project);
    verify(configuration, never()).setProperty(eq(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY), anyString());
  }

  @Test
  public void shouldGetExcludesFiltersFromPom() {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "pom.xml");
    initializer.execute(project);
    assertThat(project.getConfiguration().getString(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY)).isEqualTo("foo.xml");
  }

}
