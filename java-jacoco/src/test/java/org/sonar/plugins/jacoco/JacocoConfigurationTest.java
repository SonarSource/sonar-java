/*
 * SonarQube Java
 * Copyright (C) 2010-2017 SonarSource SA
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
package org.sonar.plugins.jacoco;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class JacocoConfigurationTest {

  private Settings settings;
  private JacocoConfiguration jacocoConfiguration;

  @Before
  public void setUp() {
    settings = new MapSettings(new PropertyDefinitions().addComponents(JacocoConfiguration.getPropertyDefinitions(Version.create(5, 6))));
    jacocoConfiguration = new JacocoConfiguration(settings);
  }

  @Test
  public void shouldExecuteOnProject() throws Exception {
    assertThat(jacocoConfiguration.shouldExecuteOnProject(true)).isTrue();
    assertThat(jacocoConfiguration.shouldExecuteOnProject(false)).isFalse();
    settings.setProperty(JacocoConfiguration.REPORT_MISSING_FORCE_ZERO, true);
    assertThat(jacocoConfiguration.shouldExecuteOnProject(true)).isTrue();
    assertThat(jacocoConfiguration.shouldExecuteOnProject(false)).isTrue();
  }

  @Test
  public void defaults() {
    assertThat(jacocoConfiguration.getReportPath()).isEqualTo("target/jacoco.exec");
    assertThat(jacocoConfiguration.getItReportPath()).isEqualTo("target/jacoco-it.exec");
  }

  @Test
  public void shouldReturnItReportPath() {
    settings.setProperty(JacocoConfiguration.IT_REPORT_PATH_PROPERTY, "target/it-jacoco-test.exec");

    assertThat(jacocoConfiguration.getItReportPath()).isEqualTo("target/it-jacoco-test.exec");
  }

  @Test
  public void shouldSetDestfile() {
    settings.setProperty(JacocoConfiguration.REPORT_PATH_PROPERTY, "jacoco.exec");

    assertThat(jacocoConfiguration.getReportPath()).isEqualTo("jacoco.exec");
  }

  @Test
  public void shouldQuoteDestfileWithSpace() {
    settings.setProperty(JacocoConfiguration.REPORT_PATH_PROPERTY, "folder spaced/jacoco.exec");

    assertThat(jacocoConfiguration.getReportPath()).isEqualTo("folder spaced/jacoco.exec");
  }

}
