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
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.utils.Version;
import org.sonar.java.filters.SonarJavaIssueFilter;
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

    InputFile defaultFile = scanForErrorsInSonarLint(code);

    // No symbol table : check reference to foo is empty.
    assertThat(context.referencesForSymbolAt(defaultFile.key(), 3, 8)).isNull();
    // No metrics on lines
    verify(fileLinesContext, never()).save();
    // No highlighting
    assertThat(context.highlightingTypeAt(defaultFile.key(), 1, 0)).isEmpty();
    // No measures
    assertThat(context.measures(defaultFile.key())).isEmpty();

    verify(javaClasspath, times(2)).getElements();
    verify(javaTestClasspath, times(1)).getElements();
  }

  @Test
  public void metrics_and_highlighting_in_sonarqube() throws Exception {

    String code = "/***/\nclass A {\n String foo() {\n  return foo();\n }\n}";

    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.create(7, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
    InputFile defaultFile = scanForErrors(code, runtime);

    // Metrics on lines
    verify(fileLinesContext, times(1)).save();
    // Highlighting
    assertThat(context.highlightingTypeAt(defaultFile.key(), 1, 0)).isNotEmpty();
    // Measures
    assertThat(context.measures(defaultFile.key())).isNotEmpty();
  }

  @Test
  public void parsing_errors_should_be_reported_to_sonarlint() throws Exception {
    scanForErrorsInSonarLint("class A {");

    assertThat(context.allAnalysisErrors()).hasSize(1);
    assertThat(context.allAnalysisErrors().iterator().next().message()).startsWith("Parse error at line 1 column 8");
  }

  @org.junit.Ignore("new semantic analysis does not throw exception in this case")
  @Test
  public void semantic_errors_should_be_reported_to_sonarlint() throws Exception {
    scanForErrorsInSonarLint("class A {} class A {}");

    assertThat(context.allAnalysisErrors()).hasSize(1);
    assertThat(context.allAnalysisErrors().iterator().next().message()).isEqualTo("Registering class 2 times : A");
  }


  private InputFile scanForErrorsInSonarLint(String code) throws IOException {
    return scanForErrors(code, SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
  }

  private InputFile scanForErrors(String code, SonarRuntime runtime) throws IOException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    context = SensorContextTester.create(baseDir);
    context.setRuntime(runtime);

    InputFile inputFile = addFile(code, context);

    // Mock visitor for metrics.
    fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    javaClasspath = mock(JavaClasspath.class);
    javaTestClasspath = mock(JavaTestClasspath.class);
    sonarComponents = new SonarComponents(fileLinesContextFactory, context.fileSystem(), javaClasspath, javaTestClasspath, mock(CheckFactory.class));
    sonarComponents.setSensorContext(context);
    SonarJavaIssueFilter passThroughFilter = (issue, chain) -> chain.accept(issue);
    JavaSquid javaSquid = new JavaSquid(new JavaVersionImpl(), sonarComponents, new Measurer(context, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), passThroughFilter);
    javaSquid.scan(Collections.singletonList(inputFile), Collections.emptyList(), Collections.emptyList());
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
