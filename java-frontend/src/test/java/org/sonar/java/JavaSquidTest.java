/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.filters.SonarJavaIssueFilter;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
class JavaSquidTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private FileLinesContext fileLinesContext;
  private ClasspathForMain javaClasspath;
  private ClasspathForTest javaTestClasspath;
  private TestIssueFilter testIssueFilter;

  private SonarComponents sonarComponents;
  private SensorContextTester context;

  @Test
  void number_of_visitors_in_sonarLint_context_LTS() throws Exception {

    String code = "/***/\nclass A {\n String foo() {\n  return foo();\n }\n}";

    InputFile defaultFile = scan(code);

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
  void parsing_errors_should_be_reported_to_sonarlint() throws Exception {
    scan("class A {");

    assertThat(context.allAnalysisErrors()).hasSize(1);
    assertThat(context.allAnalysisErrors().iterator().next().message()).startsWith("Parse error at line 1 column 8");
  }

  @Test
  void should_add_issue_filter_to_JavaSquid_scanners() throws IOException {
    testIssueFilter = new TestIssueFilter();
    scan("class A { }");
    assertThat(context.allAnalysisErrors()).isEmpty();
    assertThat(testIssueFilter.lastScannedTree).isInstanceOf(CompilationUnitTree.class);
  }

  @org.junit.jupiter.api.Disabled("new semantic analysis does not throw exception in this case")
  @Test
  void semantic_errors_should_be_reported_to_sonarlint() throws Exception {
    scan("class A {} class A {}");

    assertThat(context.allAnalysisErrors()).hasSize(1);
    assertThat(context.allAnalysisErrors().iterator().next().message()).isEqualTo("Registering class 2 times : A");
  }

  @Test
  void scanning_empty_project_should_be_logged() throws Exception {
    JavaSquid javaSquid = new JavaSquid(new JavaVersionImpl(), sonarComponents, new Measurer(context, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), testIssueFilter);
    javaSquid.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    assertThat(logTester.logs(LoggerLevel.INFO)).containsExactly(
      "No \"Main\" source files to scan.",
      "No \"Test\" source files to scan.",
      "No \"Generated\" source files to scan."
    );
  }

  private InputFile scan(String code) throws IOException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    context = SensorContextTester.create(baseDir);

    // Set sonarLint runtime
    context.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));

    InputFile inputFile = addFile(code, context);

    // Mock visitor for metrics.
    fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    javaClasspath = mock(ClasspathForMain.class);
    javaTestClasspath = mock(ClasspathForTest.class);
    sonarComponents = new SonarComponents(fileLinesContextFactory, context.fileSystem(), javaClasspath, javaTestClasspath, mock(CheckFactory.class));
    sonarComponents.setSensorContext(context);
    JavaSquid javaSquid = new JavaSquid(new JavaVersionImpl(), sonarComponents, new Measurer(context, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), testIssueFilter);
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

  private static class TestIssueFilter implements JavaFileScanner, SonarJavaIssueFilter {
    CompilationUnitTree lastScannedTree = null;
    @Override
    public void scanFile(JavaFileScannerContext context) {
      lastScannedTree = context.getTree();
    }
    @Override
    public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
      return true;
    }
  }
}
