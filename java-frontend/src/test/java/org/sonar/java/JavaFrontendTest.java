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
import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
class JavaFrontendTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private FileLinesContext fileLinesContext;
  private ClasspathForMain javaClasspath;
  private ClasspathForTest javaTestClasspath;
  private TestIssueFilter mainCodeIssueScannerAndFilter = new TestIssueFilter();;
  private TestIssueFilter testCodeIssueScannerAndFilter = new TestIssueFilter();;

  private SonarComponents sonarComponents;
  private SensorContextTester sensorContext;

  @Test
  void number_of_visitors_in_sonarLint_context_LTS() throws Exception {

    String code = "/***/\nclass A {\n String foo() {\n  return foo();\n }\n}";

    InputFile defaultFile = scan(code).get(0);

    // No symbol table : check reference to foo is empty.
    assertThat(sensorContext.referencesForSymbolAt(defaultFile.key(), 3, 8)).isNull();
    // No metrics on lines
    verify(fileLinesContext, never()).save();
    // No highlighting
    assertThat(sensorContext.highlightingTypeAt(defaultFile.key(), 1, 0)).isEmpty();
    // No measures
    assertThat(sensorContext.measures(defaultFile.key())).isEmpty();

    verify(javaClasspath, times(2)).getElements();
    verify(javaTestClasspath, times(1)).getElements();
  }

  @Test
  void parsing_errors_should_be_reported_to_sonarlint() throws Exception {
    scan("class A {");

    assertThat(sensorContext.allAnalysisErrors()).hasSize(1);
    assertThat(sensorContext.allAnalysisErrors().iterator().next().message()).startsWith("Parse error at line 1 column 8");
  }

  @Test
  void should_add_issue_filter_to_JavaFrontend_scanners() throws IOException {
    scan("class A { }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    assertThat(mainCodeIssueScannerAndFilter.lastScannedTree).isInstanceOf(CompilationUnitTree.class);
  }

  @org.junit.jupiter.api.Disabled("new semantic analysis does not throw exception in this case")
  @Test
  void semantic_errors_should_be_reported_to_sonarlint() throws Exception {
    scan("class A {} class A {}");

    assertThat(sensorContext.allAnalysisErrors()).hasSize(1);
    assertThat(sensorContext.allAnalysisErrors().iterator().next().message()).isEqualTo("Registering class 2 times : A");
  }

  @Test
  void scanning_empty_project_should_be_logged() throws Exception {
    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(), sonarComponents, new Measurer(sensorContext, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), mainCodeIssueScannerAndFilter);
    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    assertThat(logTester.logs(LoggerLevel.INFO)).containsExactly(
      "No \"Main\" source files to scan.",
      "No \"Test\" source files to scan.",
      "No \"Generated\" source files to scan."
    );
  }

  @Test
  void test_file_by_file_scan() throws IOException {
    scan("class A {}", "class B { A a; }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .contains("Unresolved imports/types have been detected during analysis.")
      .contains("A cannot be resolved to a type");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(0);
  }

  @Test
  void test_as_batch_scan() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.internal.batchMode", "true");
    scan(settings, "class A {}", "class B { A a; }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .doesNotContain("Unresolved imports/types have been detected during analysis.")
      .doesNotContain("A cannot be resolved to a type");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(0);
  }

  @Test
  void test_as_batch_scan_main_and_test() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.internal.batchMode", "true");
    scan(settings, "class A {}", "class ATest { A a; }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .doesNotContain("Unresolved imports/types have been detected during analysis.")
      .doesNotContain("A cannot be resolved to a type");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(1);
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(1);
  }

  @Test
  void test_end_of_analysis_should_be_called_once() throws IOException {
    scan("class A {}", "class B {}");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @Test
  void test_end_of_analysis_should_be_called_once_with_batch() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.internal.batchMode", "true");
    scan(settings, "class A {}", "class B { A a; }");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @Test
  void should_handle_analysis_cancellation() throws IOException {
    mainCodeIssueScannerAndFilter.isCancelled = true;
    scan("class A {}", "class B { A a; }");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(1);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @Test
  void should_handle_analysis_cancellation_batch_mode() throws IOException {
    mainCodeIssueScannerAndFilter.isCancelled = true;
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.internal.batchMode", "true");
    assertThatThrownBy(() -> scan(settings, "class A {}", "class B { A a; }"))
      .isInstanceOf(AnalysisException.class)
      .hasMessage("Analysis cancelled")
      .hasCauseInstanceOf(OperationCanceledException.class);
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(1);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

 @Test
  void should_handle_compilation_error_in_batch_mode() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.internal.batchMode", "true");
    scan(settings, "class A {}", "class B {", "class C {}");
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs).contains("Unable to parse source file : 'B.java'");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @Test
  void exception_in_rules_should_not_interrupt_analysis_in_batch_mode() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.internal.batchMode", "true");
    mainCodeIssueScannerAndFilter.exceptionDuringScan = new RecognitionException(42, "interrupted", new NullPointerException());
    scan(settings, "class A {}", "class B {}", "class C {}");
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .contains("Using ECJ batch to parse source files.")
      .contains("Unable to run check");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(3);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  private List<InputFile> scan(String... codeList) throws IOException {
    return scan(new MapSettings(), codeList);
  }

  private List<InputFile> scan(MapSettings settings, String... codeList) throws IOException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    sensorContext = SensorContextTester.create(baseDir);
    sensorContext.setSettings(settings);

    // Set sonarLint runtime
    sensorContext.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));

    List<InputFile> inputFiles = new ArrayList<>();
    for (String code : codeList) {
      inputFiles.add(addFile(code, sensorContext));
    }

    // Mock visitor for metrics.
    fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    javaClasspath = mock(ClasspathForMain.class);
    javaTestClasspath = mock(ClasspathForTest.class);
    sonarComponents = new SonarComponents(fileLinesContextFactory, sensorContext.fileSystem(), javaClasspath, javaTestClasspath, mock(CheckFactory.class));
    sonarComponents.setSensorContext(sensorContext);
    sonarComponents.mainChecks().add(mainCodeIssueScannerAndFilter);
    sonarComponents.testChecks().add(testCodeIssueScannerAndFilter);
    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(), sonarComponents, new Measurer(sensorContext, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class),
      null, mainCodeIssueScannerAndFilter);
    frontend.scan(inputFiles, Collections.emptyList(), Collections.emptyList());

    return inputFiles;
  }

  private InputFile addFile(String code, SensorContextTester context) throws IOException {
    Matcher matcher = Pattern.compile("(?:^|\\s)(?:class|interface|enum|record)\\s++(\\w++)").matcher(code);
    if (!matcher.find()) {
      throw new IllegalStateException("Failed to extract filename from: " + code);
    }
    String className = matcher.group(1);
    InputFile.Type type = className.endsWith("Test") ? InputFile.Type.TEST : InputFile.Type.MAIN;
    File file = temp.newFile(className + ".java").getAbsoluteFile();
    Files.asCharSink(file, StandardCharsets.UTF_8).write(code);
    InputFile defaultFile = TestUtils.inputFile(context.fileSystem().baseDir().getAbsolutePath(), file, type);
    context.fileSystem().add(defaultFile);
    return defaultFile;
  }

  private class TestIssueFilter implements JavaFileScanner, SonarJavaIssueFilter, EndOfAnalysisCheck {
    CompilationUnitTree lastScannedTree = null;
    int scanFileInvocationCount = 0;
    int endOfAnalysisInvocationCount = 0;
    JavaFileScannerContext scannerContext;
    boolean isCancelled = false;
    RuntimeException exceptionDuringScan = null;

    @Override
    public void scanFile(JavaFileScannerContext scannerContext) {
      this.scannerContext = scannerContext;
      scanFileInvocationCount++;
      lastScannedTree = scannerContext.getTree();
      if (isCancelled) {
        sensorContext.setCancelled(true);
      }
      if (exceptionDuringScan != null) {
        RuntimeException ex = exceptionDuringScan;
        exceptionDuringScan = null;
        throw ex;
      }
    }

    @Override
    public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
      return true;
    }
    @Override
    public void endOfAnalysis() {
      endOfAnalysisInvocationCount++;
    }
  }
}
