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
package org.sonar.plugins.java.api;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

import static org.fest.assertions.Assertions.assertThat;

public class JavaSettingsTest {

  JavaSettings javaSettings;
  Settings settings;

  @Before
  public void init() {
    settings = new Settings(new PropertyDefinitions(JavaSettings.class));
    javaSettings = new JavaSettings(settings);
  }

  @Test
  public void default_coverage_plugin_is_jacoco() {
    assertThat(javaSettings.getEnabledCoveragePlugin()).isEqualTo("jacoco");
  }

  @Test
  public void should_change_coverage_plugin() {
    settings.setProperty("sonar.java.coveragePlugin", "cobertura");
    assertThat(javaSettings.getEnabledCoveragePlugin()).isEqualTo("cobertura");
  }

  @Test
  public void should_support_deprecated_coverage_property() {
    // many values
    settings.setProperty("sonar.core.codeCoveragePlugin", "clover,cobertura");
    assertThat(javaSettings.getEnabledCoveragePlugin()).isEqualTo("clover");

    // only one value
    settings.setProperty("sonar.core.codeCoveragePlugin", "emma");
    assertThat(javaSettings.getEnabledCoveragePlugin()).isEqualTo("emma");
  }

}
