/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.exceptions.ApiMismatchException;
import org.sonar.java.filters.SonarJavaIssueFilter;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
class JavaFrontendTest {

  public static final SonarRuntime SONARLINT_RUNTIME = SonarRuntimeImpl.forSonarLint(Version.create(6, 7));
  public static final SonarRuntime SONARQUBE_RUNTIME = SonarRuntimeImpl.forSonarQube(Version.create(8, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private FileLinesContext fileLinesContext;
  private ClasspathForMain javaClasspath;
  private ClasspathForTest javaTestClasspath;
  private TestIssueFilter mainCodeIssueScannerAndFilter = new TestIssueFilter();
  private TestIssueFilter testCodeIssueScannerAndFilter = new TestIssueFilter();

  private SonarComponents sonarComponents;
  private SensorContextTester sensorContext;

  @Test
  void number_of_visitors_in_sonarLint_context_LTS() throws Exception {

    String code = "/***/\nclass A {\n String foo() {\n  return foo();\n }\n}";

    InputFile defaultFile = scan(SONARLINT_RUNTIME, code).get(0);

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
    scan(SONARLINT_RUNTIME, "class A {");

    assertThat(sensorContext.allAnalysisErrors()).hasSize(1);
    assertThat(sensorContext.allAnalysisErrors().iterator().next().message()).startsWith("Parse error at line 1 column 8");
  }

  @Test
  void should_add_issue_filter_to_JavaFrontend_scanners() throws IOException {
    scan(SONARQUBE_RUNTIME, "class A { }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    assertThat(mainCodeIssueScannerAndFilter.lastScannedTree).isInstanceOf(CompilationUnitTree.class);
  }

  @org.junit.jupiter.api.Disabled("new semantic analysis does not throw exception in this case")
  @Test
  void semantic_errors_should_be_reported_to_sonarlint() throws Exception {
    scan(SONARLINT_RUNTIME, "class A {} class A {}");

    assertThat(sensorContext.allAnalysisErrors()).hasSize(1);
    assertThat(sensorContext.allAnalysisErrors().iterator().next().message()).isEqualTo("Registering class 2 times : A");
  }

  @Test
  void scanning_empty_project_should_be_logged_in_file_by_file() throws Exception {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.fileByFile", "true");
    scan(settings, SONARQUBE_RUNTIME, Collections.emptyList());

    assertThat(logTester.logs(LoggerLevel.INFO)).containsExactly(
      "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
      "No \"Main\" source files to scan.",
      "No \"Test\" source files to scan.",
      "No \"Generated\" source files to scan."
    );
  }

  @Test
  void scanning_empty_project_should_be_logged_in_file_by_file_sonarlint() throws Exception {
    scan(new MapSettings(), SONARLINT_RUNTIME, Collections.emptyList());

    assertThat(logTester.logs(LoggerLevel.INFO)).containsExactly(
      "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
      "No \"Main\" source files to scan.",
      "No \"Test\" source files to scan.",
      "No \"Generated\" source files to scan."
    );
  }

  @Test
  void scanning_empty_project_should_be_logged_in_batch() throws Exception {
    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(), sonarComponents, new Measurer(sensorContext, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), mainCodeIssueScannerAndFilter);
    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    assertThat(logTester.logs(LoggerLevel.INFO)).containsExactly(
      "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
      "No \"Main\" source files to scan.",
      "No \"Test\" source files to scan.",
      "No \"Generated\" source files to scan."
    );
  }

  @Test
  void scanning_empty_project_should_be_logged_in_autoscan() throws Exception {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.internal.analysis.autoscan", "true");
    scan(settings, SONARQUBE_RUNTIME, Collections.emptyList());

    assertThat(logTester.logs(LoggerLevel.INFO)).containsExactly(
      "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
      "No \"Main and Test\" source files to scan."
    );
  }

  @Test
  void test_scan_logs_when_caching_is_enabled_and_can_skip_unchanged_files() throws ApiMismatchException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    SensorContextTester sensorContextTester = SensorContextTester.create(baseDir);
    sensorContextTester.setSettings(new MapSettings());

    SensorContext spy = spy(sensorContextTester);
    doReturn(true).when(spy).isCacheEnabled();
    doReturn(mock(ReadCache.class)).when(spy).previousCache();
    doReturn(mock(WriteCache.class)).when(spy).nextCache();
    var sonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(sonarComponents).context();
    doReturn(true).when(sonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      sonarComponents,
      null,
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    List<String> logs = logTester.getLogs(LoggerLevel.INFO).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.toList());
    assertThat(logs)
      .isNotEmpty()
      .containsExactly(
        "Server-side caching is enabled. The Java analyzer was able to leverage cached data from previous analyses for 0 out of 0 files. These files will not be parsed.",
        "No \"Main\" source files to scan.",
        "No \"Test\" source files to scan.",
        "No \"Generated\" source files to scan."
      );
  }

  @Test
  void test_scan_logs_when_caching_is_enabled_and_cannot_skip_unchanged_files() throws ApiMismatchException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    SensorContextTester sensorContextTester = SensorContextTester.create(baseDir);
    sensorContextTester.setSettings(new MapSettings());

    SensorContext spy = spy(sensorContextTester);
    doReturn(true).when(spy).isCacheEnabled();
    doReturn(mock(ReadCache.class)).when(spy).previousCache();
    doReturn(mock(WriteCache.class)).when(spy).nextCache();
    var sonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(sonarComponents).context();
    doReturn(false).when(sonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      sonarComponents,
      null,
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    List<String> logs = logTester.getLogs(LoggerLevel.INFO).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.toList());
    assertThat(logs)
      .isNotEmpty()
      .containsExactly(
        "Server-side caching is enabled. The Java analyzer will not try to leverage data from a previous analysis.",
        "No \"Main\" source files to scan.",
        "No \"Test\" source files to scan.",
        "No \"Generated\" source files to scan."
      );
  }

