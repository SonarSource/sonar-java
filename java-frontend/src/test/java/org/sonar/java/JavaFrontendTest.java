/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
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
import org.sonar.api.rule.RuleScope;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.Version;
import org.sonar.java.caching.CacheContextImpl;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.exceptions.ApiMismatchException;
import org.sonar.java.filters.SonarJavaIssueFilter;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
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
import static org.sonar.java.InputFileUtils.addFile;
import static org.sonar.java.TestUtils.mockSonarComponents;

@EnableRuleMigrationSupport
class JavaFrontendTest {


  // sonarlint.plugin.api.version
  public static final Version LATEST_SONARLINT_API_VERSION = Version.create(8, 18);
  public static final SonarRuntime SONARLINT_RUNTIME = SonarRuntimeImpl.forSonarLint(LATEST_SONARLINT_API_VERSION);

  //
  private static final Version LATESTS_SONAR_API_VERSION = Version.create(8, 13);
  public static final SonarRuntime SONARQUBE_RUNTIME = SonarRuntimeImpl.forSonarQube(LATESTS_SONAR_API_VERSION, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

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
    assertThat(sensorContext.allAnalysisErrors().iterator().next().message()).startsWith("Parse error at line 1 column 9");
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
  void scanning_empty_project_should_be_logged_in_file_by_file() {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.fileByFile", "true");
    scan(settings, SONARQUBE_RUNTIME, Collections.emptyList());

    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO))).containsExactly(
      "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
      "No \"Main\" source files to scan.",
      "No \"Test\" source files to scan.",
      "No \"Generated\" source files to scan."
    );
  }

  @Test
  void scanning_empty_project_should_be_logged_in_file_by_file_sonarlint() {
    scan(new MapSettings(), SONARLINT_RUNTIME, Collections.emptyList());

    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO))).containsExactly(
      "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
      "No \"Main\" source files to scan.",
      "No \"Test\" source files to scan.",
      "No \"Generated\" source files to scan."
    );
  }

  @Test
  void scanning_empty_project_should_be_logged_in_batch() {
    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(), mockSonarComponents(), new Measurer(sensorContext, mock(NoSonarFilter.class)), new NoOpTelemetry(), mock(JavaResourceLocator.class), mainCodeIssueScannerAndFilter);
    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO))).containsExactly(
      "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
      "No \"Main\" source files to scan.",
      "No \"Test\" source files to scan.",
      "No \"Generated\" source files to scan."
    );
  }

  @Test
  void scan_method_is_called_for_hook() throws IOException {
    class Hook implements CheckRegistrar, JavaFileScanner {
      int callCount;

      @Override
      public void register(RegistrarContext registrarContext) {
        registrarContext.registerCustomFileScanner(RuleScope.ALL, this);
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        callCount++;
      }
    }

    var settings = new MapSettings();
    if (sensorContext == null) {
      File baseDir = temp.getRoot().getAbsoluteFile();
      sensorContext = SensorContextTester.create(baseDir);
      sensorContext.setSettings(settings);
    }

    var hook = new Hook();
    var inputFiles = List.of(
      addFile(temp, "class A {}", sensorContext),
      addFile(temp, "class B {}", sensorContext)
    );
    scan(settings, SONARLINT_RUNTIME, inputFiles, new CheckRegistrar[]{hook});
    assertThat(hook.callCount).isEqualTo(2);
  }

  @Test
  void scanning_empty_project_should_be_logged_in_autoscan() {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.internal.analysis.autoscan", "true");
    scan(settings, SONARQUBE_RUNTIME, Collections.emptyList());

    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO))).containsExactly(
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
    var specificSonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(specificSonarComponents).context();
    doReturn(true).when(specificSonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      specificSonarComponents,
      mock(Measurer.class),
      new NoOpTelemetry(),
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO)))
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
    var specificSonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(specificSonarComponents).context();
    doReturn(false).when(specificSonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      specificSonarComponents,
      mock(Measurer.class),
      new NoOpTelemetry(),
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO)))
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
    var specificSonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(specificSonarComponents).context();
    doThrow(new ApiMismatchException(new NoSuchMethodError("BOOM!"))).when(specificSonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      specificSonarComponents,
      mock(Measurer.class),
      new NoOpTelemetry(),
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO)))
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

    var specificSonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(specificSonarComponents).context();
    doReturn(true).when(specificSonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      specificSonarComponents,
      mock(Measurer.class),
      new NoOpTelemetry(),
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO)))
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

    var specificSonarComponents = mock(SonarComponents.class);
    doReturn(spy).when(specificSonarComponents).context();
    doReturn(false).when(specificSonarComponents).canSkipUnchangedFiles();

    JavaFrontend frontend = new JavaFrontend(
      new JavaVersionImpl(),
      specificSonarComponents,
      mock(Measurer.class),
      new NoOpTelemetry(),
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO)))
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
      mockSonarComponents(),
      mock(Measurer.class),
      new NoOpTelemetry(),
      mock(JavaResourceLocator.class),
      mainCodeIssueScannerAndFilter
    );

    frontend.scan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    assertThat(filterOutAnalysisProgress(logTester.logs(Level.INFO)))
      .isNotEmpty()
      .containsExactly(
        "Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.",
        "No \"Main\" source files to scan.",
        "No \"Test\" source files to scan.",
        "No \"Generated\" source files to scan."
      );
  }

  private static List<String> filterOutAnalysisProgress(List<String> logs) {
    return logs.stream().filter(str -> !str.matches("\\d+% analyzed")).toList();
  }

  /**
   * Ensure that changes the mock, which may be needed when it is shared with other tests,
   * do not affect JavaFrontendTest.
   */
  @Test
  void test_validate_mockSonarComponents() {
    SonarComponents mock = mockSonarComponents();
    assertThat(mock.isAutoScan()).isFalse();
    assertThat(mock.isFileByFileEnabled()).isFalse();
    assertThat(mock.analysisCancelled()).isFalse();
    assertThat(mock.getBatchModeSizeInKB()).isEqualTo(-1L);
    assertThat(CacheContextImpl.of(mock).isCacheEnabled()).isFalse();
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
      .contains("2 source files to be analyzed");
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
  void should_handle_analysis_cancellation_batch_mode(String propertyName, String propertyValue) {
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
  void analysis_exception_should_interrupt_analysis_in_batch_mode(String propertyName, String propertyValue) {
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
  void exceptions_outside_rules_as_batch_should_be_logged(String propertyName, String propertyValue) {
    MapSettings settings = new MapSettings();
    settings.setProperty(propertyName, propertyValue);
    InputFile brokenFile = mock(InputFile.class);
    when(brokenFile.charset()).thenThrow(new NullPointerException());
    scan(settings, SONARQUBE_RUNTIME, Collections.singletonList(brokenFile));
    assertThat(logTester.logs(Level.ERROR)).
      containsExactly("Batch Mode failed, analysis of Java Files stopped.");
  }

  @ParameterizedTest
  @CsvSource({
    "sonar.java.experimental.batchModeSizeInKB,1000",
    "sonar.internal.analysis.autoscan,true"
  })
  void exceptions_outside_rules_as_batch_should_interrupt_analysis_if_fail_fast(String propertyName, String propertyValue) {
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

  /*@Test
  void test_preview_feature_in_max_supported_version_not_enabled_by_default() throws IOException {
    // When the actual version match the maximum supported version (currently 21), we do not enable the preview features flag
    // by default anymore, and we should expect issues parsing preview feature syntax
    logTester.setLevel(Level.DEBUG);
    scan(new MapSettings()
        .setProperty(JavaVersion.SOURCE_VERSION, "21")
        .setProperty(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled"),
      SONARLINT_RUNTIME, """
        void main() {
            System.out.println("Hello, World!");
        }
        """);
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs).contains("Use of preview features");
  }
*/

  @Test
  void test_sealed_classes_not_supported_with_java_16() throws IOException {
    logTester.setLevel(Level.DEBUG);
    scan(new MapSettings().setProperty(JavaVersion.SOURCE_VERSION, "16"),
      SONARLINT_RUNTIME, "sealed class Shape permits Circle { } final class Circle extends Shape { }");
    assertThat(sensorContext.allAnalysisErrors()).hasSize(1);
    assertThat(logTester.logs(Level.ERROR))
      .contains(
        "Unable to parse source file : 'Shape.java'",
        "Parse error at line 1 column 8: Syntax error on token \"class\", . expected");
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isZero();
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isZero();
  }

  @Test
  void test_sealed_classes_support_using_java_17() throws IOException {
    logTester.setLevel(Level.DEBUG);
    scan(new MapSettings().setProperty(JavaVersion.SOURCE_VERSION, "17"),
      SONARLINT_RUNTIME, "sealed class Shape permits Circle { } final class Circle extends Shape { }");
    assertThat(sensorContext.allAnalysisErrors()).isEmpty();
    assertTrue(logTester.logs(Level.WARN).stream().noneMatch(l -> l.endsWith("Unresolved imports/types have been detected during analysis. Enable DEBUG mode to see them.")));
    assertThat(mainCodeIssueScannerAndFilter.scanFileInvocationCount).isEqualTo(1);
    assertThat(testCodeIssueScannerAndFilter.scanFileInvocationCount).isZero();
  }

  @Test
  void test_java17_feature() throws IOException {
    logTester.setLevel(Level.DEBUG);
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
    logTester.setLevel(Level.DEBUG);
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
    logTester.setLevel(Level.DEBUG);
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
    logTester.setLevel(Level.DEBUG);
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
    logTester.setLevel(Level.DEBUG);
    scan(settings, SONARQUBE_RUNTIME, "class A {}", "class B extends A {}");
    String allLogs = String.join("\n", logTester.logs());
    assertThat(allLogs)
      .contains("Unresolved imports/types")
      .contains("Using ECJ batch to parse 2 Main java source files with batch size 0 KB.");
  }

  @Test
  void sonar_java_ignoreUnnamedModuleForSplitPackage_is_logged_at_debug_level_when_enabled() throws IOException {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.ignoreUnnamedModuleForSplitPackage", "false");
    scan(settings, SONARQUBE_RUNTIME, "package com.acme; class Anvil {}");
    assertThat(logTester.logs()).doesNotContain("The Java analyzer will ignore the unnamed module for split packages.");

    settings.setProperty("sonar.java.ignoreUnnamedModuleForSplitPackage", "true");
    scan(settings, SONARQUBE_RUNTIME, "package com.acme; class Dynamite {}");

    assertThat(logTester.logs()).contains("The Java analyzer will ignore the unnamed module for split packages.");
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
      inputFiles.add(addFile(temp, code, sensorContext));
    }
    return scan(settings, sonarRuntime, inputFiles);
  }

  private List<InputFile> scan(MapSettings settings, SonarRuntime sonarRuntime, List<InputFile> inputFiles) {
    return scan(settings, sonarRuntime, inputFiles, null);
  }

  private List<InputFile> scan(MapSettings settings, SonarRuntime sonarRuntime, List<InputFile> inputFiles, @Nullable CheckRegistrar[] checkRegistrars) {
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
    sonarComponents = new SonarComponents(fileLinesContextFactory, sensorContext.fileSystem(), javaClasspath, javaTestClasspath,
      mock(CheckFactory.class), mock(ActiveRules.class), checkRegistrars);
    sonarComponents.setSensorContext(sensorContext);
    sonarComponents.mainChecks().add(mainCodeIssueScannerAndFilter);
    sonarComponents.testChecks().add(testCodeIssueScannerAndFilter);
    JavaVersion javaVersion = settings.asConfig().get(JavaVersion.SOURCE_VERSION)
      .map(JavaVersionImpl::fromString)
      .orElse(new JavaVersionImpl());
    JavaFrontend frontend = new JavaFrontend(javaVersion,
      sonarComponents,
      new Measurer(sensorContext, mock(NoSonarFilter.class)),
      new NoOpTelemetry(),
      mock(JavaResourceLocator.class),
      null,
      sonarComponents.mainChecks().toArray(new JavaCheck[0]));
    frontend.scan(inputFiles, Collections.emptyList(), Collections.emptyList());

    return inputFiles;
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
        throw exceptionDuringScan;
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
