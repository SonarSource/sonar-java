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

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.java.SonarComponents;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaSquidSensorTest {

  private final ModuleFileSystem moduleFileSystem = mock(ModuleFileSystem.class);
  private JavaSquidSensor sensor;

  @Before
  public void setUp() {
    sensor = new JavaSquidSensor(mock(RulesProfile.class), mock(NoSonarFilter.class), mock(ProjectClasspath.class), mock(SonarComponents.class), moduleFileSystem, mock(DefaultJavaResourceLocator.class), new Settings());
  }

  @Test
  public void should_execute_on_java_project() {
    Project project = mock(Project.class);

    when(moduleFileSystem.files(any(FileQuery.class))).thenReturn(ImmutableList.<File>of());
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(moduleFileSystem.files(any(FileQuery.class))).thenReturn(ImmutableList.<File>of(new File("Fake.java")));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("JavaSquidSensor");
  }

}
