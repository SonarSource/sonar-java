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
package org.sonar.java.ast;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisException;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.ExceptionHandler;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class JavaAstScannerTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private SensorContextTester context;

  @BeforeEach
  public void setUp() throws Exception {
    context = SensorContextTester.create(new File(""));
  }

  @Test
  void test_file_by_file_scan() {
    CollectorScanner collector = new CollectorScanner();
    scanTwoFilesWithVisitor(collector);

    assertThat(collector.fileNames).containsExactly("Classes.java", "Methods.java");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).doesNotContain("Using ECJ batch to parse source files.");
  }

  @Test
  void test_as_batch_scan() {
    enableBatchMode();

    CollectorScanner collector = spy(new CollectorScanner());
    scanTwoFilesWithVisitor(collector);

    assertThat(collector.fileNames).containsExactly("Classes.java", "Methods.java");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Using ECJ batch to parse source files.");
  }

  @Test
  void test_end_of_analysis_should_be_called_once() {
    EndOfAnalysisScanner endOfAnalysisScanner = spy(new EndOfAnalysisScanner());
    scanTwoFilesWithVisitor(endOfAnalysisScanner);

    verify(endOfAnalysisScanner, Mockito.times(2)).scanFile(any());
    verify(endOfAnalysisScanner, Mockito.times(1)).endOfAnalysis();
  }

  @Test
  void test_end_of_analysis_should_be_called_once_with_batch() {
    enableBatchMode();

    EndOfAnalysisScanner endOfAnalysisScanner = spy(new EndOfAnalysisScanner());
    scanTwoFilesWithVisitor(endOfAnalysisScanner);

    verify(endOfAnalysisScanner, Mockito.times(2)).scanFile(any());
    verify(endOfAnalysisScanner, Mockito.times(1)).endOfAnalysis();
  }

  @Test
  void comments() {
    InputFile inputFile = TestUtils.inputFile("src/test/files/metrics/Comments.java");
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    JavaAstScanner.scanSingleFileForTests(inputFile, new VisitorsBridge(new Measurer(context, noSonarFilter)));
    verify(noSonarFilter).noSonarInFile(inputFile, Collections.singleton(15));
  }

  @Test
  void noSonarLines() throws Exception {
    InputFile inputFile = TestUtils.inputFile("src/test/files/metrics/NoSonar.java");
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    JavaAstScanner.scanSingleFileForTests(inputFile, new VisitorsBridge(new Measurer(context, noSonarFilter)));
    verify(noSonarFilter).noSonarInFile(inputFile, Collections.singleton(8));
    //No Sonar on tests files
    NoSonarFilter noSonarFilterForTest = mock(NoSonarFilter.class);
    JavaAstScanner.scanSingleFileForTests(inputFile, new VisitorsBridge(new Measurer(context, noSonarFilterForTest).new TestFileMeasurer()));
    verify(noSonarFilterForTest).noSonarInFile(inputFile, Collections.singleton(8));
  }

  @Test
  void scan_single_file_with_dumb_file_should_not_fail() throws Exception {
    String filename = "!!dummy";
    JavaAstScanner.scanSingleFileForTests(TestUtils.emptyInputFile(filename), new VisitorsBridge(null));
  }

  @Test
  void scan_single_file_with_dumb_file_should_not_fail_when_not_fail_fast() {
    String filename = "!!dummy";
    scanSingleFile(TestUtils.emptyInputFile(filename), false);
  }

  @Test
  void scan_single_file_with_dumb_file_should_fail_when_fail_fast() throws Exception {
    String filename = "!!dummy";
    InputFile inputFile = TestUtils.emptyInputFile(filename);
    AnalysisException e = assertThrows(AnalysisException.class,
      () -> scanSingleFile(inputFile, true));
    assertThat(e.getMessage()).isEqualTo("Unable to analyze file : '!!dummy'");
  }

  @Test
  void should_not_fail_whole_analysis_upon_parse_error_and_notify_audit_listeners() {
    FakeAuditListener listener = spy(new FakeAuditListener());
    JavaAstScanner scanner = new JavaAstScanner(null);
    scanner.setVisitorBridge(new VisitorsBridge(listener));

    scanner.scan(Collections.singletonList(TestUtils.inputFile("src/test/resources/AstScannerParseError.txt")));
    verify(listener).processRecognitionException(any(RecognitionException.class));
  }

  @Test
  void should_handle_analysis_cancellation() {
    JavaFileScanner visitor = spy(new JavaFileScanner() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        JavaAstScannerTest.this.context.setCancelled(true);
      }
    });

    scanTwoFilesWithVisitor(visitor);

    verify(visitor, Mockito.times(1))
      .scanFile(any());
    verifyNoMoreInteractions(visitor);
  }

  @ParameterizedTest
  @ValueSource(classes = {
    InterruptedException.class,
    InterruptedIOException.class,
    CancellationException.class})
  void should_interrupt_analysis_when_specific_exception_are_thrown(Class<? extends Exception> exceptionClass) throws Exception {
    InputFile inputFile = TestUtils.inputFile("src/test/files/metrics/NoSonar.java");
    VisitorsBridge visitorsBridge = new VisitorsBridge(new CheckThrowingException(new RecognitionException(42, "interrupted", exceptionClass.newInstance())));

    AnalysisException e = assertThrows(AnalysisException.class, () -> JavaAstScanner.scanSingleFileForTests(inputFile, visitorsBridge));
    assertThat(e)
      .hasMessage("Analysis cancelled")
      .hasCauseInstanceOf(RecognitionException.class);
  }

  @Test
  void should_interrupt_analysis_when_is_cancelled() throws Exception {
    InputFile inputFile = TestUtils.inputFile("src/test/files/metrics/NoSonar.java");
    SonarComponents sonarComponent = new SonarComponents(null, context.fileSystem(), null, null, null);
    sonarComponent.setSensorContext(context);
    VisitorsBridge visitorsBridge = new VisitorsBridge(Collections.singletonList(new CheckCancellingAnalysis(context)),
      new ArrayList<>(),
      sonarComponent);

    final JavaVersionImpl javaVersion = new JavaVersionImpl();
    AnalysisException e = assertThrows(AnalysisException.class,
      () -> JavaAstScanner.scanSingleFileForTests(inputFile, visitorsBridge, javaVersion, sonarComponent));
    assertThat(e)
      .hasMessage("Analysis cancelled")
      .hasCauseInstanceOf(MyCancelException.class);
  }

  @Test
  void should_swallow_log_and_report_checks_exceptions() {
    JavaAstScanner scanner = new JavaAstScanner(null);
    SonarComponents sonarComponent = new SonarComponents(null, context.fileSystem(), null, null, null);
    sonarComponent.setSensorContext(context);
    scanner.setVisitorBridge(new VisitorsBridge(Collections.singleton(new CheckThrowingException(new NullPointerException("foo"))), new ArrayList<>(), sonarComponent));
    InputFile scannedFile = TestUtils.inputFile("src/test/resources/AstScannerNoParseError.txt");

    scanner.scan(Collections.singletonList(scannedFile));
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1).contains("Unable to run check class org.sonar.java.ast.JavaAstScannerTest$CheckThrowingException -  on file '"
      + scannedFile.toString()
      + "', To help improve the SonarSource Java Analyzer, please report this problem to SonarSource: see https://community.sonarsource.com/");
    logTester.clear();
    scanner.setVisitorBridge(new VisitorsBridge(new AnnotatedCheck(new NullPointerException("foo"))));
    scannedFile = TestUtils.inputFile("src/test/resources/AstScannerParseError.txt");
    scanner.scan(Collections.singletonList(scannedFile));
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(3).contains("Unable to run check class org.sonar.java.ast.JavaAstScannerTest$AnnotatedCheck - AnnotatedCheck on file '"
      + scannedFile.toString()
      + "', To help improve the SonarSource Java Analyzer, please report this problem to SonarSource: see https://community.sonarsource.com/");
  }

  @Test
  void should_propagate_SOError() {
    JavaAstScanner scanner = new JavaAstScanner(null);
    scanner.setVisitorBridge(new VisitorsBridge(new CheckThrowingSOError()));
    List<InputFile> files = Collections.singletonList(TestUtils.inputFile("src/test/resources/AstScannerNoParseError.txt"));
    try {
      scanner.scan(files);
      fail("Should have triggered a StackOverflowError and not reach this point.");
    } catch (Error e) {
      assertThat(e).isInstanceOf(StackOverflowError.class);
      assertThat(e.getMessage()).isEqualTo("boom");
      List<String> errorLogs = logTester.logs(LoggerLevel.ERROR);
      assertThat(errorLogs).hasSize(1);
      assertThat(errorLogs.get(0)).startsWith("A stack overflow error occurred while analyzing file");
    }
  }

  @Test
  void should_report_misconfigured_java_version() {
    VisitorsBridge visitorsBridge = new VisitorsBridge(new JavaFileScanner() {
      @Override
      public void scanFile(JavaFileScannerContext context) { /* do nothing */ }
    });
    visitorsBridge.setJavaVersion(new JavaVersionImpl(8));

    InputFile inputFile = TestUtils.inputFile("src/test/resources/module-info.java");
    List<InputFile> files = Arrays.asList(inputFile, inputFile);

    JavaAstScanner scanner = new JavaAstScanner(null);
    scanner.setVisitorBridge(visitorsBridge);
    scanner.scan(files);

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN))
      // two files, only one log
      .hasSize(1)
      // skipping start of logs which contains path, and depends of OS
      .allMatch(log -> log.endsWith("module-info.java' file with misconfigured Java version."
        + " Please check that property 'sonar.java.source' is correctly configured (currently set to: 8) or exclude 'module-info.java' files from analysis."
        + " Such files only exist in Java9+ projects."));
  }

  @Test
  void should_report_analysis_error_in_sonarLint_context_withSQ_6_0() {
    JavaAstScanner scanner = new JavaAstScanner(null);
    FakeAuditListener listener = spy(new FakeAuditListener());
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.reportAnalysisError(any(RecognitionException.class), any(InputFile.class))).thenReturn(true);
    scanner.setVisitorBridge(new VisitorsBridge(Collections.singletonList(listener), new ArrayList<>(), sonarComponents));
    scanner.scan(Collections.singletonList(TestUtils.inputFile("src/test/resources/AstScannerParseError.txt")));
    verify(sonarComponents).reportAnalysisError(any(RecognitionException.class), any(InputFile.class));
    verifyZeroInteractions(listener);
  }

  private final void scanSingleFile(InputFile file, boolean failOnException) {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    sensorContextTester.setSettings(new MapSettings().setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, failOnException));

    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContextTester);

    VisitorsBridge visitorsBridge = new VisitorsBridge(new ArrayList<>(), new ArrayList<>(), sonarComponents);

    JavaAstScanner.scanSingleFileForTests(file, visitorsBridge, new JavaVersionImpl(), sonarComponents);
  }

  private void enableBatchMode() {
    MapSettings settings = mock(MapSettings.class);
    when(settings.getString(SonarComponents.SONAR_BATCH_MODE_KEY)).thenReturn("true");
    context.setSettings(settings);
  }

  private void scanTwoFilesWithVisitor(JavaFileScanner visitor) {
    DefaultFileSystem fileSystem = context.fileSystem();
    ClasspathForMain classpathForMain = new ClasspathForMain(context.config(), fileSystem);
    ClasspathForTest classpathForTest = new ClasspathForTest(context.config(), fileSystem);
    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, classpathForMain, classpathForTest, null);
    sonarComponents.setSensorContext(context);
    JavaAstScanner scanner = new JavaAstScanner(sonarComponents);
    scanner.setVisitorBridge(new VisitorsBridge(Collections.singletonList(visitor), new ArrayList<>(), sonarComponents));
    scanner.scan(Arrays.asList(
      TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java")
    ));
  }

  private static class CheckThrowingSOError implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      throw new StackOverflowError("boom");
    }
  }
  private static class CheckThrowingException implements JavaFileScanner {

    private final RuntimeException exception;

    public CheckThrowingException(RuntimeException e) {
      this.exception = e;
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      throw exception;
    }
  }

  private static class CheckCancellingAnalysis implements JavaFileScanner {
    SensorContextTester sensorContext;

    public CheckCancellingAnalysis(SensorContextTester sensorContext) {
      this.sensorContext = sensorContext;
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      sensorContext.setCancelled(true);
      throw new MyCancelException();
    }
  }

  @org.sonar.check.Rule(key = "AnnotatedCheck")
  private static class AnnotatedCheck extends CheckThrowingException {
    public AnnotatedCheck(RuntimeException e) {
      super(e);
    }
  }

  private static class MyCancelException extends RuntimeException {
  }

  private static class FakeAuditListener implements JavaFileScanner, ExceptionHandler {

    @Override
    public void processRecognitionException(RecognitionException e) {
    }

    @Override
    public void processException(Exception e) {
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {

    }
  }

  private static class CollectorScanner implements JavaFileScanner {
    List<String> fileNames = new ArrayList<>();
    @Override
    public void scanFile(JavaFileScannerContext context) {
      fileNames.add(context.getInputFile().filename());
    }
  }

  private static class EndOfAnalysisScanner implements JavaFileScanner, EndOfAnalysisCheck {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      // Do nothing
    }

    @Override
    public void endOfAnalysis() {
      // Do nothing
    }
  }
}
