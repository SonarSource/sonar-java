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

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.internal.DefaultFilePredicates;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindbugsConfigurationTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private FileSystem fs;
  private ModuleFileSystem mfs;
  private Settings settings;
  private File baseDir;
  private FindbugsConfiguration conf;
  private ProjectClasspath classpath;
  private JavaResourceLocator javaResourceLocator;

  @Before
  public void setUp() throws Exception {
    baseDir = temp.newFolder("findbugs");

    mfs = mock(ModuleFileSystem.class);
    when(mfs.workingDir()).thenReturn(temp.newFolder());
    when(mfs.baseDir()).thenReturn(baseDir);

    fs = mock(FileSystem.class);
    when(fs.predicates()).thenReturn(new DefaultFileSystem().predicates());

    settings = new Settings(new PropertyDefinitions().addComponents(FindbugsConfiguration.getPropertyDefinitions()));
    classpath = mock(ProjectClasspath.class);
    javaResourceLocator = mock(JavaResourceLocator.class);
    conf = new FindbugsConfiguration(fs, mfs, settings, RulesProfile.create(), new FindbugsProfileExporter(), classpath, javaResourceLocator);
  }

  @Test
  public void should_return_report_file() throws Exception {
    assertThat(conf.getTargetXMLReport().getCanonicalPath()).isEqualTo(new File(mfs.workingDir(), "findbugs-result.xml").getCanonicalPath());
  }

  @Test
  public void should_save_include_config() throws Exception {
    conf.saveIncludeConfigXml();
    File findbugsIncludeFile = new File(mfs.workingDir(), "findbugs-include.xml");
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
  public void should_fail_if_source_files_but_no_class_files() throws IOException, AnalysisNotNeededException {
    when(fs.hasFiles(any(FilePredicate.class))).thenReturn(true);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Findbugs needs sources to be compiled");

    conf.getFindbugsProject();
  }

  @Test
  public void should_fail_if_no_source_files_and_no_class_files() throws IOException, AnalysisNotNeededException {
    when(fs.hasFiles(any(FilePredicate.class))).thenReturn(false);

    thrown.expect(AnalysisNotNeededException.class);

    conf.getFindbugsProject();
  }

  @Test
  public void should_set_class_files() throws IOException, AnalysisNotNeededException {
    File file = temp.newFile("MyClass.class");
    when(javaResourceLocator.classFilesToAnalyze()).thenReturn(ImmutableList.of(file));
    Project findbugsProject = conf.getFindbugsProject();

    assertThat(findbugsProject.getFileList()).containsOnly(file.getCanonicalPath());
    conf.stop();
  }

  @Test
  public void should_copy_lib_in_working_dir() throws IOException {
    String jsr305 = "findbugs/jsr305.jar";
    String annotations = "findbugs/annotations.jar";

    conf.copyLibs();
    assertThat(new File(mfs.workingDir(), jsr305)).isFile();
    assertThat(new File(mfs.workingDir(), annotations)).isFile();
    conf.stop();
    assertThat(new File(mfs.workingDir(), jsr305)).doesNotExist();
    assertThat(new File(mfs.workingDir(), annotations)).doesNotExist();
  }

}
