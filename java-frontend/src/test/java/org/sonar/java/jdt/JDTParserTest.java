/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.jdt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.parser.TerminalToken;
import org.eclipse.jdt.internal.formatter.Token;
import org.eclipse.jdt.internal.formatter.TokenManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia.CommentKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.sonar.java.jdt.JDTAstConverter.convertTokenTypeToCommentKind;
import static org.sonar.java.jdt.JDTAstConverter.isComment;
import static org.sonar.java.jdt.JDTParser.shouldEnablePreviewFlag;
import static org.sonar.java.model.JParser.MAXIMUM_SUPPORTED_JAVA_VERSION;
import static org.sonar.java.model.JParser.Mode.BATCH;
import static org.sonar.java.model.JParserTestUtils.DEFAULT_CLASSPATH;

class JDTParserTest {

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.DEBUG);

  @Test
  void test_first_index_of_tokens_in_eclipse_ast() {
    String version = JParser.MAXIMUM_SUPPORTED_JAVA_VERSION.effectiveJavaVersionAsString();
    String unitName = "C.java";
    String source = "class C { }";

    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    JavaCore.setComplianceOptions(version, JavaCore.getOptions());
    astParser.setResolveBindings(true);
    astParser.setUnitName(unitName);
    astParser.setSource(source.toCharArray());
    CompilationUnit compilationUnit = (CompilationUnit) astParser.createAST(null);
    TokenManager tokenManager = JDTAstConverter.createTokenManager(version, unitName, source);

    assertThat(JDTAstConverter.firstIndexIn(tokenManager, compilationUnit, TerminalToken.TokenNameIdentifier, TerminalToken.TokenNameLBRACE)).isEqualTo(1);
    assertThat(JDTAstConverter.firstIndexIn(tokenManager, compilationUnit, TerminalToken.TokenNameLBRACE, TerminalToken.TokenNameIdentifier)).isEqualTo(1);
    assertThat(JDTAstConverter.firstIndexIn(tokenManager, compilationUnit, TerminalToken.TokenNameRBRACE, TerminalToken.TokenNameLBRACE)).isEqualTo(2);
    assertThatThrownBy(() -> JDTAstConverter.firstIndexIn(tokenManager, compilationUnit, TerminalToken.TokenNamebreak, TerminalToken.TokenNameconst))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to find token TokenNamebreak or TokenNameconst in the tokens of a org.eclipse.jdt.core.dom.CompilationUnit");
  }

  @Test
  void test_comment_tokens() {
    String version = JParser.MAXIMUM_SUPPORTED_JAVA_VERSION.effectiveJavaVersionAsString();
    String unitName = "C.java";
    String source = """
      class A {
        // line comment
        /* block comment */
        /// markdown comment 1
        /// markdown comment 2
        /**
          * javadoc comment
          */
        void foo() {}
      }
      """;
    TokenManager tokens = JDTAstConverter.createTokenManager(version, unitName, source);

    assertThat(tokens.size()).isEqualTo(15);

    Token token0 = tokens.get(0);
    assertThat(token0.toString(source)).isEqualTo("class");
    assertThat(token0.isComment()).isFalse();
    assertThat(isComment(token0)).isFalse();
    assertThatThrownBy(() -> convertTokenTypeToCommentKind(token0))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Unexpected value: TokenNameclass");

    Token token3 = tokens.get(3);
    assertThat(token3.toString(source)).isEqualTo("// line comment");
    assertThat(token3.isComment()).isTrue();
    assertThat(isComment(token3)).isTrue();
    assertThat(convertTokenTypeToCommentKind(token3)).isEqualTo(CommentKind.LINE);

    Token token4 = tokens.get(4);
    assertThat(token4.toString(source)).isEqualTo("/* block comment */");
    assertThat(token4.isComment()).isTrue();
    assertThat(isComment(token4)).isTrue();
    assertThat(convertTokenTypeToCommentKind(token4)).isEqualTo(CommentKind.BLOCK);

    Token token5 = tokens.get(5);
    assertThat(token5.toString(source)).isEqualTo("/// markdown comment 1\n  /// markdown comment 2");
    assertThat(token5.isComment()).isTrue();
    assertThat(isComment(token5)).isTrue();
    assertThat(convertTokenTypeToCommentKind(token5)).isEqualTo(CommentKind.MARKDOWN);

    Token token6 = tokens.get(6);
    assertThat(token6.toString(source)).isEqualTo("/**\n    * javadoc comment\n    */");
    assertThat(token6.isComment()).isTrue();
    assertThat(isComment(token6)).isTrue();
    assertThat(convertTokenTypeToCommentKind(token6)).isEqualTo(CommentKind.JAVADOC);

    Token token7 = tokens.get(7);
    assertThat(token7.toString(source)).isEqualTo("void");
    assertThat(token7.isComment()).isFalse();
    assertThat(isComment(token7)).isFalse();
  }

  @Test
  void failing_batch_mode_should_continue_file_by_file() {
    List<InputFile> inputFiles = Arrays.asList(
      TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java"));
    List<String> trace = new ArrayList<>();
    BiConsumer<InputFile, JParseResult> action = (inputFile, result) -> {
      trace.addAll(logTester.logs());
      logTester.clear();
      try {
        JavaTree.CompilationUnitTreeImpl tree = result.get();
        trace.add("[action] analyse class " + ((ClassTree)tree.types().get(0)).simpleName().name() + " in " + inputFile.filename());
      } catch (Exception e) {
        trace.add("[action] handle " + e.getClass().getSimpleName() + ": " + e.getMessage());
      }
    };
    BatchWithException batchWithException = new BatchWithException();
    // exception before the first file
    batchWithException.exceptions.push(new NullPointerException("Boom!"));
    batchWithException.parse(inputFiles, () -> false, new AnalysisProgress(inputFiles.size()), action);
    trace.addAll(logTester.logs());
    logTester.clear();
    assertThat(trace).containsExactly(
      "Unexpected NullPointerException: Boom!",
      "Fallback to file by file analysis for 2 files",
      "[action] analyse class HelloWorld in Classes.java",
      "[action] analyse class MyClass in Methods.java");
  }

  @Test
  void successful_batch_mode_with_missing_analyzed_files_should_continue_file_by_file() {
    List<InputFile> inputFiles = Arrays.asList(
      TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java"));
    BiConsumer<InputFile, JParseResult> doNothingAction = (inputFile, result) -> {};
    JDTParser config = spy((JDTParser) JParser.Mode.BATCH
      .create(MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false));
    // Return a lazy ASTParser that do nothing to ensure that we have not analyzed files
    ASTParser astParser = mock(ASTParser.class);
    doReturn(astParser).when(config).jdtAstParser();

    config.parse(inputFiles, () -> false, new AnalysisProgress(inputFiles.size()), doNothingAction);

    assertThat(logTester.logs()).containsExactly(
      "Unexpected AnalysisException: 2/2 files were not analyzed by the batch mode",
      "Fallback to file by file analysis for 2 files");
    logTester.clear();
  }

  @Test
  void failing_batch_mode_should_stop_file_by_file_when_cancelled() {
    List<InputFile> inputFiles = Arrays.asList(
      TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java"));
    List<String> trace = new ArrayList<>();
    AtomicBoolean isCanceled = new AtomicBoolean(false);
    BiConsumer<InputFile, JParseResult> action = (inputFile, result) -> {
      trace.addAll(logTester.logs());
      logTester.clear();
      try {
        JavaTree.CompilationUnitTreeImpl tree = result.get();
        trace.add("[action] analyze class " + ((ClassTree)tree.types().get(0)).simpleName().name() + " in " + inputFile.filename());
        // cancel analysis after first file
        isCanceled.set(true);
      } catch (Exception e) {
        trace.add("[action] handle " + e.getClass().getSimpleName() + ": " + e.getMessage());
      }
    };

    JParser config = BATCH
      .create(MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false);
    AnalysisProgress analysisProgress = new AnalysisProgress(inputFiles.size());
    assertThatThrownBy(() -> config.parse(inputFiles, isCanceled::get, analysisProgress, action))
      .isInstanceOf(OperationCanceledException.class);

    trace.addAll(logTester.logs());
    logTester.clear();
    assertThat(trace).containsExactly(
      "Starting batch processing.",
      "[action] analyze class HelloWorld in Classes.java",
      "Batch processing: Cancelled!");
    // Missing: [action] analyse class MyClass in Methods.java
  }

  @Test
  void failing_batch_mode_with_empty_file_list_should_not_continue_file_by_file() {
    List<InputFile> inputFiles = List.of();
    List<String> trace = new ArrayList<>();
    BiConsumer<InputFile, JParseResult> action = (inputFile, result) -> trace.add("should not be called ");
    BatchWithException batchWithException = new BatchWithException();
    // exception before the first file
    batchWithException.exceptions.push(new NullPointerException("Boom!"));
    batchWithException.parse(inputFiles, () -> false, new AnalysisProgress(inputFiles.size()), action);
    trace.addAll(logTester.logs());
    logTester.clear();
    assertThat(trace).containsExactly(
      "Unexpected NullPointerException: Boom!");
  }

  @ParameterizedTest
  @EnumSource(JParser.Mode.class)
  void should_preserve_configuration_between_parses(JParser.Mode mode) {
    JParser parser = mode.create(new JavaVersionImpl(17), List.of());
    for (String name : List.of("A", "B")) {
      var tree = parser.parse("17", name + ".java", "record " + name + "(String value) {}");
      assertThat(tree.semanticProblems()).isEmpty();
      ClassTree record = (ClassTree) tree.types().get(0);
      assertThat(record.recordComponents().get(0).symbol().type().is("java.lang.String")).isTrue();
    }
  }

  @Test
  void should_enable_preview() {
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl())).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(8))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(11))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(16))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(17))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(18))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(19))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(20))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(42))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(42, true))).isTrue();

    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromString("1.8"))).isFalse();
    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromString("1.8", "True"))).isTrue();
  }

  static class BatchWithException extends JDTParser.Batch {

    Deque<RuntimeException> exceptions = new LinkedList<>();

    public BatchWithException() {
      super(MAXIMUM_SUPPORTED_JAVA_VERSION, DEFAULT_CLASSPATH, false);
    }

    @Override
    public ASTParser jdtAstParser() {
      if (!exceptions.isEmpty()) {
        throw exceptions.pop();
      }
      return super.jdtAstParser();
    }

  }
}
