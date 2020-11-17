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
package org.sonar.java.ast;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisException;
import org.sonar.java.ExceptionHandler;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.se.SymbolicExecutionMode;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodTree;

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

@EnableRuleMigrationSupport
class JavaAstScannerTest {

  @Rule
  public LogTester logTester = new LogTester();
  private SensorContextTester context;

  @BeforeEach
  public void setUp() throws Exception {
    context = SensorContextTester.create(new File(""));
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
  void should_handle_analysis_cancellation() throws Exception {
    JavaFileScanner visitor = spy(new JavaFileScanner() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        JavaAstScannerTest.this.context.setCancelled(true);
      }
    });
    SonarComponents sonarComponents = new SonarComponents(null, context.fileSystem(), null, null, null);
    sonarComponents.setSensorContext(context);
    JavaAstScanner scanner = new JavaAstScanner(sonarComponents);
    scanner.setVisitorBridge(new VisitorsBridge(Lists.newArrayList(visitor), new ArrayList<>(), sonarComponents));
    scanner.scan(Arrays.asList(
      TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java")
    ));

    verify(visitor, Mockito.times(1))
      .scanFile(any());
    verifyNoMoreInteractions(visitor);
  }

  @Test
  void should_interrupt_analysis_when_InterruptedException_is_thrown() {
    InputFile inputFile = TestUtils.inputFile("src/test/files/metrics/NoSonar.java");
    VisitorsBridge visitorsBridge = new VisitorsBridge(new CheckThrowingException(new RecognitionException(42, "interrupted", new InterruptedException())));
    AnalysisException e = assertThrows(AnalysisException.class,
      () -> JavaAstScanner.scanSingleFileForTests(inputFile, visitorsBridge));
    assertThat(e.getMessage()).isEqualTo("Analysis cancelled");
    assertThat(e.getCause().getClass()).isEqualTo(RecognitionException.class);
  }

  @Test
  void should_interrupt_analysis_when_InterruptedIOException_is_thrown() {
    InputFile inputFile = TestUtils.inputFile("src/test/files/metrics/NoSonar.java");
    VisitorsBridge visitorsBridge = new VisitorsBridge(new CheckThrowingException(new RecognitionException(42, "interrupted", new InterruptedIOException())));
    AnalysisException e = assertThrows(AnalysisException.class,
      () -> JavaAstScanner.scanSingleFileForTests(inputFile, visitorsBridge));
    assertThat(e.getMessage()).isEqualTo("Analysis cancelled");
    assertThat(e.getCause().getClass()).isEqualTo(RecognitionException.class);
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
  void should_swallow_log_and_report_checks_exceptions_for_symbolic_execution() {
    JavaAstScanner scanner = new JavaAstScanner(null);
    logTester.clear();
    SonarComponents sonarComponent = new SonarComponents(null, context.fileSystem(), null, null, null);
    context.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    sonarComponent.setSensorContext(context);
    scanner.setVisitorBridge(new VisitorsBridge(Collections.singletonList(new SECheck() {
      @Override
      public void init(MethodTree methodTree, CFG cfg) {
        throw new NullPointerException("nobody expect the spanish inquisition !");
      }
    }), new ArrayList<>(), sonarComponent, SymbolicExecutionMode.ENABLED));
    scanner.scan(Collections.singletonList(TestUtils.inputFile("src/test/resources/se/MethodBehavior.java")));
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith("Unable to run check class org.sonar.java.se.SymbolicExecutionVisitor");
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
  void should_report_analysis_error_in_sonarLint_context_withSQ_6_0() {
    JavaAstScanner scanner = new JavaAstScanner(null);
    FakeAuditListener listener = spy(new FakeAuditListener());
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.reportAnalysisError(any(RecognitionException.class), any(InputFile.class))).thenReturn(true);
    scanner.setVisitorBridge(new VisitorsBridge(Lists.newArrayList(listener), new ArrayList<>(), sonarComponents));
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

  @org.sonar.check.Rule(key = "AnnotatedCheck")
  private static class AnnotatedCheck extends CheckThrowingException {
    public AnnotatedCheck(RuntimeException e) {
      super(e);
    }
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

}
