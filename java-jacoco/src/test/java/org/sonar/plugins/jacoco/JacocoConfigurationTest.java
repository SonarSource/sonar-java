/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

import static org.fest.assertions.Assertions.assertThat;

public class JacocoConfigurationTest {

  private Settings settings;
  private JacocoConfiguration jacocoSettings;
  private DefaultFileSystem fileSystem;

  @Before
  public void setUp() {
    settings = new Settings(new PropertyDefinitions().addComponents(JacocoConfiguration.getPropertyDefinitions()));
    fileSystem = new DefaultFileSystem(null);
    jacocoSettings = new JacocoConfiguration(settings, fileSystem);
  }

  @Test
  public void shouldExecuteOnProject() throws Exception {
    assertThat(jacocoSettings.shouldExecuteOnProject(true)).isFalse();
    assertThat(jacocoSettings.shouldExecuteOnProject(false)).isFalse();
    DefaultInputFile phpFile = new DefaultInputFile("src/foo/bar.php");
    phpFile.setLanguage("php");
    fileSystem.add(phpFile);
    assertThat(jacocoSettings.shouldExecuteOnProject(true)).isFalse();
    assertThat(jacocoSettings.shouldExecuteOnProject(false)).isFalse();
    DefaultInputFile javaFile = new DefaultInputFile("src/foo/bar.java");
    javaFile.setLanguage("java");
    fileSystem.add(javaFile);
    assertThat(jacocoSettings.shouldExecuteOnProject(true)).isTrue();
    assertThat(jacocoSettings.shouldExecuteOnProject(false)).isFalse();
    settings.setProperty(JacocoConfiguration.REPORT_MISSING_FORCE_ZERO, true);
    assertThat(jacocoSettings.shouldExecuteOnProject(true)).isTrue();
    assertThat(jacocoSettings.shouldExecuteOnProject(false)).isTrue();
  }

  @Test
  public void defaults() {
    assertThat(jacocoSettings.getReportPath()).isEqualTo("target/jacoco.exec");
    assertThat(jacocoSettings.getItReportPath()).isEqualTo("target/jacoco-it.exec");
  }

  @Test
  public void shouldReturnItReportPath() {
    settings.setProperty(JacocoConfiguration.IT_REPORT_PATH_PROPERTY, "target/it-jacoco-test.exec");

    assertThat(jacocoSettings.getItReportPath()).isEqualTo("target/it-jacoco-test.exec");
  }

  @Test
  public void shouldSetDestfile() {
    settings.setProperty(JacocoConfiguration.REPORT_PATH_PROPERTY, "jacoco.exec");

    assertThat(jacocoSettings.getReportPath()).isEqualTo("jacoco.exec");
  }

  @Test
  public void shouldQuoteDestfileWithSpace() {
    settings.setProperty(JacocoConfiguration.REPORT_PATH_PROPERTY, "folder spaced/jacoco.exec");

    assertThat(jacocoSettings.getReportPath()).isEqualTo("folder spaced/jacoco.exec");
  }

}
