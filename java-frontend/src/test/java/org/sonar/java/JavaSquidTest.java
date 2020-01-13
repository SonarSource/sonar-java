/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
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

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private FileLinesContext fileLinesContext;
  private JavaClasspath javaClasspath;
  private JavaTestClasspath javaTestClasspath;

  private SonarComponents sonarComponents;
  private SensorContextTester context;

  @Test
  public void number_of_visitors_in_sonarLint_context_LTS() throws Exception {

    String code = "/***/\nclass A {\n String foo() {\n  return foo();\n }\n}";

    InputFile defaultFile = scanForErrors(code);

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
    scanForErrors(code);

    assertThat(sonarComponents.analysisErrors).hasSize(1);
    AnalysisError analysisError = sonarComponents.analysisErrors.get(0);
    assertThat(analysisError.getMessage()).startsWith("Parse error at line 5 column 1:");
    assertThat(analysisError.getCause()).startsWith("com.sonar.sslr.api.RecognitionException: Parse error at line 5 column 1:");
    assertThat(analysisError.getFilename()).endsWith("test.java");
    assertThat(analysisError.getKind()).isEqualTo(AnalysisError.Kind.PARSE_ERROR);
  }

  @org.junit.Ignore("new semantic analysis does not throw exception in this case")
  @Test
  public void verify_analysis_errors_are_collected_on_semantic_error() throws Exception {
    String code = "/***/\nclass A {\n String foo() {\n  return foo();\n }\n}\nclass A {}";
    scanForErrors(code);

    assertThat(sonarComponents.analysisErrors).hasSize(1);
    AnalysisError analysisError = sonarComponents.analysisErrors.get(0);
    assertThat(analysisError.getMessage()).startsWith("Registering class 2 times : A");
    assertThat(analysisError.getCause()).startsWith("java.lang.IllegalStateException: Registering class 2 times : A");
    assertThat(analysisError.getFilename()).endsWith("test.java");
    assertThat(analysisError.getKind()).isEqualTo(AnalysisError.Kind.SEMANTIC_ERROR);
  }

  @Test
  public void parsing_errors_should_be_reported_to_sonarlint() throws Exception {
    scanForErrors("class A {");

    assertThat(context.allAnalysisErrors()).hasSize(1);
    assertThat(context.allAnalysisErrors().iterator().next().message()).startsWith("Parse error at line 1 column 8");
  }

  @org.junit.Ignore("new semantic analysis does not throw exception in this case")
  @Test
  public void semantic_errors_should_be_reported_to_sonarlint() throws Exception {
    scanForErrors("class A {} class A {}");

    assertThat(context.allAnalysisErrors()).hasSize(1);
    assertThat(context.allAnalysisErrors().iterator().next().message()).isEqualTo("Registering class 2 times : A");
  }


  private InputFile scanForErrors(String code) throws IOException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    context = SensorContextTester.create(baseDir);

    // Set sonarLint runtime
    context.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));

    InputFile inputFile = addFile(code, context);

    // Mock visitor for metrics.
    fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    javaClasspath = mock(JavaClasspath.class);
    javaTestClasspath = mock(JavaTestClasspath.class);
    sonarComponents = new SonarComponents(fileLinesContextFactory, context.fileSystem(), javaClasspath, javaTestClasspath, mock(CheckFactory.class));
    sonarComponents.setSensorContext(context);
    JavaSquid javaSquid = new JavaSquid(new JavaVersionImpl(), sonarComponents, new Measurer(context, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), null);
    javaSquid.scan(Collections.singletonList(inputFile), Collections.emptyList());

    return inputFile;
  }

  private InputFile addFile(String code, SensorContextTester context) throws IOException {
    File file = temp.newFile("test.java").getAbsoluteFile();
    Files.asCharSink(file, StandardCharsets.UTF_8).write(code);
    InputFile defaultFile = TestUtils.inputFile(context.fileSystem().baseDir().getAbsolutePath(), file);
    context.fileSystem().add(defaultFile);
    return defaultFile;
  }
}
