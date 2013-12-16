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

import edu.umd.cs.findbugs.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.scan.filesystem.SimpleModuleFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FindbugsConfigurationTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private SimpleModuleFileSystem fs;
  private Settings settings;
  private File baseDir;
  private FindbugsConfiguration conf;
  private ProjectClasspath classpath;

  @Before
  public void setUp() {
    baseDir = temp.newFolder("findbugs");
    fs = new SimpleModuleFileSystem(baseDir);
    settings = new Settings(new PropertyDefinitions().addComponents(FindbugsConfiguration.getPropertyDefinitions()));
    classpath = mock(ProjectClasspath.class);
    conf = new FindbugsConfiguration(fs, settings, RulesProfile.create(), new FindbugsProfileExporter(), classpath);
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

  @Test
  public void should_fail_if_no_binary_dirs() throws IOException {
    thrown.expect(SonarException.class);
    thrown.expectMessage("Findbugs needs sources to be compiled");

    assertThat(fs.binaryDirs()).isEmpty();
    conf.getFindbugsProject();
  }

  @Test
  public void should_support_multiple_binary_dirs() throws IOException {
    File binaryDir1 = temp.newFolder("binary1");
    File binaryFile11 = temp.newFile("binary1/MyClass11.class");
    temp.newFolder("binary1/sub");
    File binaryFile12 = temp.newFile("binary1/sub/MyClass12.class");
    temp.newFile("binary1/Fake.txt");
    File binaryDir2 = temp.newFolder("binary2");
    File binaryFile21 = temp.newFile("binary2/MyClass21.class");

    fs.addBinaryDir(binaryDir1);
    fs.addBinaryDir(binaryDir2);

    Project findbugsProject = conf.getFindbugsProject();

    assertThat(findbugsProject.getFileList()).containsOnly(
      binaryFile11.getCanonicalPath(),
      binaryFile12.getCanonicalPath(),
      binaryFile21.getCanonicalPath());
  }

  @Test
  public void should_copy_lib_in_working_dir() throws IOException {
    String jsr205 = "findbugs/jsr305.jar";
    String annotations = "findbugs/annotations.jar";

    conf.start();
    assertThat(new File(fs.workingDir(), jsr205)).isFile();
    assertThat(new File(fs.workingDir(), annotations)).isFile();
    conf.stop();
    assertThat(new File(fs.workingDir(), jsr205)).doesNotExist();
    assertThat(new File(fs.workingDir(), annotations)).doesNotExist();
  }

}
