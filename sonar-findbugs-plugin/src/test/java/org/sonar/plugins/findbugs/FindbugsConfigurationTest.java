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
package org.sonar.plugins.findbugs;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.SimpleModuleFileSystem;

import java.io.File;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FindbugsConfigurationTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private ModuleFileSystem fs;
  private Settings settings;
  private File baseDir;
  private FindbugsConfiguration conf;

  @Before
  public void setUp() {
    baseDir = tempFolder.newFolder("findbugs");
    fs = new SimpleModuleFileSystem(baseDir);
    settings = new Settings(new PropertyDefinitions().addComponent(FindbugsPlugin.class));
    conf = new FindbugsConfiguration(fs, settings, RulesProfile.create(), new FindbugsProfileExporter(), null);
  }

  @Test
  public void should_return_report_file() throws Exception {
    assertThat(conf.getTargetXMLReport().getCanonicalPath()).isEqualTo(new File(fs.workingDir(), "findbugs-result.xml").getCanonicalPath());
  }

  @Test
  public void should_save_include_config() throws Exception {
    conf.saveIncludeConfigXml();
    File findbugsIncludeFile = new File(fs.workingDir(), "findbugs-include.xml");
    assertThat(findbugsIncludeFile.exists()).isTrue();
  }

  @Test
  public void should_return_effort() {
    assertThat(conf.getEffort()).as("default effort").isEqualTo("default");
    settings.setProperty(FindbugsConstants.EFFORT_PROPERTY, "Max");
    assertThat(conf.getEffort()).isEqualTo("max");
  }

  @Test
  public void should_return_timeout() {
    assertThat(conf.getTimeout()).as("default timeout").isEqualTo(600000);
    settings.setProperty(FindbugsConstants.TIMEOUT_PROPERTY, 1);
    assertThat(conf.getTimeout()).isEqualTo(1);
  }

  @Test
  public void should_return_locale() {
    settings.setProperty(CoreProperties.CORE_VIOLATION_LOCALE_PROPERTY, "fr");
    assertThat(conf.getLocale()).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void should_return_excludes_filters() {
    assertThat(conf.getExcludesFilters()).isEmpty();
    settings.setProperty(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY, " foo.xml , bar.xml,");
    assertThat(conf.getExcludesFilters()).hasSize(2);
  }

  @Test
  public void should_return_confidence_level() {
    assertThat(conf.getConfidenceLevel()).as("default confidence level").isEqualTo("medium");
    settings.setProperty(FindbugsConstants.EFFORT_PROPERTY, "HIGH");
    assertThat(conf.getEffort()).isEqualTo("high");
  }

}
