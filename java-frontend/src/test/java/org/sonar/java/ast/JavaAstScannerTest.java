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
package org.sonar.java.ast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.typed.ActionParser;
import com.sonar.sslr.api.typed.GrammarBuilder;
import java.io.File;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisError;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.parser.JavaNodeBuilder;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.se.SymbolicExecutionMode;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.squidbridge.AstScannerExceptionHandler;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JavaAstScannerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();
  SensorContextTester context;
  private DefaultFileSystem fs;

  @Before
  public void setUp() throws Exception {
    context = SensorContextTester.create(new File(""));
    fs = context.fileSystem();
  }

  @Test
  public void comments() {
    File file = new File("src/test/files/metrics/Comments.java");
    DefaultInputFile resource = new TestInputFileBuilder("", "src/test/files/metrics/Comments.java").build();
    fs.add(resource);
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    JavaAstScanner.scanSingleFileForTests(file, new VisitorsBridge(new Measurer(fs, context, noSonarFilter)));
    verify(noSonarFilter).noSonarInFile(resource, ImmutableSet.of(15));
  }

  @Test
  public void noSonarLines() throws Exception {
    File file = new File("src/test/files/metrics/NoSonar.java");
    DefaultInputFile resource = new TestInputFileBuilder("", "src/test/files/metrics/NoSonar.java").build();
    fs.add(resource);
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    JavaAstScanner.scanSingleFileForTests(file, new VisitorsBridge(new Measurer(fs, context, noSonarFilter)));
    verify(noSonarFilter).noSonarInFile(resource, ImmutableSet.of(8));
    //No Sonar on tests files
    NoSonarFilter noSonarFilterForTest = mock(NoSonarFilter.class);
    JavaAstScanner.scanSingleFileForTests(file, new VisitorsBridge(new Measurer(fs, context, noSonarFilterForTest).new TestFileMeasurer()));
    verify(noSonarFilterForTest).noSonarInFile(resource, ImmutableSet.of(8));
  }

  @Test
  public void scan_single_file_with_dumb_file_should_fail() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    String filename = "!!dummy";
    thrown.expectMessage(filename);
    JavaAstScanner.scanSingleFileForTests(new File(filename), new VisitorsBridge(null));
  }

  @Test
  public void should_not_fail_whole_analysis_upon_parse_error_and_notify_audit_listeners() {
    FakeAuditListener listener = spy(new FakeAuditListener());
    JavaAstScanner scanner = defaultJavaAstScanner();
    scanner.setVisitorBridge(new VisitorsBridge(listener));

    scanner.scan(ImmutableList.of(new File("src/test/resources/AstScannerParseError.txt")));
    verify(listener).processRecognitionException(any(RecognitionException.class));
  }

  @Test
  public void should_handle_analysis_cancellation() throws Exception {
    JavaFileScanner visitor = spy(new JavaFileScanner() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        // do nothing
      }
    });
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.analysisCancelled()).thenReturn(true);
    JavaAstScanner scanner = new JavaAstScanner(JavaParser.createParser(), sonarComponents);
    scanner.setVisitorBridge(new VisitorsBridge(Lists.newArrayList(visitor), Lists.newArrayList(), sonarComponents));
    scanner.scan(ImmutableList.of(new File("src/test/files/metrics/NoSonar.java")));
    verifyZeroInteractions(visitor);
  }

  @Test
  public void should_interrupt_analysis_when_InterruptedException_is_thrown() {
    File file = new File("src/test/files/metrics/NoSonar.java");

    thrown.expectMessage("Analysis cancelled");
    thrown.expect(new AnalysisExceptionBaseMatcher(RecognitionException.class, "instanceof AnalysisException with RecognitionException cause"));

    JavaAstScanner.scanSingleFileForTests(file, new VisitorsBridge(new CheckThrowingException(new RecognitionException(42, "interrupted", new InterruptedException()))));
  }

  @Test
  public void should_interrupt_analysis_when_InterruptedIOException_is_thrown() {
    File file = new File("src/test/files/metrics/NoSonar.java");

    thrown.expectMessage("Analysis cancelled");
    thrown.expect(new AnalysisExceptionBaseMatcher(RecognitionException.class, "instanceof AnalysisException with RecognitionException cause"));

    JavaAstScanner.scanSingleFileForTests(file, new VisitorsBridge(new CheckThrowingException(new RecognitionException(42, "interrupted", new InterruptedIOException()))));
  }

  @Test
  public void should_swallow_log_and_report_checks_exceptions() {
    JavaAstScanner scanner = defaultJavaAstScanner();
    SonarComponents sonarComponent = new SonarComponents(null, context.fileSystem(), null, null, null, null);
    sonarComponent.setSensorContext(context);
    scanner.setVisitorBridge(new VisitorsBridge(Collections.singleton(new CheckThrowingException(new NullPointerException("foo"))), new ArrayList<>(), sonarComponent));
    File scannedFile = new File("src/test/resources/AstScannerNoParseError.txt");

    scanner.scan(ImmutableList.of(scannedFile));
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1).contains("Unable to run check class org.sonar.java.ast.JavaAstScannerTest$CheckThrowingException -  on file "
      + scannedFile.getPath()
      + ", To help improve SonarJava, please report this problem to SonarSource : see https://www.sonarqube.org/community/");
    assertThat(sonarComponent.analysisErrors).hasSize(1);
    assertThat(sonarComponent.analysisErrors.get(0).getKind()).isSameAs(AnalysisError.Kind.CHECK_ERROR);
    logTester.clear();
    scanner.setVisitorBridge(new VisitorsBridge(new AnnotatedCheck(new NullPointerException("foo"))));
    scannedFile = new File("src/test/resources/AstScannerParseError.txt");
    scanner.scan(ImmutableList.of(scannedFile));
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(3).contains("Unable to run check class org.sonar.java.ast.JavaAstScannerTest$AnnotatedCheck - AnnotatedCheck on file "
      + scannedFile.getPath()
      + ", To help improve SonarJava, please report this problem to SonarSource : see https://www.sonarqube.org/community/");
  }

  @Test
  public void should_swallow_log_and_report_checks_exceptions_for_symbolic_execution() {
    JavaAstScanner scanner = new JavaAstScanner(JavaParser.createParser(), null);
    logTester.clear();
    SonarComponents sonarComponent = new SonarComponents(null, context.fileSystem(), null, null, null, null);
    context.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    sonarComponent.setSensorContext(context);
    scanner.setVisitorBridge(new VisitorsBridge(Collections.singletonList(new SECheck() {
      @Override
      public void init(MethodTree methodTree, CFG cfg) {
        throw new NullPointerException("nobody expect the spanish inquisition !");
      }
    }), new ArrayList<>(), sonarComponent, SymbolicExecutionMode.ENABLED_WITHOUT_X_FILE));
    scanner.scan(ImmutableList.of(new File("src/test/resources/se/MethodBehavior.java")));
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith("Unable to run check class org.sonar.java.se.SymbolicExecutionVisitor");
    assertThat(sonarComponent.analysisErrors).hasSize(1);
    assertThat(sonarComponent.analysisErrors.get(0).getKind()).isSameAs(AnalysisError.Kind.SE_ERROR);
  }
  @Test
  public void should_propagate_SOError() {
    thrown.expect(StackOverflowError.class);
    JavaAstScanner scanner = defaultJavaAstScanner();
    scanner.setVisitorBridge(new VisitorsBridge(new CheckThrowingSOError()));
    scanner.scan(ImmutableList.of(new File("src/test/resources/AstScannerNoParseError.txt")));
  }

  @Test
  public void should_report_analysis_error_in_sonarLint_context_withSQ_6_0() {
    JavaAstScanner scanner = defaultJavaAstScanner();
    FakeAuditListener listener = spy(new FakeAuditListener());
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.reportAnalysisError(any(RecognitionException.class), any(File.class))).thenReturn(true);
    scanner.setVisitorBridge(new VisitorsBridge(Lists.newArrayList(listener), Lists.newArrayList(), sonarComponents));
    scanner.scan(ImmutableList.of(new File("src/test/resources/AstScannerParseError.txt")));
    verify(sonarComponents).reportAnalysisError(any(RecognitionException.class), any(File.class));
    verifyZeroInteractions(listener);
  }

  private static JavaAstScanner defaultJavaAstScanner() {
    return new JavaAstScanner(new ActionParser<>(StandardCharsets.UTF_8, FakeLexer.builder(), FakeGrammar.class, new FakeTreeFactory(), new JavaNodeBuilder(), FakeLexer.ROOT), null);
  }

  private static class CheckThrowingSOError implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      throw new StackOverflowError();
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

  private static class AnalysisExceptionBaseMatcher extends BaseMatcher {

    private final Class<? extends Exception> expectedCause;
    private final String description;

    public AnalysisExceptionBaseMatcher(Class<? extends Exception> expectedCause, String description) {
      this.expectedCause = expectedCause;
      this.description = description;
    }

    @Override
    public boolean matches(Object item) {
      return item instanceof AnalysisException
        && expectedCause.equals(((AnalysisException) item).getCause().getClass());
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(this.description);
    }

  }

  private static class FakeAuditListener implements JavaFileScanner, AstScannerExceptionHandler {

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

  public static class FakeTreeFactory {
    public FakeTreeFactory(){}
    public Tree root(JavaTree javaTree) {
      return new Tree() {
        @Override
        public boolean is(Kind... kind) {
          return false;
        }

        @Override
        public void accept(TreeVisitor visitor) {

        }

        @Override
        public Kind kind() {
          return null;
        }

        @Override
        public Tree parent() {
          return null;
        }

        @Override
        public SyntaxToken firstToken() {
          return null;
        }

        @Override
        public SyntaxToken lastToken() {
          return null;
        }
      };
    }
  }

  public static class FakeGrammar {
    final GrammarBuilder<InternalSyntaxToken> b;
    final FakeTreeFactory f;

    public FakeGrammar(GrammarBuilder<InternalSyntaxToken> b, FakeTreeFactory f) {
      this.b = b;
      this.f = f;
    }

    public Tree ROOT() {
      return b.<Tree>nonterminal(FakeLexer.ROOT).is(f.root(b.token(FakeLexer.TOKEN)));
    }
  }

  public enum FakeLexer implements GrammarRuleKey {
    ROOT, TOKEN;

    public static LexerlessGrammarBuilder builder() {
      LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

      b.rule(TOKEN).is("foo");
      b.setRootRule(ROOT);

      return b;
    }

  }

}
