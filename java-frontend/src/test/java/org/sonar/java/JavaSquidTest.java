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
package org.sonar.java;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.utils.Version;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JavaSquidTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void number_of_visitors_in_sonarLint_context_LTS() throws Exception {
    testVersion(Version.create(5, 6));
  }
  @Test
  public void number_of_visitors_in_sonarLint_context_60() throws Exception {
    testVersion(Version.create(6, 0));
  }

  private void testVersion(Version version) throws IOException {
    SensorContextTester context = SensorContextTester.create(temp.getRoot());

    // set up a file to analyze
    File file = temp.newFile().getAbsoluteFile();
    Files.write("/***/\nclass A {\n String foo() {\n  return foo();\n }\n}", file, StandardCharsets.UTF_8);
    DefaultInputFile defaultFile = new TestInputFileBuilder("myProjectKey", file.getName())
      .setLanguage("java")
      .initMetadata(new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
      .setCharset(StandardCharsets.UTF_8)
      .build();
    context.fileSystem().add(defaultFile);

    // Set sonarLint runtime
    context.setRuntime(SonarRuntimeImpl.forSonarLint(version));

    // Mock visitor for metrics.
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    FileSystem fs = context.fileSystem();
    JavaClasspath javaClasspath = mock(JavaClasspath.class);
    JavaTestClasspath javaTestClasspath = mock(JavaTestClasspath.class);
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, mock(CheckFactory.class));
    sonarComponents.setSensorContext(context);
    JavaSquid javaSquid = new JavaSquid(new JavaVersionImpl(), sonarComponents, new Measurer(fs, context, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), null);
    javaSquid.scan(Lists.newArrayList(file), Lists.newArrayList());

    if(version.isGreaterThanOrEqual(Version.create(6, 0))) {
      // No symbol table : check reference to foo is empty.
      assertThat(context.referencesForSymbolAt(defaultFile.key(), 3, 8)).isNull();
      // No metrics on lines
      verify(fileLinesContext, never()).save();
      // No highlighting
      assertThat(context.highlightingTypeAt(defaultFile.key(), 1, 0)).isEmpty();
      // No measures
      assertThat(context.measures(defaultFile.key())).isEmpty();
    } else {
      assertThat(context.referencesForSymbolAt(defaultFile.key(), 3, 8)).hasSize(1);
      verify(fileLinesContext, times(1)).save();
      assertThat(context.highlightingTypeAt(defaultFile.key(), 1, 0)).hasSize(1).contains(TypeOfText.STRUCTURED_COMMENT);
      // No measures
      assertThat(context.measures(defaultFile.key())).isNotEmpty();
    }

    verify(javaClasspath, times(1)).getElements();
    verify(javaTestClasspath, times(1)).getElements();

  }
}
