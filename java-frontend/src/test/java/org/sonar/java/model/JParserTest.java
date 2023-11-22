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
import org.eclipse.jdt.core.dom.ASTParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.java.model.JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
import static org.sonar.java.model.JParserConfig.Mode.BATCH;
import static org.sonar.java.model.JParserConfig.Mode.FILE_BY_FILE;
import static org.sonar.java.model.JParserTestUtils.DEFAULT_CLASSPATH;
import static org.sonar.java.model.JParserTestUtils.parse;

class JParserTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

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
    InputFile inputFile = Mockito.mock(InputFile.class);
    Mockito.doReturn("/tmp/Example.java")
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

    InputFile inputFile = Mockito.mock(InputFile.class);
    Mockito
      .doReturn("/tmp/Example.java")
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
  void statement_variable_declaration() {
    CompilationUnitTree t = test("class C { void m() { int a, b; } }");
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    VariableTree s1 = (VariableTree) s.body().get(0);
    VariableTree s2 = (VariableTree) s.body().get(1);
    assertSame(s1.type(), s2.type());
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

  @Test
  void dont_include_running_VM_Bootclasspath_if_android_runtime_already_provided_in_classpath(@TempDir Path tempFolder) throws IOException {
    VariableTree s1 = parseAndGetVariable("class C { void m() { String a; } }");
    assertThat(s1.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");

    Path fakeAndroidSdk = tempFolder.resolve("android.jar");
    Files.createFile(fakeAndroidSdk);
    s1 = parseAndGetVariable("class C { void m() { String a; } }", fakeAndroidSdk.toFile());
    assertThat(s1.type().symbolType().fullyQualifiedName()).isEqualTo("Recovered#typeBindingLString;0");
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
  void failing_batch_mode_should_continue_file_by_file() throws Exception {
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
      "[action] handle NullPointerException: Boom!",
      "Fallback to file by file analysis for 2 files",
      "[action] analyse class HelloWorld in Classes.java",
      "[action] analyse class MyClass in Methods.java");
  }

  @Test
  void failing_batch_mode_should_stop_file_by_file_when_cancelled() throws Exception {
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
        trace.add("[action] analyse class " + ((ClassTree)tree.types().get(0)).simpleName().name() + " in " + inputFile.filename());
        // cancel analysis after first file
        isCanceled.set(true);
      } catch (Exception e) {
        trace.add("[action] handle " + e.getClass().getSimpleName() + ": " + e.getMessage());
      }
    };
    BatchWithException batchWithException = new BatchWithException();
    // exception before the first file
    batchWithException.exceptions.push(new NullPointerException("Boom!"));
    batchWithException.parse(inputFiles, isCanceled::get, new AnalysisProgress(inputFiles.size()), action);
    trace.addAll(logTester.logs());
    logTester.clear();
    assertThat(trace).containsExactly(
      "[action] handle NullPointerException: Boom!",
      "Fallback to file by file analysis for 2 files",
      "[action] analyse class HelloWorld in Classes.java");
    // Missing Methods.java because of cancellation
  }

  @Test
  void failing_batch_mode_with_empty_file_list_should_not_continue_file_by_file() throws Exception {
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
      "Unexpected java.lang.NullPointerException: Boom!");
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
  void test_is_canceled_is_called_before_each_action_file_by_file() throws Exception {
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

    InOrder inOrder = Mockito.inOrder(action, isCanceled);
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
    CompilationUnitTree t = JParserTestUtils.parse("Foo.java", code, Arrays.asList(classpath));
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
