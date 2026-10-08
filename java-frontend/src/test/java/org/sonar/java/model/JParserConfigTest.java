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
package org.sonar.java.model;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.TestUtils;
import org.sonar.java.classpath.SourcePath;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.sonar.java.model.JParserConfig.shouldEnablePreviewFlag;

class JParserConfigTest {
  @TempDir
  Path temp;

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.INFO);

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

  @Test
  void a_debug_message_is_logged_when_shouldIgnoreUnnamedModuleForSplitPackage_is_set() {
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList());
    assertThat(logTester.logs()).isEmpty();
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), false);
    assertThat(logTester.logs()).isEmpty();
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), true);
    assertThat(logTester.logs()).containsExactly("The Java analyzer will ignore the unnamed module for split packages.");
  }

  @ParameterizedTest
  @CsvSource({"BATCH,false", "FILE_BY_FILE,false", "BATCH,true"})
  void resolves_dependency_sources_without_analyzing_them_or_using_class_files(JParserConfig.Mode mode, boolean fallback) throws Exception {
    Path transitiveRoot = Files.createDirectories(temp.resolve("c/c"));
    Files.writeString(transitiveRoot.resolve("Base.java"),
      "package c; public class Base<T> { public T identity(T value) { return value; } }");
    Path dependencyRoot = Files.createDirectories(temp.resolve("a/a"));
    Files.writeString(dependencyRoot.resolve("Api.java"), """
      package a;
      @Deprecated public class Api extends c.Base<String> {
        public String choose(String value) { return value; }
        public int choose(int value) { return value; }
      }
      """);
    Path consumer = temp.resolve("B.java");
    Files.writeString(consumer, """
      class B extends a.Api {
        String generic() { return identity("value"); }
        String overload() { return choose("value"); }
      }
      """);

    SourcePath sourcePath = new SourcePath(List.of(temp.resolve("a").toFile(), temp.resolve("c").toFile()), List.of("UTF-8", "UTF-8"));
    var config = fallback ? new JParserConfig.Batch(new JavaVersionImpl(17), List.of(), sourcePath, false) {
      private boolean failNext = true;

      @Override
      public ASTParser astParser() {
        if (failNext) {
          failNext = false;
          throw new IllegalStateException("Force fallback");
        }
        return super.astParser();
      }
    } : mode.create(new JavaVersionImpl(17), List.of(), sourcePath, false);

    InputFile inputFile = TestUtils.inputFile(consumer.toFile());
    List<InputFile> scanned = new ArrayList<>();
    config.parse(List.of(inputFile), () -> false, new AnalysisProgress(1), (file, result) -> {
      scanned.add(file);
      ClassTree type = (ClassTree) assertDoesNotThrow(result::get).types().getFirst();
      assertThat(type.symbol().superClass().fullyQualifiedName()).isEqualTo("a.Api");
      assertThat(type.symbol().superClass().symbol().metadata().isAnnotatedWith("java.lang.Deprecated")).isTrue();
      assertThat(type.symbol().superClass().symbol().superClass().fullyQualifiedName()).isEqualTo("c.Base");
      for (var member : type.members()) {
        var method = (MethodTree) member;
        var returned = (ReturnStatementTree) method.block().body().getFirst();
        var invocation = (MethodInvocationTree) returned.expression();
        assertThat(invocation.methodSymbol().isUnknown()).isFalse();
        assertThat(invocation.symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");
      }
    });
    assertThat(scanned).containsExactly(inputFile);
    try (var files = Files.walk(temp)) {
      assertThat(files.filter(path -> path.toString().endsWith(".class"))).isEmpty();
    }
  }


  @ParameterizedTest
  @EnumSource(JParserConfig.Mode.class)
  void uses_the_source_root_encoding(JParserConfig.Mode mode) throws Exception {
    Path dependencyRoot = Files.createDirectories(temp.resolve("a/a"));
    Files.writeString(dependencyRoot.resolve("Api.java"),
      "package a; public class Api { public static String café() { return null; } }", StandardCharsets.ISO_8859_1);
    Path consumer = Files.writeString(temp.resolve("B.java"), "class B { String value() { return a.Api.café(); } }");
    SourcePath sourcePath = new SourcePath(List.of(temp.resolve("a").toFile()), List.of("ISO-8859-1"));

    var config = mode.create(new JavaVersionImpl(17), List.of(), sourcePath, false);
    List<JParserConfig.Result> results = new ArrayList<>();
    config.parse(List.of(TestUtils.inputFile(consumer.toFile())), () -> false, new AnalysisProgress(1), (file, result) -> results.add(result));

    var invocation = returnedInvocation(results.getFirst());
    assertThat(invocation.methodSymbol().isUnknown()).isFalse();
    assertThat(invocation.methodSymbol().name()).isEqualTo("café");
  }

  @ParameterizedTest
  @EnumSource(JParserConfig.Mode.class)
  void prefers_source_paths_over_binary_classpath_definitions(JParserConfig.Mode mode) throws Exception {
    Path dependencyRoot = Files.createDirectories(temp.resolve("a/a"));
    Path dependency = dependencyRoot.resolve("Api.java");
    Files.writeString(dependency, "package a; public class Api { public static int value() { return 1; } }");

    Path binaries = Files.createDirectory(temp.resolve("classes"));
    assertThat(javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", binaries.toString(), dependency.toString())).isZero();
    Files.writeString(dependency, "package a; public class Api { public static String value() { return null; } }");
    Path consumer = Files.writeString(temp.resolve("B.java"), "class B { Object value() { return a.Api.value(); } }");
    SourcePath sourcePath = new SourcePath(List.of(temp.resolve("a").toFile()), List.of("UTF-8"));
    var config = mode.create(new JavaVersionImpl(17), List.of(binaries.toFile()), sourcePath, false);

    List<JParserConfig.Result> results = new ArrayList<>();
    config.parse(List.of(TestUtils.inputFile(consumer.toFile())), () -> false, new AnalysisProgress(1), (file, result) -> results.add(result));
    assertThat(returnedInvocation(results.getFirst()).symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");
  }

  private static MethodInvocationTree returnedInvocation(JParserConfig.Result result) throws Exception {
    var type = (ClassTree) result.get().types().getFirst();
    var method = (MethodTree) type.members().getFirst();
    return (MethodInvocationTree) ((ReturnStatementTree) method.block().body().getFirst()).expression();
  }

}
