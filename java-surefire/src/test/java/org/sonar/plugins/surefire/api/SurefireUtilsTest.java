/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.plugins.surefire.api;

import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SurefireUtilsTest {

  @Test
  public void shouldGetReportsFromProperty() {
    Settings settings = new MapSettings();
    settings.setProperty("sonar.junit.reportsPath", "target/surefire");

    DefaultFileSystem fs = new DefaultFileSystem(new File("src/test/resources/org/sonar/plugins/surefire/api/SurefireUtilsTest/shouldGetReportsFromProperty"));
    PathResolver pathResolver = new PathResolver();
    assertThat(SurefireUtils.getReportsDirectory(settings, fs, pathResolver).exists()).isTrue();
    assertThat(SurefireUtils.getReportsDirectory(settings, fs, pathResolver).isDirectory()).isTrue();
  }


  @Test
  public void return_default_value_if_property_unset() throws Exception {
    Settings settings = mock(Settings.class);
    DefaultFileSystem fs = new DefaultFileSystem(new File("src/test/resources/org/sonar/plugins/surefire/api/SurefireUtilsTest/shouldGetReportsFromProperty"));
    PathResolver pathResolver = new PathResolver();
    File directory = SurefireUtils.getReportsDirectory(settings, fs, pathResolver);
    assertThat(directory.getCanonicalPath()).endsWith("target"+File.separator+"surefire-reports");
    assertThat(directory.exists()).isFalse();
    assertThat(directory.isDirectory()).isFalse();
  }
}