  @Test
  void test_scan_logs_when_caching_is_enabled_and_cannot_determine_if_unchanged_files_can_be_skipped() throws ApiMismatchException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    SensorContextTester sensorContextTester = SensorContextTester.create(baseDir);
    sensorContextTester.setSettings(new MapSettings());

    SensorContext spy = spy(sensorContextTester);
    doReturn(true).when(spy).isCacheEnabled();
    doReturn(mock(ReadCache.class)).when(spy).previousCache();
    doReturn(mock(WriteCache.class)).when(spy).nextCache();
    var sonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(sonarComponents).context();
    doThrow(new ApiMismatchException(new NoSuchMethodError("BOOM!"))).when(sonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      sonarComponents,
      null,
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    List<String> logs = logTester.getLogs(LoggerLevel.INFO).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.toList());
    assertThat(logs)
      .isNotEmpty()
      .containsExactly(
        "Server-side caching is enabled. The Java analyzer will not try to leverage data from a previous analysis.",
        "No \"Main\" source files to scan.",
        "No \"Test\" source files to scan.",
        "No \"Generated\" source files to scan."
      );
  }

  @Test
  void test_scan_logs_when_caching_is_disabled_and_can_skip_unchanged_files() throws ApiMismatchException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    SensorContextTester sensorContextTester = SensorContextTester.create(baseDir);
    sensorContextTester.setSettings(new MapSettings());

    SensorContext spy = spy(sensorContextTester);
    doReturn(false).when(spy).isCacheEnabled();

    var sonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(sonarComponents).context();
    doReturn(true).when(sonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      sonarComponents,
      null,
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    List<String> logs = logTester.getLogs(LoggerLevel.INFO).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.toList());
    assertThat(logs)
      .isNotEmpty()
      .containsExactly(
        "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
        "No \"Main\" source files to scan.",
        "No \"Test\" source files to scan.",
        "No \"Generated\" source files to scan."
      );
  }

  @Test
  void test_scan_logs_when_caching_is_disabled_and_cannot_skip_unchanged_files() throws ApiMismatchException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    SensorContextTester sensorContextTester = SensorContextTester.create(baseDir);
    sensorContextTester.setSettings(new MapSettings());

    SensorContext spy = spy(sensorContextTester);
    doReturn(false).when(spy).isCacheEnabled();

    var sonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(sonarComponents).context();
    doReturn(false).when(sonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      sonarComponents,
      null,
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    List<String> logs = logTester.getLogs(LoggerLevel.INFO).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.toList());
    assertThat(logs)
      .isNotEmpty()
      .containsExactly(
        "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
        "No \"Main\" source files to scan.",
        "No \"Test\" source files to scan.",
        "No \"Generated\" source files to scan."
      );
  }

  @Test
  void test_scan_logs_when_caching_is_disabled_when_sonar_components_is_null() {
    File baseDir = temp.getRoot().getAbsoluteFile();
    SensorContextTester sensorContextTester = SensorContextTester.create(baseDir);
    sensorContextTester.setSettings(new MapSettings());

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      null,
      null,
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    List<String> logs = logTester.getLogs(LoggerLevel.INFO).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.toList());
    assertThat(logs)
      .isNotEmpty()
      .containsExactly(
        "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
        "No \"Main\" source files to scan.",
        "No \"Test\" source files to scan.",
        "No \"Generated\" source files to scan."
      );
  }

  @Test
  void test_getters_with_null_sonarComponents() {
    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(), null, new Measurer(sensorContext, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class), mainCodeIssueScannerAndFilter);
    assertThat(frontend.isAutoScan()).isFalse();
    assertThat(frontend.isFileByFileEnabled()).isFalse();
    assertThat(frontend.analysisCancelled()).isFalse();
    assertThat(frontend.getBatchModeSizeInKB()).isEqualTo(-1L);
  }

  @Test
  void test_file_by_file_scan() throws IOException {
    scan(SONARLINT_RUNTIME, "class A {}", "class B { A a; }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .contains("Unresolved imports/types have been detected during analysis.")
      .contains("A cannot be resolved to a type");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isZero();
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.java.experimental.batchModeSizeInKB,1000",
    "sonar.internal.analysis.autoscan,true"
  })
  void test_no_batch_mode_when_sonarlint(String propertyName, String propertyValue) throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty(propertyName, propertyValue);
    scan(settings, SONARLINT_RUNTIME, "class A {}", "class B { A a; }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .doesNotContain("Using ECJ batch to parse source files.")
      .contains("Java \"Main\" source files AST scan");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isZero();
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.java.experimental.batchModeSizeInKB,1000",
    "sonar.internal.analysis.autoscan,true",
    // alias of sonar.internal.analysis.autoscan that will be removed
    "sonar.java.internal.batchMode,true"
  })
  void test_as_batch_scan(String propertyName, String propertyValue) throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty(propertyName, propertyValue);
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B { A a; }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .doesNotContain("Unresolved imports/types have been detected during analysis.")
      .doesNotContain("A cannot be resolved to a type");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isZero();
  }

  @Test
  void test_as_autoscan_main_and_test() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.internal.analysis.autoscan", "true");
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class ATest { A a; }");
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
    scan(SONARLINT_RUNTIME, "class A {}", "class B {}");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.java.experimental.batchModeSizeInKB,1000",
    "sonar.internal.analysis.autoscan,true"
  })
  void test_end_of_analysis_should_be_called_once_with_batch(String propertyName, String propertyValue) throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty(propertyName, propertyValue);
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B { A a; }");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @Test
  void test_end_of_analysis_should_be_called_once_with_several_batches() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty(SonarComponents.SONAR_BATCH_SIZE_KEY, 0L);
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B { A a; }");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(2);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @Test
  void should_handle_analysis_cancellation() throws IOException {
    mainCodeIssueScannerAndFilter.isCancelled = true;
    scan(SONARLINT_RUNTIME, "class A {}", "class B { A a; }");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(1);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.java.experimental.batchModeSizeInKB,1000",
    "sonar.internal.analysis.autoscan,true"
  })
  void should_handle_analysis_cancellation_batch_mode(String propertyName, String propertyValue) throws IOException {
    mainCodeIssueScannerAndFilter.isCancelled = true;
    MapSettings settings = new MapSettings();
    settings.setProperty(propertyName, propertyValue);
    assertThatThrownBy(() -> scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B { A a; }"))
      .isInstanceOf(AnalysisException.class)
      .hasMessage("Analysis cancelled")
      .hasCauseInstanceOf(OperationCanceledException.class);
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(1);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.java.experimental.batchModeSizeInKB,1000",
    "sonar.internal.analysis.autoscan,true"
  })
  void should_handle_compilation_error_in_batch_mode(String propertyName, String propertyValue) throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty(propertyName, propertyValue);
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B {", "class C {}");
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs).contains("Unable to parse source file : 'B.java'");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(3);
    assertThat(mainCodeIssueScannerAndFilter.endOfAnalysisInvocationCount).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.java.experimental.batchModeSizeInKB,1000",
    "sonar.internal.analysis.autoscan,true"
  })
  void analysis_exception_should_interrupt_analysis_in_batch_mode(String propertyName, String propertyValue) throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty(propertyName, propertyValue);
    mainCodeIssueScannerAndFilter.exceptionDuringScan = new IllegalRuleParameterException("Test AnalysisException", new NullPointerException());
    assertThatThrownBy(() -> scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B {}", "class C {}"))
      .isInstanceOf(AnalysisException.class)
      .hasMessage("Bad configuration of rule parameter");
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.java.experimental.batchModeSizeInKB,1000",
    "sonar.internal.analysis.autoscan,true"
  })
  void exceptions_outside_rules_as_batch_should_be_logged(String propertyName, String propertyValue) throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty(propertyName, propertyValue);
    InputFile brokenFile = mock(InputFile.class);
    when(brokenFile.charset()).thenThrow(new NullPointerException());
    scan(settings, SONARQUBE_RUNTIME, Collections.singletonList(brokenFile));
    assertThat(logTester.logs(LoggerLevel.ERROR)).
      containsExactly("Batch Mode failed, analysis of Java Files stopped.");
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.java.experimental.batchModeSizeInKB,1000",
    "sonar.internal.analysis.autoscan,true"
  })
  void exceptions_outside_rules_as_batch_should_interrupt_analysis_if_fail_fast(String propertyName, String propertyValue) throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty(propertyName, propertyValue);
    settings.setProperty("sonar.internal.analysis.failFast", "true");
    InputFile brokenFile = mock(InputFile.class);
    when(brokenFile.charset()).thenThrow(new NullPointerException());
    List<InputFile> inputFiles = Collections.singletonList(brokenFile);
    assertThatThrownBy(() -> scan(settings, SONARQUBE_RUNTIME, inputFiles))
      .isInstanceOf(AnalysisException.class)
      .hasMessage("Batch Mode failed, analysis of Java Files stopped.");
  }

  @Test
  void test_preview_feature_in_max_supported_version_do_not_log_message() throws IOException {
    // When the actual version match the maximum supported version (currently 17), the preview features flag is
    // enabled in the parser config and we made sure to be able to parse preview features, no need to log anything.
    logTester.setLevel(LoggerLevel.DEBUG);
    scan(new MapSettings().setProperty(JavaVersion.SOURCE_VERSION, "17"),
      SONARLINT_RUNTIME, "class A { void m(String s) { switch(s) { case null: default: } } }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs).doesNotContain("Unresolved imports/types", "Use of preview features");
  }

  @Test
  void test_preview_feature_in_version_greater_than_maximum_do_not_log_message() throws IOException {
    // It can happen that a new version of Java is used, that we do not officially support. In this case
    // we still enable the flag to optimize our chances to correctly parse the code.
    logTester.setLevel(LoggerLevel.DEBUG);
    scan(new MapSettings().setProperty(JavaVersion.SOURCE_VERSION, "42"),
      SONARLINT_RUNTIME, "class A { void m(String s) { switch(s) { case null: default: } } }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs).doesNotContain("Unresolved imports/types", "Use of preview features");
  }

  @Test
  void test_sealed_classes_in_java_16_log_message() throws IOException {
    // When the actual version is lower than the maximum supported version (currently 17),
    // we can not guarantee that we are still parsing preview features the same way (it may have evolved) and log a message.
    logTester.setLevel(LoggerLevel.DEBUG);
    scan(new MapSettings().setProperty(JavaVersion.SOURCE_VERSION, "16"),
      SONARLINT_RUNTIME, "sealed class Shape permits Circle { } final class Circle extends Shape { }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    assertTrue(logTester.logs(LoggerLevel.WARN).stream().noneMatch(l -> l.endsWith("Unresolved imports/types have been detected during analysis. Enable DEBUG mode to see them.")));
    assertTrue(logTester.logs(LoggerLevel.WARN).stream().anyMatch(l -> l.endsWith("Use of preview features have been detected during analysis. Enable DEBUG mode to see them.")));
    // We should keep this message or we won't have anything actionable in the debug logs to understand the warning
    assertTrue(logTester.logs(LoggerLevel.DEBUG).stream().anyMatch(l -> l.replace("\r\n", "\n").endsWith("Use of preview features:\n" +
      "- The Java feature 'Sealed Types' is only available with source level 17 and above")));
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(1);
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isZero();
  }

  @Test
  void test_java17_feature() throws IOException {
    logTester.setLevel(LoggerLevel.DEBUG);
    scan(new MapSettings().setProperty(JavaVersion.SOURCE_VERSION, "17"),
      SONARLINT_RUNTIME, "sealed class Shape permits Circle { } final class Circle extends Shape { }");
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs).doesNotContain("Unresolved imports/types", "Use of preview features");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(1);
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isZero();
  }

  @Test
  void test_scan_as_autoscan_uses_a_single_batch() throws IOException {
    MapSettings settings = new MapSettings().setProperty(SonarComponents.SONAR_AUTOSCAN, true);
    logTester.setLevel(LoggerLevel.DEBUG);
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B extends A {}");
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .doesNotContain("Unresolved imports/types")
      .contains("Using ECJ batch to parse 2 Main and Test java source files in a single batch.");
  }

  @Test
  void test_scan_as_batch_uses_configured_batch_size_when_below_threshold() throws IOException {
    MapSettings settings = new MapSettings()
      .setProperty(SonarComponents.SONAR_BATCH_SIZE_KEY, 1);
    logTester.setLevel(LoggerLevel.DEBUG);
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B extends A {}");
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .doesNotContain("Unresolved imports/types")
      .containsOnlyOnce("Using ECJ batch to parse 2 Main java source files with batch size 1 KB.");
  }

  @Test
  void test_scan_in_a_single_batch_when_batch_size_overflows() throws IOException {
    long overTheTopBatchSize = 9_223_372_036_855_038L;
    MapSettings settings = new MapSettings()
      .setProperty(SonarComponents.SONAR_BATCH_SIZE_KEY, overTheTopBatchSize);
    logTester.setLevel(LoggerLevel.DEBUG);
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B extends A {}");
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .doesNotContain("Unresolved imports/types")
      .doesNotContainPattern("Scanning with batch size .*")
      .contains("Using ECJ batch to parse 2 Main java source files in a single batch.");
  }

  @Test
  void test_scan_as_batch_effectively_splits_scans_in_batches() throws IOException {
    MapSettings settings = new MapSettings()
      .setProperty(SonarComponents.SONAR_BATCH_SIZE_KEY, 0);
    logTester.setLevel(LoggerLevel.DEBUG);
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B extends A {}");
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .contains("Unresolved imports/types")
      .contains("Using ECJ batch to parse 2 Main java source files with batch size 0 KB.");
  }

  @Test
  void batch_generator_returns_an_empty_list_when_no_input_files() throws IOException {
    List<InputFile> emptyList = Collections.emptyList();
    JavaFrontend.BatchGenerator generator = new JavaFrontend.BatchGenerator(emptyList.iterator(), 0);
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();
  }

  @Test
  void batch_generator_returns_at_most_one_item_per_batch_when_size_is_zero() throws IOException {
    if (sensorContext == null) {
      File baseDir = temp.getRoot().getAbsoluteFile();
      sensorContext = SensorContextTester.create(baseDir);
      sensorContext.setSettings(new MapSettings());
    }
    List<InputFile> inputFiles = new ArrayList<>();
    inputFiles.add(addFile("class A {}", sensorContext));
    inputFiles.add(addFile("class B extends A {}", sensorContext));
    JavaFrontend.BatchGenerator generator = new JavaFrontend.BatchGenerator(inputFiles.iterator(), 0);
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next())
      .hasSize(1)
      .contains(inputFiles.get(0));
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next())
      .hasSize(1)
      .contains(inputFiles.get(1));
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();
  }

  @Test
  void batch_generator_returns_batches_with_multiple_files_that_are_smaller_than_batch_size() throws IOException {
    if (sensorContext == null) {
      File baseDir = temp.getRoot().getAbsoluteFile();
      sensorContext = SensorContextTester.create(baseDir);
      sensorContext.setSettings(new MapSettings());
    }
    InputFile A = addFile("class A { public void doSomething() {} }", sensorContext);
    InputFile B = addFile("class B extends A {}", sensorContext);
    InputFile C = addFile("class C {}", sensorContext);

    long sizeofA = A.file().length() + 1;
    JavaFrontend.BatchGenerator generator = new JavaFrontend.BatchGenerator(
      Arrays.asList(A, B, C).iterator(), sizeofA
    );
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(A);
    assertThat(generator.hasNext()).isTrue();
    List<InputFile> batchWithMultipleFiles = generator.next();
    assertThat(batchWithMultipleFiles).hasSize(2).contains(B).contains(C);
    long batchSize = batchWithMultipleFiles.stream().map(i -> i.file().length()).reduce(0L, Long::sum);
    assertThat(batchSize).isLessThanOrEqualTo(sizeofA);
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();

    long sizeOfAPlusB = A.file().length() + B.file().length();
    generator = new JavaFrontend.BatchGenerator(
      Arrays.asList(A, B, C).iterator(), sizeOfAPlusB
    );
    assertThat(generator.hasNext()).isTrue();
    batchWithMultipleFiles = generator.next();
    assertThat(batchWithMultipleFiles).hasSize(2).contains(A).contains(B);
    batchSize = batchWithMultipleFiles.stream().map(i -> i.file().length()).reduce(0L, Long::sum);
    assertThat(batchSize).isLessThanOrEqualTo(sizeOfAPlusB);
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(C);
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();
  }

  @Test
  void batch_generator_includes_file_excluded_from_previous_batch_into_next_batch() throws IOException {
    if (sensorContext == null) {
      File baseDir = temp.getRoot().getAbsoluteFile();
      sensorContext = SensorContextTester.create(baseDir);
      sensorContext.setSettings(new MapSettings());
    }
    InputFile A = addFile("class A { public void doSomething() {} }", sensorContext);
    InputFile B = addFile("class B extends A {}", sensorContext);
    InputFile C = addFile("class C {}", sensorContext);
    JavaFrontend.BatchGenerator generator = new JavaFrontend.BatchGenerator(
      Arrays.asList(A, C, B).iterator(), C.file().length()
    );
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(A);
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(C);
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(B);
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();
  }

  private List<InputFile> scan(SonarRuntime sonarRuntime, String... codeList) throws IOException {
    return scan(new MapSettings(), sonarRuntime, codeList);
  }

  private List<InputFile> scan(MapSettings settings, SonarRuntime sonarRuntime, String... codeList) throws IOException {
    if (sensorContext == null) {
      File baseDir = temp.getRoot().getAbsoluteFile();
      sensorContext = SensorContextTester.create(baseDir);
      sensorContext.setSettings(settings);
    }
    List<InputFile> inputFiles = new ArrayList<>();
    for (String code : codeList) {
      inputFiles.add(addFile(code, sensorContext));
    }
    return scan(settings, sonarRuntime, inputFiles);
  }

  private List<InputFile> scan(MapSettings settings, SonarRuntime sonarRuntime, List<InputFile> inputFiles) throws IOException {
    if (sensorContext == null) {
      File baseDir = temp.getRoot().getAbsoluteFile();
      sensorContext = SensorContextTester.create(baseDir);
      sensorContext.setSettings(settings);
    }
    sensorContext.setRuntime(sonarRuntime);

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
    JavaVersion javaVersion = settings.asConfig().get(JavaVersion.SOURCE_VERSION)
      .map(JavaVersionImpl::fromString)
      .orElse(new JavaVersionImpl());
    JavaFrontend frontend = new JavaFrontend(javaVersion, sonarComponents, new Measurer(sensorContext, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class),
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

  private class TestIssueFilter implements JavaFileScanner, SonarJavaIssueFilter, EndOfAnalysis {
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
    public void endOfAnalysis(ModuleScannerContext context) {
      endOfAnalysisInvocationCount++;
    }
  }
}
