/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.model;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.AnalysisException;
import org.sonar.java.CheckFailureException;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.checks.EndOfAnalysisVisitor;
import org.sonar.java.checks.VisitorThatCanBeSkipped;
import org.sonar.java.exceptions.ApiMismatchException;
import org.sonar.java.notchecks.VisitorNotInChecksPackage;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class VisitorsBridgeTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private SonarComponents sonarComponents = null;

  private static final File FILE = new File("src/test/files/model/SimpleClass.java");
  private static final InputFile INPUT_FILE = TestUtils.inputFile(FILE);
  private static final CompilationUnitTree COMPILATION_UNIT_TREE = JParserTestUtils.parse(FILE);

  private static final NullPointerException NPE = new NullPointerException("BimBadaboum");

  @Test
  @Disabled("Unable to reproduce since ECJ migration")
  void test_semantic_exclusions() {
    VisitorsBridge visitorsBridgeWithoutSemantic = new VisitorsBridge(Collections.singletonList((JavaFileScanner) context -> {
      assertThat(context.getSemanticModel()).isNull();
      assertThat(context.fileParsed()).isTrue();
    }), new ArrayList<>(), null);
    checkFile(constructFileName("java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(constructFileName("src", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(constructFileName("home", "user", "oracleSdk", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(constructFileName("java", "io", "Serializable.java"), "package java.io; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(constructFileName("java", "lang", "annotation", "Annotation.java"), "package java.lang.annotation; class Annotation {}", visitorsBridgeWithoutSemantic);

    VisitorsBridge visitorsBridgeWithParsingIssue = new VisitorsBridge(Collections.singletonList(new IssuableSubscriptionVisitor() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        assertThat(context.fileParsed()).isFalse();
      }

      @Override
      public List<Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD);
      }
    }), new ArrayList<>(), null);
    checkFile(constructFileName("org", "foo", "bar", "Foo.java"), "class Foo { arrrrrrgh", visitorsBridgeWithParsingIssue);
  }

  private static void checkFile(String filename, String code, VisitorsBridge visitorsBridge) {
    visitorsBridge.setCurrentFile(TestUtils.emptyInputFile(filename));
    visitorsBridge.visitFile(JParserTestUtils.parse(code), false);
  }


  private static String constructFileName(String... path) {
    String result = "";
    for (String s : path) {
      result += s + File.separator;
    }
    return result.substring(0, result.length() - 1);
  }

  @Test
  void rethrow_exception_when_hidden_property_set_to_true_with_JavaFileScanner() {
    VisitorsBridge visitorsBridge = visitorsBridge(new JFS_ThrowingNPEJavaFileScanner(), true);
    try {
      visitorsBridge.visitFile(COMPILATION_UNIT_TREE, false);
      Fail.fail("scanning of file should have raise an exception");
    } catch (AnalysisException e) {
      assertThat(e.getMessage()).contains("Failing check");
      assertThat(e.getCause()).isInstanceOf(CheckFailureException.class);
      assertThat(e.getCause().getCause()).isSameAs(NPE);
    } catch (Exception e) {
      Fail.fail("Should have been an AnalysisException");
    }
    assertThat(logTester.logs(Level.ERROR)).hasSize(1);
    assertThat(logTester.logs(Level.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder("JFS_ThrowingNPEJavaFileScanner - JFS");
  }

  @Test
  void swallow_exception_when_hidden_property_set_to_false_with_JavaFileScanner() {
    try {
      visitorsBridge(new JFS_ThrowingNPEJavaFileScanner(), false)
        .visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exception should be swallowed when property is not set");
    }
    assertThat(logTester.logs(Level.ERROR)).hasSize(1);
    assertThat(logTester.logs(Level.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder("JFS_ThrowingNPEJavaFileScanner - JFS");
  }

  @Test
  void rethrow_exception_when_hidden_property_set_to_true_with_SubscriptionVisitor() {
    VisitorsBridge visitorsBridge = visitorsBridge(new SV1_ThrowingNPEVisitingClass(), true);
    try {
      visitorsBridge.visitFile(COMPILATION_UNIT_TREE, false);
      Fail.fail("scanning of file should have raise an exception");
    } catch (AnalysisException e) {
      assertThat(e.getMessage()).contains("Failing check");
      assertThat(e.getCause()).isInstanceOf(CheckFailureException.class);
      assertThat(e.getCause().getCause()).isSameAs(NPE);
    } catch (Exception e) {
      Fail.fail("Should have been an AnalysisException");
    }
    assertThat(logTester.logs(Level.ERROR)).hasSize(1);
    assertThat(logTester.logs(Level.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder("SV1_ThrowingNPEVisitingClass - SV1");
  }

  @Test
  void swallow_exception_when_hidden_property_set_to_false_with_SubscriptionVisitor() {
    try {
      visitorsBridge(Arrays.asList(
        new SV1_ThrowingNPEVisitingClass(),
        new SV2_ThrowingNPELeavingClass(),
        new SV3_ThrowingNPEVisitingToken(),
        new SV4_ThrowingNPEVisitingTrivia()),
        false)
        .visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exceptions should be swallowed when property is not set");
    }
    assertThat(logTester.logs(Level.ERROR)).hasSize(4);
    assertThat(logTester.logs(Level.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder(
        "SV1_ThrowingNPEVisitingClass - SV1",
        "SV2_ThrowingNPELeavingClass - SV2",
        "SV3_ThrowingNPEVisitingToken - SV3",
        "SV4_ThrowingNPEVisitingTrivia - SV4");
  }

  @Test
  void swallow_exception_when_hidden_property_set_to_false_with_IssuableSubscriptionVisitor() {
    try {
      visitorsBridge(Arrays.asList(
        new IV1_ThrowingNPEVisitingClass(),
        new IV2_ThrowingNPELeavingClass()),
        false)
        .visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exceptions should be swallowed when property is not set");
    }
    assertThat(logTester.logs(Level.ERROR)).hasSize(1);
    assertThat(logTester.logs(Level.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsOnly("IV1_ThrowingNPEVisitingClass - IV1");
  }

  @Test
  void swallow_exception_when_hidden_property_set_to_false_with_all_kinds_of_visisitors() {
    try {
      visitorsBridge(Arrays.asList(
        new SV1_ThrowingNPEVisitingClass(),
        new IV1_ThrowingNPEVisitingClass()),
        false)
        .visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exceptions should be swallowed when property is not set");
    }
    assertThat(logTester.logs(Level.ERROR)).hasSize(2);
    assertThat(logTester.logs(Level.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder(
        "SV1_ThrowingNPEVisitingClass - SV1",
        "IV1_ThrowingNPEVisitingClass - IV1");
  }

  @Test
  void no_log_when_filter_execute_fine() {
    VisitorsBridge visitorsBridge = visitorsBridge(Arrays.asList(), true);
    try {
      visitorsBridge.visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("No exception should be raised");
    }
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  void should_not_create_symbol_table_for_generated() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    VisitorsBridge bridge = new VisitorsBridge(Collections.emptySet(), Collections.emptyList(), sonarComponents);
    bridge.setCurrentFile(new GeneratedFile(null));
    Tree tree = new JavaTree.CompilationUnitTreeImpl(null, new ArrayList<>(), new ArrayList<>(), null, null);
    bridge.visitFile(tree, false);
    verify(sonarComponents, never()).symbolizableFor(any());
  }

  @Test
  void filter_scanner_by_java_version() {
    List<String> trace = new ArrayList<>();
    class RuleForAllJavaVersion implements JavaFileScanner, EndOfAnalysis {
      @Override
      public void scanFile(JavaFileScannerContext context) {
      }

      @Override
      public void endOfAnalysis(ModuleScannerContext context) {
        trace.add(this.getClass().getSimpleName());
      }
    }
    class RuleForJava15 implements JavaFileScanner, JavaVersionAwareVisitor, EndOfAnalysis {
      @Override
      public boolean isCompatibleWithJavaVersion(JavaVersion version) {
        return version.isJava15Compatible();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
      }

      @Override
      public void endOfAnalysis(ModuleScannerContext context) {
        trace.add(this.getClass().getSimpleName());
      }
    }
    class SubscriptionVisitorForJava10 extends IssuableSubscriptionVisitor implements JavaFileScanner, JavaVersionAwareVisitor, EndOfAnalysis {
      @Override
      public boolean isCompatibleWithJavaVersion(JavaVersion version) {
        return version.isJava10Compatible();
      }

      @Override
      public List<Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.TOKEN);
      }

      @Override
      public void endOfAnalysis(ModuleScannerContext context) {
        trace.add(this.getClass().getSimpleName());
      }
    }
    List<JavaFileScanner> visitors = Arrays.asList(
      new RuleForAllJavaVersion(),
      new RuleForJava15(),
      new SubscriptionVisitorForJava10());
    // By default, the visitor bridge is created with a version = -1 (unset)
    VisitorsBridge visitorsBridge = new VisitorsBridge(visitors, Collections.emptyList(), null);
    visitorsBridge.endOfAnalysis();
    assertThat(trace).containsExactly("RuleForAllJavaVersion");

    trace.clear();
    visitorsBridge = new VisitorsBridge(visitors, Collections.emptyList(), null, new JavaVersionImpl(8));
    visitorsBridge.endOfAnalysis();
    assertThat(trace).containsExactly("RuleForAllJavaVersion");

    trace.clear();
    visitorsBridge = new VisitorsBridge(visitors, Collections.emptyList(), null, new JavaVersionImpl(11));
    visitorsBridge.endOfAnalysis();
    assertThat(trace).containsExactly("RuleForAllJavaVersion", "SubscriptionVisitorForJava10");

    trace.clear();
    visitorsBridge = new VisitorsBridge(visitors, Collections.emptyList(), null, new JavaVersionImpl(16));
    visitorsBridge.endOfAnalysis();
    assertThat(trace).containsExactly("RuleForAllJavaVersion", "RuleForJava15", "SubscriptionVisitorForJava10");
  }

  @Test
  void canSkipScanningOfUnchangedFiles_returns_false_by_default() {
    VisitorsBridge vb = visitorsBridge(Collections.emptyList(), true);
    assertThat(vb.canSkipScanningOfUnchangedFiles()).isFalse();
  }

  @Test
  void canSkipScanningOfUnchangedFiles_returns_based_on_context() throws ApiMismatchException {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    VisitorsBridge vb = new VisitorsBridge(
      Collections.emptyList(),
      Collections.emptyList(),
      sonarComponents
    );

    doReturn(true).when(sonarComponents).canSkipUnchangedFiles();
    assertThat(vb.canSkipScanningOfUnchangedFiles()).isTrue();

    doReturn(false).when(sonarComponents).canSkipUnchangedFiles();
    assertThat(vb.canSkipScanningOfUnchangedFiles()).isFalse();

    ApiMismatchException exception = new ApiMismatchException(new NoSuchMethodError());
    doThrow(exception).when(sonarComponents).canSkipUnchangedFiles();
    assertThat(vb.canSkipScanningOfUnchangedFiles()).isFalse();
  }

  @Test
  void canVisitorBeSkippedOnUnchangedFiles_returns_false_for_EndOfAnalysisChecks() {
    Object visitor = new EndOfAnalysisVisitor();
    assertThat(VisitorsBridge.canVisitorBeSkippedOnUnchangedFiles(visitor)).isFalse();
  }

  @Test
  void canVisitorBeSkippedOnUnchangedFiles_returns_false_for_visitors_defined_outside_of_checks_package() {
    Object visitor = new VisitorNotInChecksPackage();
    assertThat(VisitorsBridge.canVisitorBeSkippedOnUnchangedFiles(visitor)).isFalse();
  }

  @Test
  void canVisitorBeSkippedOnUnchangedFiles_returns_true_for_valid_visitors() {
    Object visitor = new VisitorThatCanBeSkipped();
    assertThat(VisitorsBridge.canVisitorBeSkippedOnUnchangedFiles(visitor)).isTrue();
  }

  @Test
  void visitorsBridge_uses_appropriate_scanners() throws ApiMismatchException {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doReturn(true).when(sonarComponents).canSkipUnchangedFiles();

    VisitorThatCanBeSkipped skippableVisitor = spy(new VisitorThatCanBeSkipped());
    EndOfAnalysisVisitor endOfAnalysisVisitor = spy(new EndOfAnalysisVisitor());
    VisitorNotInChecksPackage unskippableVisitor = spy(new VisitorNotInChecksPackage());
    VisitorWithIncompatibleVersion incompatibleVisitor = spy(new VisitorWithIncompatibleVersion());

    VisitorsBridge visitorsBridge = new VisitorsBridge(
      List.of(skippableVisitor, endOfAnalysisVisitor, unskippableVisitor, incompatibleVisitor),
      Collections.emptyList(),
      sonarComponents,
      JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION
    );

    verify(skippableVisitor, times(1)).nodesToVisit();
    verify(endOfAnalysisVisitor, never()).nodesToVisit();
    verify(unskippableVisitor, times(2)).nodesToVisit();
    verify(incompatibleVisitor, never()).nodesToVisit();

    visitorsBridge.visitFile(null, true);

    verify(skippableVisitor, never()).visitNode(any());
    verify(endOfAnalysisVisitor, times(1)).scanFile(any());
    verify(unskippableVisitor, times(1)).visitNode(any());
    verify(incompatibleVisitor, never()).visitNode(any());

    visitorsBridge.visitFile(null, false);
    verify(skippableVisitor, times(1)).visitNode(any());
    verify(endOfAnalysisVisitor, times(2)).scanFile(any());
    verify(unskippableVisitor, times(2)).visitNode(any());
    verify(incompatibleVisitor, never()).visitNode(any());
  }

  @Test
  void endOfAnalysis_logs_nothing_when_no_file_has_been_analyzed() {
    VisitorsBridge visitorsBridge = new VisitorsBridge(
      Collections.emptyList(),
      Collections.emptyList(),
      null
    );
    assertThat(logTester.getLogs(Level.INFO)).isEmpty();
    visitorsBridge.endOfAnalysis();
    assertThat(logTester.getLogs(Level.INFO)).isEmpty();
  }

  @Test
  void endOfAnalysis_logs_when_no_file_has_been_optimized() throws ApiMismatchException {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doReturn(false).when(sonarComponents).canSkipUnchangedFiles();
    VisitorsBridge visitorsBridge = new VisitorsBridge(
      Collections.emptyList(),
      Collections.emptyList(),
      sonarComponents
    );

    assertThat(logTester.getLogs(Level.INFO)).isEmpty();
    visitorsBridge.visitFile(null, false);
    assertThat(logTester.getLogs(Level.INFO)).isEmpty();
    visitorsBridge.endOfAnalysis();
    List<LogAndArguments> logsAfterEndOfAnalysis = logTester.getLogs(Level.INFO);
    assertThat(logsAfterEndOfAnalysis).hasSize(1);
    assertThat(logsAfterEndOfAnalysis.get(0).getFormattedMsg())
      .isEqualTo("Did not optimize analysis for any files, performed a full analysis for all 1 files.");
  }

  @Test
  void endOfAnalysis_logs_when_at_least_one_file_has_been_optimized() throws ApiMismatchException {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doReturn(false).when(sonarComponents).canSkipUnchangedFiles();
    VisitorsBridge visitorsBridge = new VisitorsBridge(
      Collections.emptyList(),
      Collections.emptyList(),
      sonarComponents
    );

    assertThat(logTester.getLogs(Level.INFO)).isEmpty();
    visitorsBridge.visitFile(null, true);
    assertThat(logTester.getLogs(Level.INFO)).isEmpty();
    visitorsBridge.endOfAnalysis();
    List<LogAndArguments> logsAfterEndOfAnalysis = logTester.getLogs(Level.INFO);
    assertThat(logsAfterEndOfAnalysis).hasSize(1);
    assertThat(logsAfterEndOfAnalysis.get(0).getFormattedMsg())
      .isEqualTo("Optimized analysis for 1 of 1 files.");
  }

  @Nested
  class ScanWithoutParsing {

    @Test
    void setCacheContext_sets_the_expected_value() {
      CacheContext cacheContext = mock(CacheContext.class);
      VisitorsBridge visitorsBridge = new VisitorsBridge(null);
      assertThat(visitorsBridge.cacheContext).isNotEqualTo(cacheContext);
      visitorsBridge.setCacheContext(cacheContext);
      assertThat(visitorsBridge.cacheContext).isEqualTo(cacheContext);
    }

    @Test
    void scanWithoutParsing_returns_false_when_the_file_cannot_be_skipped() throws ApiMismatchException {
      // When VB has no SonarComponents
      VisitorsBridge visitorsBridge = new VisitorsBridge(null);
      InputFile inputFile = mock(InputFile.class);
      doReturn(InputFile.Status.CHANGED).when(inputFile).status();
      CacheContext cacheContext = mock(CacheContext.class);
      assertThat(visitorsBridge.scanWithoutParsing(inputFile)).isFalse();

      // When SonarComponents is set and does not allow the file to be skipped
      SonarComponents sonarComponents = mock(SonarComponents.class);
      doReturn(false).when(sonarComponents).fileCanBeSkipped(any(InputFile.class));
      doReturn(true).when(sonarComponents).canSkipUnchangedFiles();
      VisitorsBridge visitorsBridgeWithSonarComponents = new VisitorsBridge(
        Collections.emptyList(),
        Collections.emptyList(),
        sonarComponents
      );

      assertThat(visitorsBridge.scanWithoutParsing(inputFile)).isFalse();
    }

    @Test
    void scanWithoutParsing_returns_false_when_the_file_is_a_generated_file() throws ApiMismatchException {
      InputFile inputFile = new GeneratedFile(Path.of("non-existing-generated-file.java"));

      SonarComponents sonarComponents = spy(new SonarComponents(null, null, null, null, null));
      SensorContext contextMock = mock(SensorContext.class);
      sonarComponents.setSensorContext(contextMock);

      doReturn(true).when(sonarComponents).canSkipUnchangedFiles();
      VisitorsBridge visitorsBridge = new VisitorsBridge(
        Collections.singletonList(new EndOfAnalysisVisitor()),
        Collections.emptyList(),
        sonarComponents
      );

      assertThat(visitorsBridge.scanWithoutParsing(inputFile)).isFalse();
    }

    @Test
    void scanWithoutParsing_returns_true_for_scanners_that_do_not_override_scanWithoutParsing() throws ApiMismatchException {
      JavaFileScanner scanner = new DefaultEndOfAnalysisCheck();
      assertThat(scan_without_parsing(scanner)).isTrue();
    }

    @Test
    void scanWithoutParsing_returns_false_when_a_JFS_cannot_scan_successfully_without_parsing() throws ApiMismatchException {
      assertThat(scan_without_parsing(new ScannerThatCannotScanWithoutParsing())).isFalse();
    }

    @Test
    void scanWithoutParsing_returns_false_when_an_ISV_cannot_scan_successfully_without_parsing() throws ApiMismatchException {
      assertThat(scan_without_parsing(new IsvThatCannotScanWithoutParsing())).isFalse();
    }

    @Test
    void scanWithoutParsing_returns_false_when_a_JFS_throws_an_exception_while_scanning_without_parsing_and_fail_fast_is_disabled() throws ApiMismatchException {
      ScannerThatCannotScanWithoutParsing scanner = spy(new ScannerThatCannotScanWithoutParsing());
      doThrow(new RuntimeException("boom")).when(scanner).scanWithoutParsing(any());

      returns_false_when_a_scanner_throws_an_exception_while_scanning_without_parsing_and_fail_fast_is_disabled(scanner);
    }

    @Test
    void scanWithoutParsing_returns_false_when_an_ISV_throws_an_exception_while_scanning_without_parsing_and_fail_fast_is_disabled() throws ApiMismatchException {
      IsvThatCannotScanWithoutParsing scanner = spy(new IsvThatCannotScanWithoutParsing());
      doThrow(new RuntimeException("boom")).when(scanner).scanWithoutParsing(any());

      returns_false_when_a_scanner_throws_an_exception_while_scanning_without_parsing_and_fail_fast_is_disabled(scanner);
    }

    @Test
    void scanWithoutParsing_triggers_an_AnalysisException_when_a_JFS_throws_while_scanning_without_parsing() throws ApiMismatchException {
      ScannerThatCannotScanWithoutParsing scanner = spy(new ScannerThatCannotScanWithoutParsing());
      RuntimeException boom = new RuntimeException("boom");
      doThrow(boom).when(scanner).scanWithoutParsing(any());

      triggers_an_AnalysisException_when_a_scanner_throws_while_scanning_without_parsing(scanner);
    }

    @Test
    void scanWithoutParsing_triggers_an_AnalysisException_when_an_ISV_throws_while_scanning_without_parsing() throws ApiMismatchException {
      IsvThatCannotScanWithoutParsing scanner = spy(new IsvThatCannotScanWithoutParsing());
      RuntimeException boom = new RuntimeException("boom");
      doThrow(boom).when(scanner).scanWithoutParsing(any());

      triggers_an_AnalysisException_when_a_scanner_throws_while_scanning_without_parsing(scanner);
    }

    private void returns_false_when_a_scanner_throws_an_exception_while_scanning_without_parsing_and_fail_fast_is_disabled(JavaFileScanner scanner) throws ApiMismatchException {
      SonarComponents sonarComponents = mock(SonarComponents.class);
      doReturn(true).when(sonarComponents).fileCanBeSkipped(any(InputFile.class));
      doReturn(true).when(sonarComponents).canSkipUnchangedFiles();
      doReturn(false).when(sonarComponents).shouldFailAnalysisOnException();

      assertThat(scan_without_parsing(sonarComponents, scanner)).isFalse();
    }

    private void triggers_an_AnalysisException_when_a_scanner_throws_while_scanning_without_parsing(JavaFileScanner scanner) throws ApiMismatchException {
      SonarComponents sonarComponents = mock(SonarComponents.class);
      doReturn(true).when(sonarComponents).fileCanBeSkipped(any(InputFile.class));
      doReturn(true).when(sonarComponents).canSkipUnchangedFiles();
      doReturn(true).when(sonarComponents).shouldFailAnalysisOnException();

      InputFile inputFile = mock(InputFile.class);
      doReturn(InputFile.Status.CHANGED).when(inputFile).status();

      assertThatThrownBy(() -> scan_without_parsing(sonarComponents, scanner, inputFile))
        .hasRootCauseMessage("boom")
        .hasRootCauseInstanceOf(RuntimeException.class)
        .hasMessage("Failing check")
        .isInstanceOf(AnalysisException.class);

      String expectedLogMessage = String.format(
        "Scan without parsing of file %s failed for scanner %s.",
        inputFile.toString(),
        scanner.getClass().getCanonicalName()
      );

      List<LogAndArguments> warningLogs = logTester.getLogs(Level.WARN);
      assertThat(warningLogs).hasSize(1);
      assertThat(warningLogs.get(0).getFormattedMsg()).isEqualTo(expectedLogMessage);
    }

    private boolean scan_without_parsing(JavaFileScanner scanner) throws ApiMismatchException {
      SonarComponents sonarComponents = mock(SonarComponents.class);
      doReturn(true).when(sonarComponents).fileCanBeSkipped(any(InputFile.class));
      doReturn(true).when(sonarComponents).canSkipUnchangedFiles();

      return scan_without_parsing(sonarComponents, scanner);
    }

    private boolean scan_without_parsing(SonarComponents sonarComponents, JavaFileScanner scanner) {
      InputFile inputFile = mock(InputFile.class);
      doReturn(InputFile.Status.CHANGED).when(inputFile).status();
      return scan_without_parsing(sonarComponents, scanner, inputFile);
    }

    private boolean scan_without_parsing(SonarComponents sonarComponents, JavaFileScanner scanner, InputFile inputFile) {
      VisitorsBridge visitorsBridge = new VisitorsBridge(
        Collections.singletonList(scanner),
        Collections.emptyList(),
        sonarComponents
      );

      visitorsBridge.setCurrentFile(inputFile);

      return visitorsBridge.scanWithoutParsing(inputFile);
    }
  }

  private static String ruleKeyFromErrorLog(String errorLog) {
    String newString = errorLog.substring("Unable to run check class ".length(), errorLog.indexOf(" on file"));
    if (newString.contains("SymbolicExecutionVisitor")) {
      return "SE";
    }
    return newString.substring(newString.lastIndexOf("$") + 1);
  }

  private final VisitorsBridge visitorsBridge(JavaFileScanner visitor, boolean failOnException) {
    return visitorsBridge(Collections.singletonList(visitor), failOnException);
  }

  private final VisitorsBridge visitorsBridge(Collection<JavaFileScanner> visitors, boolean failOnException) {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    sensorContextTester.setSettings(new MapSettings().setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, failOnException));

    sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContextTester);

    VisitorsBridge visitorsBridge = new VisitorsBridge(visitors, new ArrayList<>(), sonarComponents);
    visitorsBridge.setCurrentFile(INPUT_FILE);

    return visitorsBridge;
  }

  @org.sonar.check.Rule(key = "JFS")
  private static class JFS_ThrowingNPEJavaFileScanner implements JavaFileScanner {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "SV1")
  private static class SV1_ThrowingNPEVisitingClass extends SubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "SV2")
  private static class SV2_ThrowingNPELeavingClass extends SubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void leaveNode(Tree tree) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "SV3")
  private static class SV3_ThrowingNPEVisitingToken extends SubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TOKEN);
    }

    @Override
    public void visitToken(SyntaxToken syntaxToken) {
      if ("{".equals(syntaxToken.text())) {
        // so it only throws once and not on every token
        throw NPE;
      }
    }
  }

  @org.sonar.check.Rule(key = "SV4")
  private static class SV4_ThrowingNPEVisitingTrivia extends SubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "IV1")
  private static class IV1_ThrowingNPEVisitingClass extends IssuableSubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "IV2")
  private static class IV2_ThrowingNPELeavingClass extends IssuableSubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void leaveNode(Tree tree) {
      throw NPE;
    }
  }

  private static class VisitorWithIncompatibleVersion extends IssuableSubscriptionVisitor implements EndOfAnalysis, JavaVersionAwareVisitor {
    @Override
    public List<Kind> nodesToVisit() {
      return List.of(Kind.COMPILATION_UNIT);
    }

    @Override
    public void endOfAnalysis(ModuleScannerContext context) {
      // do nothing
    }

    @Override
    public boolean isCompatibleWithJavaVersion(JavaVersion version) {
      return false;
    }
  }

  private static class DefaultEndOfAnalysisCheck implements EndOfAnalysis, JavaFileScanner {

    @Override
    public void endOfAnalysis(ModuleScannerContext context) {
      /* Do nothing */
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      /* Do nothing */
    }
  }

  private static class ScannerThatCannotScanWithoutParsing implements EndOfAnalysis, JavaFileScanner {
    /**
     * Always fail
     */
    @Override
    public boolean scanWithoutParsing(InputFileScannerContext fileScannerContext) {
      return false;
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      /* Do nothing */
    }

    @Override
    public void endOfAnalysis(ModuleScannerContext context) {
      /* Do nothing */
    }
  }

  private static class IsvThatCannotScanWithoutParsing extends IssuableSubscriptionVisitor implements EndOfAnalysis {

    @Override
    public void endOfAnalysis(ModuleScannerContext context) {
      /* Do nothing */
    }

    @Override
    public List<Kind> nodesToVisit() {
      return Collections.emptyList();
    }

    @Override
    public boolean scanWithoutParsing(InputFileScannerContext fileScannerContext) {
      return false;
    }
  }
}
