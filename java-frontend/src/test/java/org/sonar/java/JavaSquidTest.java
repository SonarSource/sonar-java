/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.Version;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JavaSquidTest {

  private static final SonarRuntime RUNTIME = SonarRuntimeImpl.forSonarQube(Version.create(7, 3), SonarQubeSide.SCANNER);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void number_of_visitors_in_sonarLint_context_LTS() throws Exception {
    SensorContextTester context = SensorContextTester.create(temp.getRoot().getAbsoluteFile());

    String code = "/***/\nclass A {\n String foo() {\n  return foo();\n }\n}";
    DefaultInputFile defaultFile = addFile(code, context);

    // Set sonarLint runtime
    context.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));

    // Mock visitor for metrics.
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    FileSystem fs = context.fileSystem();
    JavaClasspath javaClasspath = mock(JavaClasspath.class);
    JavaTestClasspath javaTestClasspath = mock(JavaTestClasspath.class);
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, mock(CheckFactory.class), RUNTIME);
    sonarComponents.setSensorContext(context);
    JavaSquid javaSquid = new JavaSquid(new JavaVersionImpl(), sonarComponents, new Measurer(fs, context, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), null);
    javaSquid.scan(Collections.singletonList(defaultFile.file()), Collections.emptyList());

    // No symbol table : check reference to foo is empty.
    assertThat(context.referencesForSymbolAt(defaultFile.key(), 3, 8)).isNull();
    // No metrics on lines
    verify(fileLinesContext, never()).save();
    // No highlighting
    assertThat(context.highlightingTypeAt(defaultFile.key(), 1, 0)).isEmpty();
    // No measures
    assertThat(context.measures(defaultFile.key())).isEmpty();

    verify(javaClasspath, times(1)).getElements();
    verify(javaTestClasspath, times(1)).getElements();

  }

  @Test
  public void verify_analysis_errors_are_collected_on_parse_error() throws Exception {
    String code = "/***/\nclass A {\n String foo() {\n  return foo();\n }\n";
    SonarComponents sonarComponents = collectAnalysisErrors(code);
    assertThat(sonarComponents.analysisErrors).hasSize(1);
    AnalysisError analysisError = sonarComponents.analysisErrors.get(0);
    assertThat(analysisError.getMessage()).startsWith("Parse error at line 6 column 1:");
    assertThat(analysisError.getCause()).startsWith("com.sonar.sslr.api.RecognitionException: Parse error at line 6 column 1:");
    assertThat(analysisError.getFilename()).endsWith(".tmp");
    assertThat(analysisError.getKind()).isEqualTo(AnalysisError.Kind.PARSE_ERROR);
  }

  @Test
  public void verify_analysis_errors_are_collected_on_semantic_error() throws Exception {
    String code = "/***/\nclass A {\n String foo() {\n  return foo();\n }\n}\nclass A {}";
    SonarComponents sonarComponents = collectAnalysisErrors(code);
    assertThat(sonarComponents.analysisErrors).hasSize(1);
    AnalysisError analysisError = sonarComponents.analysisErrors.get(0);
    assertThat(analysisError.getMessage()).startsWith("Registering class 2 times : A");
    assertThat(analysisError.getCause()).startsWith("java.lang.IllegalStateException: Registering class 2 times : A");
    assertThat(analysisError.getFilename()).endsWith(".tmp");
    assertThat(analysisError.getKind()).isEqualTo(AnalysisError.Kind.SEMANTIC_ERROR);
  }

  private SonarComponents collectAnalysisErrors(String code) throws IOException {
    SensorContextTester context = SensorContextTester.create(temp.getRoot().getAbsoluteFile());
    DefaultInputFile defaultFile = addFile(code, context);

    context.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    // Mock visitor for metrics.
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    Server server = mock(Server.class);
    when(server.getPublicRootUrl()).thenReturn("https://sonarcloud.io");

    FileSystem fs = context.fileSystem();
    JavaClasspath javaClasspath = mock(JavaClasspath.class);
    JavaTestClasspath javaTestClasspath = mock(JavaTestClasspath.class);
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, mock(CheckFactory.class), RUNTIME);
    sonarComponents.setSensorContext(context);
    JavaSquid javaSquid = new JavaSquid(new JavaVersionImpl(), sonarComponents, new Measurer(fs, context, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), null);
    javaSquid.scan(Collections.singletonList(defaultFile.file()), Collections.emptyList());
    return sonarComponents;
  }

  @Test
  public void parsing_errors_should_be_reported_to_sonarlint() throws Exception {
    SensorContextTester context = setupAnalysisError("class A {");

    assertThat(context.allAnalysisErrors()).hasSize(1);
    assertThat(context.allAnalysisErrors().iterator().next().message()).startsWith("Parse error at line 1 column 10");
  }

  @Test
  public void semantic_errors_should_be_reported_to_sonarlint() throws Exception {
    SensorContextTester context = setupAnalysisError("class A {} class A {}");

    assertThat(context.allAnalysisErrors()).hasSize(1);
    assertThat(context.allAnalysisErrors().iterator().next().message()).isEqualTo("Registering class 2 times : A");
  }

  private SensorContextTester setupAnalysisError(String code) throws IOException {
    SensorContextTester context = SensorContextTester.create(temp.getRoot().getAbsoluteFile());

    DefaultInputFile inputFile = addFile(code, context);

    // Set sonarLint runtime
    context.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));

    // Mock visitor for metrics.
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    FileSystem fs = context.fileSystem();
    JavaClasspath javaClasspath = mock(JavaClasspath.class);
    JavaTestClasspath javaTestClasspath = mock(JavaTestClasspath.class);
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, fs, javaClasspath, javaTestClasspath, mock(CheckFactory.class), RUNTIME);
    sonarComponents.setSensorContext(context);
    JavaSquid javaSquid = new JavaSquid(new JavaVersionImpl(), sonarComponents, new Measurer(fs, context, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), null);
    javaSquid.scan(Collections.singletonList(inputFile.file()), Collections.emptyList());
    return context;
  }

  private DefaultInputFile addFile(String code, SensorContextTester context) throws IOException {
    File file = temp.newFile().getAbsoluteFile();
    Files.write(code, file, StandardCharsets.UTF_8);
    DefaultInputFile defaultFile = new TestInputFileBuilder(temp.getRoot().getAbsolutePath(), file.getName())
      .setLanguage("java")
      .initMetadata(new String(java.nio.file.Files.readAllBytes(file.getAbsoluteFile().toPath()), StandardCharsets.UTF_8))
      .setCharset(StandardCharsets.UTF_8)
      .build();
    context.fileSystem().add(defaultFile);
    return defaultFile;
  }
}
