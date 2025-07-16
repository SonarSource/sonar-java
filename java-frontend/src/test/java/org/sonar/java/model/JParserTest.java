/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import com.sonar.sslr.api.RecognitionException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.RecordPatternTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia.CommentKind;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.java.model.JParser.convertTokenTypeToCommentKind;
import static org.sonar.java.model.JParser.isComment;
import static org.sonar.java.model.JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
import static org.sonar.java.model.JParserConfig.Mode.BATCH;
import static org.sonar.java.model.JParserConfig.Mode.FILE_BY_FILE;
import static org.sonar.java.model.JParserTestUtils.DEFAULT_CLASSPATH;
import static org.sonar.java.model.JParserTestUtils.parse;

class JParserTest {

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.DEBUG);

  @Test
  void should_throw_RecognitionException_in_case_of_syntax_error() {
    // Note that without check for syntax errors will cause IndexOutOfBoundsException
    RecognitionException e1 = assertThrows(RecognitionException.class,
      () -> test("class C"),
      "Parse error at line 1 column 6: Syntax error, insert \"ClassBody\" to complete CompilationUnit");
    assertThat(e1.getLine()).isEqualTo(1);

    // Note that syntax tree will be correct even in presence of this syntax error
    // javac doesn't produce error in this case, however this is not allowed according to JLS 12
    assertThrows(RecognitionException.class,
      () -> test("import a; ; import b;"),
      "Parse error at line 1 column 10: Syntax error on token \";\", delete this token");
  }

  @Test
  void should_recover_if_parser_fails() {
    String version = "12";
    List<File> classpath = Collections.singletonList(new File("unknownFile"));
    ASTParser astParser = FILE_BY_FILE.create(JavaVersionImpl.fromString(version), classpath).astParser();
    assertThrows(RecognitionException.class, () -> JParser.parse(astParser, version, "A", "class A { }"));
  }

  @Test
  void should_throw_RecognitionException_in_case_of_lexical_error() {
    // Note that without check for errors will cause InvalidInputException
    assertThrows(RuntimeException.class, () -> testExpression("''"), "Parse error at line 1 column 30: Invalid character constant");
  }

  @Test
  void err() {
    // ASTNode.METHOD_DECLARATION with flag ASTNode.MALFORMED: missing return type
    assertThrows(IndexOutOfBoundsException.class, () -> test("interface Foo { public foo(); // comment\n }"));
  }

  @Test
  void unknown_types_are_collected() {
    // import org.foo missing, type Bar unknown
    CompilationUnitTree cut = test("import org.foo.Bar;\n class Foo {\n void foo(Bar b) {}\n }\n");
    assertThat(((CompilationUnitTreeImpl) cut).sema.undefinedTypes.stream().map(Object::toString))
      .containsExactlyInAnyOrder("The import org.foo cannot be resolved", "Bar cannot be resolved to a type");
  }

  @Test
  void warnings_are_collected() {
    // import org.foo missing, type Bar unknown
    CompilationUnitTree cut = test("import java.util.List;\n import java.util.ArrayList;\n class Foo {\n void foo(Bar b) {}\n }\n");
    assertThat(((CompilationUnitTreeImpl) cut).sema.undefinedTypes.stream().map(Object::toString))
      .containsExactlyInAnyOrder("Bar cannot be resolved to a type");
  }

  @Test
  void should_rethrow_when_consumer_throws() {
    RuntimeException expected = new RuntimeException();
    BiConsumer<InputFile, JParserConfig.Result> consumer = (inputFile, result) -> {
      throw expected;
    };
    InputFile inputFile = mock(InputFile.class);
    doReturn("/tmp/Example.java")
      .when(inputFile).absolutePath();

    Set<InputFile> inputFiles = Collections.singleton(inputFile);
    JParserConfig config = JParserConfig.Mode.FILE_BY_FILE.create(MAXIMUM_SUPPORTED_JAVA_VERSION, Collections.emptyList());

    AnalysisProgress analysisProgress = new AnalysisProgress(inputFiles.size());
    RuntimeException actual = assertThrows(RuntimeException.class, () -> config.parse(inputFiles, () -> false,
      analysisProgress, consumer));
    assertSame(expected, actual);
  }

  @Test
  void consumer_should_receive_exceptions_thrown_during_parsing() throws Exception {
    List<JParserConfig.Result> results = new ArrayList<>();
    BiConsumer<InputFile, JParserConfig.Result> consumer = (inputFile, result) -> results.add(result);

    InputFile inputFile = mock(InputFile.class);
    doReturn("/tmp/Example.java")
      .when(inputFile).absolutePath();
    Mockito
      .doThrow(IOException.class)
      .when(inputFile).contents();

    FILE_BY_FILE
      .create(MAXIMUM_SUPPORTED_JAVA_VERSION, Collections.emptyList())
      .parse(Collections.singleton(inputFile), () -> false, new AnalysisProgress(1), consumer);

    JParserConfig.Result result = results.get(0);
    assertThrows(IOException.class, result::get);
  }

  @Test
  void consumer_should_receive_exceptions_thrown_during_parsing_as_batch() throws Exception {
    List<JParserConfig.Result> results = new ArrayList<>();
    BiConsumer<InputFile, JParserConfig.Result> consumer = (inputFile, result) -> results.add(result);
    InputFile inputFile = spy(TestUtils.inputFile("src/test/files/metrics/Classes.java"));
    when(inputFile.contents()).thenThrow(IOException.class);

    BATCH
      .create(MAXIMUM_SUPPORTED_JAVA_VERSION, Collections.emptyList())
      .parse(Collections.singleton(inputFile), () -> false, new AnalysisProgress(1), consumer);

    JParserConfig.Result result = results.get(0);
    assertThrows(IOException.class, result::get);
  }

  @Test
  void should_propagate_exception_raised_by_batch_parsing() {
    NullPointerException expected = new NullPointerException("");
    BiConsumer<InputFile, JParserConfig.Result> consumerThrowing = (inputFile, result) -> {
      throw expected;
    };
    List<InputFile> inputFiles = Collections.singletonList(TestUtils.inputFile("src/test/files/metrics/Classes.java"));
    JParserConfig config = BATCH.create(MAXIMUM_SUPPORTED_JAVA_VERSION, DEFAULT_CLASSPATH);
    AnalysisProgress analysisProgress = new AnalysisProgress(inputFiles.size());
    NullPointerException actual = assertThrows(NullPointerException.class, () -> config.parse(inputFiles, () -> false,
      analysisProgress, consumerThrowing));

    assertSame(expected, actual);
  }

  @Test
  void eof() {
    {
      CompilationUnitTree t = test("");
      assertEquals("", t.eofToken().text());
      assertEquals(Range.at(1, 1, 1, 1), t.eofToken().range());
    }
    {
      CompilationUnitTree t = test(" ");
      assertEquals("", t.eofToken().text());
      assertEquals(Range.at(1, 2, 1, 2), t.eofToken().range());
    }
    {
      CompilationUnitTree t = test(" \n");
      assertEquals("", t.eofToken().text());
      assertEquals(Range.at(2, 1, 2, 1), t.eofToken().range());
    }
  }

  @Test
  void declaration_enum() {
    CompilationUnitTree cu = test("enum E { C }");
    ClassTree t = (ClassTree) cu.types().get(0);
    EnumConstantTree c = (EnumConstantTree) t.members().get(0);
    assertSame(
      c.simpleName(),
      c.initializer().identifier()
    );
  }

  @Test
  void processEnumConstantDeclaration_sets_enum_initializer_following_a_markdown_comment_as_next_token() {
    CompilationUnitTree cu = test("enum E { C /// comment before initializer\n (3); E(int a) {} }");
    ClassTree t = (ClassTree) cu.types().get(0);
    EnumConstantTree c = (EnumConstantTree) t.members().get(0);
    assertThat(c.initializer().arguments().openParenToken()).isNotNull();
    assertThat(c.initializer().arguments()).hasSize(1);
  }

  @Test
  void statement_variable_declaration() {
    CompilationUnitTree t = test("class C { void m() { int a, b; } }");
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    VariableTree s1 = (VariableTree) s.body().get(0);
    VariableTree s2 = (VariableTree) s.body().get(1);
    assertSame(s1.type(), s2.type());
    assertThat(s1.simpleName().isUnnamedVariable()).isFalse();
    assertThat(s2.simpleName().isUnnamedVariable()).isFalse();
  }

  @Test
  void parse_static_method_invocation_on_a_conditional_expression_with_null_literal_on_the_else_operand() {
    List<File> classpath = List.of();
    assertDoesNotThrow(() -> JParserTestUtils.parse("Reproducer.java", """
      package checks;

      public class Reproducer {
        void foo(Reproducer o) {
          (o != null ? o : null).bar();
        }
        static void bar() {
        }
      }
      """, classpath));
  }

  @Test
  void unnamed_variable_in_a_local_variable_declaration_statement() {
    CompilationUnitTree t = test("""
      class C {
        void m(java.util.List<String> list) {
          String _ = list.remove(0);
        }
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    VariableTree variableTree = (VariableTree) s.body().get(0);
    assertThat(variableTree.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");
    assertThat(variableTree.simpleName().name()).isEqualTo("_");
    assertThat(variableTree.simpleName().isUnnamedVariable()).isTrue();
  }

  @Test
  void unnamed_variable_in_the_resource_of_a_try_with_resources_statement() {
    CompilationUnitTree t = test("""
      import java.nio.file.Files;

      class C {
        void m() throws java.io.IOException {
          try (var _ = new java.io.FileOutputStream("foo")) {
          }
        }
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    TryStatementTree tryStatementTree = (TryStatementTree) s.body().get(0);
    VariableTree variableTree = (VariableTree) tryStatementTree.resourceList().get(0);
    assertThat(variableTree.type().kind()).isEqualTo(Tree.Kind.VAR_TYPE);
    assertThat(variableTree.type().symbolType().fullyQualifiedName()).isEqualTo("java.io.FileOutputStream");
    assertThat(variableTree.simpleName().name()).isEqualTo("_");
    assertThat(variableTree.simpleName().isUnnamedVariable()).isTrue();
  }

  @Test
  void unnamed_variable_in_header_of_a_basic_for_loop() {
    CompilationUnitTree t = test("""
      class C {
        void m() {
          for (int i = 0, _ = sideEffect(); i < 10; i++) { }
        }
        int sideEffect() {
          return -1;
        }
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    ForStatementTree forStatementTree = (ForStatementTree) s.body().get(0);

    VariableTree variable1Tree = (VariableTree) forStatementTree.initializer().get(0);
    assertThat(variable1Tree.type().symbolType().fullyQualifiedName()).isEqualTo("int");
    assertThat(variable1Tree.simpleName().name()).isEqualTo("i");
    assertThat(variable1Tree.simpleName().isUnnamedVariable()).isFalse();

    VariableTree variable2Tree = (VariableTree) forStatementTree.initializer().get(1);
    assertThat(variable2Tree.type().symbolType().fullyQualifiedName()).isEqualTo("int");
    assertThat(variable2Tree.simpleName().name()).isEqualTo("_");
    assertThat(variable2Tree.simpleName().isUnnamedVariable()).isTrue();
  }

  @Test
  void unnamed_variable_in_header_of_an_enhanced_for_loop() {
    CompilationUnitTree t = test("""
      class C {
        int size(List<String> names) {
          int s = 0;
          for (String _ : names) { s++; }
          return s;
        }
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    ForEachStatement forEachStatement = (ForEachStatement) s.body().get(1);
    VariableTree variableTree = forEachStatement.variable();
    assertThat(variableTree.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");
    assertThat(variableTree.simpleName().name()).isEqualTo("_");
    assertThat(variableTree.simpleName().isUnnamedVariable()).isTrue();
  }

  @Test
  void unnamed_variable_in_an_exception_parameter_of_a_catch_block() {
    CompilationUnitTree t = test("""
      class C {
        int parse(String s) {
          try {
            return Integer.parseInt(s);
          } catch (NumberFormatException _) {
            return -1;
          }
        }
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    TryStatementTree tryStatementTree = (TryStatementTree) s.body().get(0);
    CatchTree catchTree = tryStatementTree.catches().get(0);
    VariableTree variableTree = catchTree.parameter();
    assertThat(variableTree.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.NumberFormatException");
    assertThat(variableTree.simpleName().name()).isEqualTo("_");
    assertThat(variableTree.simpleName().isUnnamedVariable()).isTrue();
  }

  @Test
  void unnamed_variable_in_a_formal_parameter_of_a_lambda_expression() {
    CompilationUnitTree t = test("""
      class C {
        java.util.function.Function<String, String> lambda() {
          return _ -> "NODATA";
        }
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    ReturnStatementTree returnStatementTree = (ReturnStatementTree) s.body().get(0);
    LambdaExpressionTree lambdaExpressionTree = (LambdaExpressionTree) returnStatementTree.expression();
    VariableTree variableTree = lambdaExpressionTree.parameters().get(0);
    assertThat(variableTree.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");
    assertThat(variableTree.simpleName().name()).isEqualTo("_");
    assertThat(variableTree.simpleName().isUnnamedVariable()).isTrue();
  }

  @Test
  void unnamed_pattern_variable() {
    CompilationUnitTree t = test("""
      class C {
        int m(Object o) {
          return switch (o) {
            case Box(var /*comment*/_) -> 1;
            default -> 0;
          };
        }
        record Box(int value) {}
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    ReturnStatementTree returnStatementTree = (ReturnStatementTree) s.body().get(0);
    SwitchExpressionTree switchExpressionTree = (SwitchExpressionTree) returnStatementTree.expression();
    CaseGroupTree caseGroupTree = switchExpressionTree.cases().get(0);
    CaseLabelTree caseLabelTree = caseGroupTree.labels().get(0);
    RecordPatternTree recordPatternTree = (RecordPatternTree) caseLabelTree.expressions().get(0);
    assertThat(recordPatternTree.symbolType().fullyQualifiedName()).isEqualTo("C$Box");
    TypePatternTree patternTree = (TypePatternTree) recordPatternTree.patterns().get(0);
    VariableTree variableTree = patternTree.patternVariable();
    assertThat(variableTree.type().symbolType().fullyQualifiedName()).isEqualTo("int");
    assertThat(variableTree.simpleName().name()).isEqualTo("_");
    assertThat(variableTree.simpleName().symbol()).isEqualTo(Symbol.UNKNOWN_SYMBOL);
    assertThat(variableTree.simpleName().isUnnamedVariable()).isTrue();
  }


  @Test
  void unnamed_pattern() {
    CompilationUnitTree t = test("""
      class C {
        int m(Object o) {
          return switch (o) {
            case Box(_) -> 1;
            default -> 0;
          };
        }
        record Box(int value) {}
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    ReturnStatementTree returnStatementTree = (ReturnStatementTree) s.body().get(0);
    SwitchExpressionTree switchExpressionTree = (SwitchExpressionTree) returnStatementTree.expression();
    CaseGroupTree caseGroupTree = switchExpressionTree.cases().get(0);
    CaseLabelTree caseLabelTree = caseGroupTree.labels().get(0);
    RecordPatternTree recordPatternTree = (RecordPatternTree) caseLabelTree.expressions().get(0);
    assertThat(recordPatternTree.symbolType().name()).isEqualTo("Box");
    assertThat(recordPatternTree.symbolType().fullyQualifiedName()).isEqualTo("C$Box");
    TypePatternTree patternTree = (TypePatternTree) recordPatternTree.patterns().get(0);
    VariableTree variableTree = patternTree.patternVariable();
    assertThat(variableTree.symbol().name()).isEqualTo("_");
    assertThat(variableTree.symbol().type().fullyQualifiedName()).isEqualTo("int");
    assertThat(variableTree.type().symbolType().fullyQualifiedName()).isEqualTo("int");
    assertThat(variableTree.simpleName().name()).isEqualTo("_");
    assertThat(variableTree.simpleName().isUnnamedVariable()).isTrue();
  }

  @Test
  void test_first_index_of_tokens_in_eclipse_ast() {
    String version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION.effectiveJavaVersionAsString();
    String unitName = "C.java";
    String source = "class C { }";

    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    JavaCore.setComplianceOptions(version, JavaCore.getOptions());
    astParser.setResolveBindings(true);
    astParser.setUnitName(unitName);
    astParser.setSource(source.toCharArray());
    CompilationUnit compilationUnit = (CompilationUnit) astParser.createAST(null);
    TokenManager tokenManager = JParser.createTokenManager(version, unitName, source);

    assertThat(JParser.firstIndexIn(tokenManager, compilationUnit, TerminalToken.TokenNameIdentifier, TerminalToken.TokenNameLBRACE)).isEqualTo(1);
    assertThat(JParser.firstIndexIn(tokenManager, compilationUnit, TerminalToken.TokenNameLBRACE, TerminalToken.TokenNameIdentifier)).isEqualTo(1);
    assertThat(JParser.firstIndexIn(tokenManager, compilationUnit, TerminalToken.TokenNameRBRACE, TerminalToken.TokenNameLBRACE)).isEqualTo(2);
    assertThatThrownBy(() -> JParser.firstIndexIn(tokenManager, compilationUnit, TerminalToken.TokenNamebreak, TerminalToken.TokenNameconst))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to find token TokenNamebreak or TokenNameconst in the tokens of a org.eclipse.jdt.core.dom.CompilationUnit");
  }

  @Test
  void test_comment_tokens() {
    String version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION.effectiveJavaVersionAsString();
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
    TokenManager tokens = JParser.createTokenManager(version, unitName, source);

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
    assertThat(token5.isComment()).isFalse(); // JDT issue https://github.com/eclipse-jdt/eclipse.jdt.core/issues/3914
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
  void dont_include_running_VM_Bootclasspath_if_jvm_rt_jar_already_provided_in_classpath(@TempDir Path tempFolder) throws IOException {
    VariableTree s1 = parseAndGetVariable("class C { void m() { String a; } }");
    assertThat(s1.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");

    Path fakeRt = tempFolder.resolve("rt.jar");
    Files.createFile(fakeRt);
    s1 = parseAndGetVariable("class C { void m() { String a; } }", fakeRt.toFile());
    assertThat(s1.type().symbolType().fullyQualifiedName()).isEqualTo("Recovered#typeBindingLString;0");
  }

  @ParameterizedTest
  @ValueSource(ints = {8, 9, 17})
  void dont_include_running_VM_Bootclasspath_if_android_runtime_already_provided_in_classpath(int javaVersion) throws IOException {
    JavaVersion androidVersion = new JavaVersionImpl(javaVersion);
    String source = "class C { void m() { String a; Integer b; } }";
    VariableTree a = parseAndGetVariable(source, androidVersion);
    VariableTree b = (VariableTree) ((BlockTree) a.parent()).body().get(1);
    assertThat(a.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");
    assertThat(b.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.Integer");

    Path fakeAndroidSdk = Path.of("src", "test", "resources", "android.jar").toRealPath();
    a = parseAndGetVariable(source, androidVersion, fakeAndroidSdk.toFile());
    b = (VariableTree) ((BlockTree) a.parent()).body().get(1);
    assertThat(a.type().symbolType().fullyQualifiedName()).isEqualTo("Recovered#typeBindingLString;0");
    assertThat(b.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.Integer");
  }

  @Test
  void dont_include_running_VM_Bootclasspath_if_jvm_jrt_fs_jar_already_provided_in_classpath() throws IOException {
    // Don't use JUnit TempFolder as the fake jrt-fs.jar remains locked on Windows because of the JrtFsLoader URLClassloader
    Path tempFolder = Files.createTempDirectory("sonarjava");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(tempFolder.toFile())));

    VariableTree s1 = parseAndGetVariable("class C { void m() { String a; } }");
    assertThat(s1.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");

    Path fakeJrtFs = createFakeJrtFs(tempFolder);

    File fakeJrtFsFile = fakeJrtFs.toFile();
    Throwable expected = assertThrows(Throwable.class, () -> parseAndGetVariable("class C { void m() { String a; } }", fakeJrtFsFile));
    String javaVersion = System.getProperty("java.version");
    if (javaVersion != null && javaVersion.startsWith("1.8")) {
      assertThat(expected).hasCauseExactlyInstanceOf(ProviderNotFoundException.class).hasRootCauseMessage("Provider \"jrt\" not found");
    } else {
      assertThat(expected).isInstanceOf(ClassFormatError.class).hasMessage("Truncated class file");
    }
  }

  @Test
  void test_parse_file_by_file() throws Exception {
    List<InputFile> inputFiles = Arrays.asList(TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java"));
    List<JParserConfig.Result> results = new ArrayList<>();
    List<InputFile> inputFilesProcessed = new ArrayList<>();
    FILE_BY_FILE
      .create(MAXIMUM_SUPPORTED_JAVA_VERSION, DEFAULT_CLASSPATH)
      .parse(inputFiles, () -> false, new AnalysisProgress(inputFiles.size()), (inputFile, result) -> {
        results.add(result);
        inputFilesProcessed.add(inputFile);
      });

    assertResultsOfParsing(results, inputFilesProcessed);
  }

  @Test
  void test_parse_as_batch() throws Exception {
    List<InputFile> inputFiles = Arrays.asList(TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java"));
    List<JParserConfig.Result> results = new ArrayList<>();
    List<InputFile> inputFilesProcessed = new ArrayList<>();
    BATCH
      .create(MAXIMUM_SUPPORTED_JAVA_VERSION, DEFAULT_CLASSPATH)
      .parse(inputFiles, () -> false, new AnalysisProgress(inputFiles.size()), (inputFile, result) -> {
        results.add(result);
        inputFilesProcessed.add(inputFile);
      });

    assertResultsOfParsing(results, inputFilesProcessed);
  }

  @Test
  void failing_batch_mode_should_continue_file_by_file() {
    List<InputFile> inputFiles = Arrays.asList(
      TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java"));
    List<String> trace = new ArrayList<>();
    BiConsumer<InputFile, JParserConfig.Result> action = (inputFile, result) -> {
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
    BiConsumer<InputFile, JParserConfig.Result> doNothingAction = (inputFile, result) -> {};
    JParserConfig config = spy(JParserConfig.Mode.BATCH
      .create(MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false));
    // Return a lazy ASTParser that do nothing to ensure that we have not analyzed files
    when(config.astParser()).thenReturn(mock(ASTParser.class));

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
    BiConsumer<InputFile, JParserConfig.Result> action = (inputFile, result) -> {
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

    JParserConfig config = BATCH
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
  void test_no_line_continuation_in_text_blocks() {
    CompilationUnitTree tree = parse(new File("src/test/files/metrics/TextBlock.java"));
    var tokens = tokens((JavaTree) tree);
    assertThat(tokens)
      .extracting(token -> token.line() + "," + token.column() + ": " + token.text())
      .containsExactly(
        "1,0: class",
        "1,6: TextBlock",
        "1,16: {",
        "2,2: String",
        "2,9: a",
        "2,11: =",
        "2,13: \"\"\"\n    Hello,\n    world!\n    \"\"\"",
        "5,7: ;",
        "6,0: }",
        "6,1: ");
  }

  @Test
  void test_line_continuation_in_text_blocks() {
    CompilationUnitTree tree = parse(new File("src/test/files/metrics/TextBlockWithLineContinuation.java"));
    var tokens = tokens((JavaTree) tree);
    assertThat(tokens)
      .extracting(token -> token.line() + "," + token.column() + ": " + token.text())
      .containsExactly(
        "1,0: class",
        "1,6: TextBlock",
        "1,16: {",
        "2,2: String",
        "2,9: a",
        "2,11: =",
        "2,13: \"\"\"\n    Hello,\\\n    world!\n    \"\"\"",
        "5,7: ;",
        "6,2: String",
        "6,9: b",
        "6,11: =",
        "6,13: \"\"\"\n    Goodbye,\\\n    cruel \\\n    world!\n    \"\"\"",
        "10,7: ;",
        "11,0: }",
        "11,1: ");
  }

  public static List<InternalSyntaxToken> tokens(JavaTree tree) {
    var tokens = new ArrayList<InternalSyntaxToken>();
    if (tree instanceof InternalSyntaxToken) {
      tokens.add((InternalSyntaxToken) tree);
    } else {
      for (var child : tree.getChildren()) {
        tokens.addAll(tokens((JavaTree) child));
      }
    }
    return tokens;
  }

  @Test
  void failing_batch_mode_with_empty_file_list_should_not_continue_file_by_file() {
    List<InputFile> inputFiles = List.of();
    List<String> trace = new ArrayList<>();
    BiConsumer<InputFile, JParserConfig.Result> action = (inputFile, result) -> trace.add("should not be called ");
    BatchWithException batchWithException = new BatchWithException();
    // exception before the first file
    batchWithException.exceptions.push(new NullPointerException("Boom!"));
    batchWithException.parse(inputFiles, () -> false, new AnalysisProgress(inputFiles.size()), action);
    trace.addAll(logTester.logs());
    logTester.clear();
    assertThat(trace).containsExactly(
      "Unexpected NullPointerException: Boom!");
  }

  static class BatchWithException extends JParserConfig.Batch {

    Deque<RuntimeException> exceptions = new LinkedList<>();

    public BatchWithException() {
      super(MAXIMUM_SUPPORTED_JAVA_VERSION, DEFAULT_CLASSPATH, false);
    }

    @Override
    public ASTParser astParser() {
      if (!exceptions.isEmpty()) {
        throw exceptions.pop();
      }
      return super.astParser();
    }

  }

  private void assertResultsOfParsing(List<JParserConfig.Result> results, List<InputFile> inputFilesProcessed) throws Exception {
    assertThat(inputFilesProcessed).hasSize(2);
    assertThat(inputFilesProcessed.get(0).filename()).isEqualTo("Classes.java");
    assertThat(inputFilesProcessed.get(1).filename()).isEqualTo("Methods.java");

    assertThat(results).hasSize(2);
    List<Tree> result1Children = results.get(0).get().children();
    List<Tree> result2Children = results.get(1).get().children();

    assertThat(result1Children).hasSize(5);
    assertThat(result2Children).hasSize(6);
    assertThat(((ClassTreeImpl) result1Children.get(0)).members().get(0)).isInstanceOf(MethodTree.class);
    assertThat(((ClassTreeImpl) result2Children.get(0)).members()).isNotEmpty().allMatch(m -> m instanceof MethodTree);
  }

  @Test
  void test_is_canceled_is_called_before_each_action_file_by_file() {
    List<InputFile> inputFiles = Arrays.asList(TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java"));
    BiConsumer<InputFile, JParserConfig.Result> action = spy(new BiConsumer<InputFile, JParserConfig.Result>() {
      @Override
      public void accept(InputFile inputFile, JParserConfig.Result result) {
        // Do nothing
      }
    });
    BooleanSupplier isCanceled = spy(new BooleanSupplier() {
      @Override
      public boolean getAsBoolean() {
        return false;
      }
    });

    FILE_BY_FILE
      .create(MAXIMUM_SUPPORTED_JAVA_VERSION, DEFAULT_CLASSPATH)
      .parse(inputFiles, isCanceled, new AnalysisProgress(inputFiles.size()), action);

    InOrder inOrder = inOrder(action, isCanceled);
    inOrder.verify(isCanceled).getAsBoolean();
    inOrder.verify(action).accept(any(), any());
    inOrder.verify(isCanceled).getAsBoolean();
    inOrder.verify(action).accept(any(), any());
  }

  @Test
  void test_is_canceled_is_called_file_by_file() {
    List<InputFile> inputFiles = Arrays.asList(TestUtils.inputFile("src/test/files/metrics/Classes.java"),
      TestUtils.inputFile("src/test/files/metrics/Methods.java"));
    List<JParserConfig.Result> results = new ArrayList<>();
    FILE_BY_FILE
      .create(MAXIMUM_SUPPORTED_JAVA_VERSION, DEFAULT_CLASSPATH)
      .parse(inputFiles, () -> true, new AnalysisProgress(inputFiles.size()), (inputFile, result) -> results.add(result));

    assertThat(results).isEmpty();
  }

  @Test
  void for_statement_should_support_array_types_from_variable_declaration_fragment() {
    CompilationUnitTree unit = parse("class A {void foo(){for(int a[];;){}}}");
    ForStatementTree forStatement = (ForStatementTree) ((MethodTree) ((ClassTree) unit.types().get(0)).members().get(0)).block().body().get(0);
    VariableTree variableTree = (VariableTree) forStatement.initializer().get(0);
    assertThat(variableTree.type().kind()).isEqualTo(Tree.Kind.ARRAY_TYPE);
    assertThat(( (ArrayTypeTree) variableTree.type()).openBracketToken()).isNotNull();
  }

  @Test
  void for_statement_should_support_array_types_from_variable_declaration_fragment_with_initializer() {
    CompilationUnitTree unit = parse("class A {void foo(){for(int a[] = new int[0];;){}}}");
    ForStatementTree forStatement = (ForStatementTree) ((MethodTree) ((ClassTree) unit.types().get(0)).members().get(0)).block().body().get(0);
    VariableTree variableTree = (VariableTree) forStatement.initializer().get(0);
    assertThat(variableTree.type().kind()).isEqualTo(Tree.Kind.ARRAY_TYPE);
    assertThat(( (ArrayTypeTree) variableTree.type()).openBracketToken()).isNotNull();
  }

  @Test
  void try_statement_should_support_array_types_from_variable_declaration_fragment() {
    CompilationUnitTree unit = parse("class A {void foo(){try(int a[] = new int[0]){}}}");
    TryStatementTree tryStatementTree = (TryStatementTree) ((MethodTree) ((ClassTree) unit.types().get(0)).members().get(0)).block().body().get(0);
    VariableTree variableTree = (VariableTree) tryStatementTree.resourceList().get(0);
    assertThat(variableTree.type().kind()).isEqualTo(Tree.Kind.ARRAY_TYPE);
    assertThat(( (ArrayTypeTree) variableTree.type()).openBracketToken()).isNotNull();
  }

  private Path createFakeJrtFs(Path tempFolder) throws IOException {
    // We have to put a fake JrtFileSystemProvider in the JAR to make JrtFsLoader crash and by this way verify that this is truely our JAR
    // that was loaded
    Path fakeJrtFSProviderClass = tempFolder.resolve("jdk/internal/jrtfs/JrtFileSystemProvider.class");
    Files.createDirectories(fakeJrtFSProviderClass.getParent());
    Files.createFile(fakeJrtFSProviderClass);

    Path fakeJrtFs = tempFolder.resolve("lib/jrt-fs.jar");
    Files.createDirectories(fakeJrtFs.getParent());

    // Starting from 3.31.0, ECJ requires the presence of a release file to parse
    Path fakeRelease = tempFolder.resolve("release");
    Files.createFile(fakeRelease);

    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    try (JarOutputStream target = new JarOutputStream(Files.newOutputStream(fakeJrtFs), manifest)) {
      add(tempFolder.resolve("jdk").toFile(), tempFolder.toFile(), target);
    }
    return fakeJrtFs;
  }

  // https://stackoverflow.com/a/59351837/534773
  private static void add(File source, File baseDir, JarOutputStream target) {
    BufferedInputStream in = null;

    try {
      if (!source.exists()) {
        throw new IOException("Source directory is empty");
      }
      if (source.isDirectory()) {
        // For Jar entries, all path separates should be '/'(OS independent)
        String entryName = baseDir.toPath().relativize(source.toPath()).toFile().getPath().replace("\\", "/");
        if (!entryName.isEmpty()) {
          if (!entryName.endsWith("/")) {
            entryName += "/";
          }
          JarEntry entry = new JarEntry(entryName);
          entry.setTime(source.lastModified());
          target.putNextEntry(entry);
          target.closeEntry();
        }
        for (File nestedFile : source.listFiles()) {
          add(nestedFile, baseDir, target);
        }
        return;
      }

      String entryName = baseDir.toPath().relativize(source.toPath()).toFile().getPath().replace("\\", "/");
      JarEntry entry = new JarEntry(entryName);
      entry.setTime(source.lastModified());
      target.putNextEntry(entry);
      in = new BufferedInputStream(new FileInputStream(source));

      byte[] buffer = new byte[1024];
      while (true) {
        int count = in.read(buffer);
        if (count == -1) {
          break;
        }
        target.write(buffer, 0, count);
      }
      target.closeEntry();
    } catch (Exception ignored) {
      // ignored exception
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ignored) {
          throw new RuntimeException(ignored);
        }
      }
    }
  }

  private VariableTree parseAndGetVariable(String code, File... classpath) {
    return parseAndGetVariable(code, JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, classpath);
  }

  private VariableTree parseAndGetVariable(String code, JavaVersion javaVersion, File... classpath) {
    CompilationUnitTree t = JParserTestUtils.parse("Foo.java", code, Arrays.asList(classpath), javaVersion);
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    return (VariableTree) s.body().get(0);
  }

  private static void testExpression(String expression) {
    test("class C { Object m() { return " + expression + " ; } }");
  }

  private static CompilationUnitTree test(String source) {
    return JParserTestUtils.parse(source);
  }

}
