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

import com.intellij.openapi.application.ApplicationManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.ProgressMonitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PsiFrontendBackendTest {
  private static final FrontendConfiguration CONFIGURATION = FrontendConfiguration.from(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false);

  @ParameterizedTest
  @EnumSource(JParserConfig.Mode.class)
  void normal_entry_points_use_selected_backend(JParserConfig.Mode mode) throws IOException {
    var config = mode.create(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of());
    config.parse(List.of(input("A.java", "class A { String value; }")), () -> false, new AnalysisProgress(1), (file, result) -> {
      var tree = tree(result);
      assertThat(tree.sema instanceof JSema).isTrue();
      assertThat(tree.sema.getClassType("java.lang.String").is("java.lang.String")).isTrue();
    });
    try (var parsed = FrontendParser.parse(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), "A.java", "class A {}")) {
      assertThat(parsed.tree().sema instanceof JSema).isTrue();
    }
  }

  @Test
  void batch_uses_decoded_snapshots_and_file_local_semantics() throws IOException {
    var a = input("A.java", "package p; class A { B b; String text = \"é\"; }");
    var b = input("B.java", "package p; class B { String value; }");
    var callbacks = new ArrayList<String>();
    var trees = new ArrayList<JavaTree.CompilationUnitTreeImpl>();
    try (var session = new PsiFrontendBackend().open(CONFIGURATION)) {
      var monitor = new ProgressMonitor(() -> false, new AnalysisProgress(2));
      try {
        session.parseBatch(FrontendBatch.from(List.of(a, b)), monitor, (input, unit) -> {
          try {
            var tree = unit.convert();
            trees.add(tree);
            callbacks.add(input.filename());
            assertThat(tree.sema).isInstanceOf(JSema.class);
            if (input == a) {
              var cls = (ClassTree) tree.types().getFirst();
              var field = (VariableTree) cls.members().getFirst();
              assertThat(field.symbol().type().fullyQualifiedName()).isEqualTo("p.B");
              assertThat(field.symbol().type().symbol().declaration()).isNull();
            }
            assertThat(tree.sema.getClassType("p.Unsupplied").isUnknown()).isTrue();
          } catch (IOException failure) {
            throw new AssertionError(failure);
          }
        });
        assertThat(trees.getFirst().sema.getClassType("p.B").isUnknown()).isFalse();
      } finally {
        monitor.done();
      }
    }
    assertThat(callbacks).containsExactly("A.java", "B.java");
    assertThatThrownBy(() -> trees.getFirst().sema.getClassType("java.lang.Integer"))
      .isInstanceOf(IllegalStateException.class).hasMessage("PSI environment is closed");
    assertThat(ApplicationManager.getApplication()).isNull();
  }

  @Test
  void failed_batch_retries_only_pending_inputs_using_psi_file_mode() throws IOException {
    var files = List.of(input("A.java", "class A {}"), input("B.java", "class B {}"), input("C.java", "class C {}"));
    var opened = new AtomicInteger();
    var callbacks = new ArrayList<String>();
    var config = new JParserConfig.Batch(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false) {
      @Override
      FrontendBackend backend() {
        return configuration -> {
          var delegate = new PsiFrontendBackend().open(configuration);
          if (opened.getAndIncrement() != 0) {
            return delegate;
          }
          return new FrontendSession() {
            {
              own(delegate::close);
            }

            @Override
            JavaTree.CompilationUnitTreeImpl parse(String name, String source) {
              return delegate.parse(name, source);
            }

            @Override
            void parseBatch(FrontendBatch inputs, ProgressMonitor monitor, BiConsumer<InputFile, ParsedUnit> action) {
              delegate.parseBatch(inputs, monitor, (file, unit) -> {
                if (file == files.get(1)) {
                  throw new IllegalStateException("batch failure after A");
                }
                action.accept(file, unit);
              });
            }
          };
        };
      }
    };
    config.parse(files, () -> false, new AnalysisProgress(3), (file, result) -> {
      callbacks.add(file.filename());
      assertThat(tree(result).sema).isInstanceOf(JSema.class);
    });
    assertThat(callbacks).containsExactly("A.java", "B.java", "C.java");
    assertThat(opened).hasValue(3);
    assertThat(ApplicationManager.getApplication()).isNull();
  }

  @Test
  void syntax_failure_still_delivers_later_files_and_disposes_batch() throws IOException {
    var results = new ArrayList<JParserConfig.Result>();
    psiBatch().parse(List.of(input("A.java", "class A { void x( }"), input("B.java", "class B {}")),
      () -> false, new AnalysisProgress(2), (file, result) -> results.add(result));
    assertThat(results).hasSize(2);
    assertThatThrownBy(results.getFirst()::get).isInstanceOf(com.sonar.sslr.api.RecognitionException.class);
    assertThat(tree(results.getLast()).sema).isInstanceOf(JSema.class);
    assertThat(ApplicationManager.getApplication()).isNull();
  }

  @Test
  void cancellation_during_lazy_callback_propagates_without_retry() throws IOException {
    var canceled = new AtomicBoolean();
    var callbacks = new AtomicInteger();
    assertThatThrownBy(() -> psiBatch().parse(List.of(input("A.java", "class A {}"), input("B.java", "class B {}")),
      canceled::get, new AnalysisProgress(2), (file, result) -> {
        callbacks.incrementAndGet();
        canceled.set(true);
        tree(result).sema.getClassType("java.lang.String");
      })).isInstanceOf(OperationCanceledException.class);
    assertThat(callbacks).hasValue(1);
    assertThat(ApplicationManager.getApplication()).isNull();
  }

  @Test
  void cancellation_before_the_first_batch_callback_does_not_fallback() throws IOException {
    var input = input("A.java", "class A {}");
    assertThatThrownBy(() -> psiBatch().parse(List.of(input), () -> true, new AnalysisProgress(1),
      (file, result) -> { throw new AssertionError("Unexpected callback"); })).isInstanceOf(OperationCanceledException.class);
    assertThat(ApplicationManager.getApplication()).isNull();
  }

  @Test
  void file_session_preserves_interruption() {
    try (var session = new PsiFrontendBackend().open(CONFIGURATION)) {
      Thread.currentThread().interrupt();
      try {
        assertThatThrownBy(() -> session.parse("A.java", "class A {}")).isInstanceOf(OperationCanceledException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
      } finally {
        Thread.interrupted();
      }
    }
    assertThat(ApplicationManager.getApplication()).isNull();
  }

  @Test
  void single_frontend_reaches_child_jvm_despite_legacy_selection() throws Exception {
    for (boolean inheritEnvironment : List.of(true, false)) {
      var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "-cp", System.getProperty("java.class.path"), BackendProbe.class.getName()).redirectErrorStream(true);
      process.environment().put("SONAR_JAVA_FRONTEND", inheritEnvironment ? "ecj" : "invalid");
      process.command().add(1, "-Dsonar.java.frontend=ecj");
      var child = process.start();
      try {
        assertThat(child.waitFor(30, TimeUnit.SECONDS)).isTrue();
        String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(child.exitValue()).as(output).isZero();
        assertThat(output).contains("backend=PSI");
      } finally {
        child.destroyForcibly();
      }
    }
  }

  public static class BackendProbe {
    public static void main(String[] args) {
      try (var parsed = FrontendParser.parse(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), "A.java", "class A {}")) {
        if (!(parsed.tree().sema instanceof JSema)) {
          throw new AssertionError("Wrong backend");
        }
        System.out.println("backend=PSI");
      }
      if (ApplicationManager.getApplication() != null) {
        throw new AssertionError("Leaked PSI application");
      }
      System.exit(0);
    }
  }

  private static JParserConfig psiBatch() {
    return new JParserConfig.Batch(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false) {
      @Override
      FrontendBackend backend() {
        return new PsiFrontendBackend();
      }
    };
  }

  private static JavaTree.CompilationUnitTreeImpl tree(JParserConfig.Result result) {
    try {
      return result.get();
    } catch (Exception failure) {
      throw new AssertionError(failure);
    }
  }

  private static InputFile input(String name, String source) throws IOException {
    var file = mock(InputFile.class);
    when(file.filename()).thenReturn(name);
    when(file.absolutePath()).thenReturn(Path.of("target", "psi-execution", name).toAbsolutePath().toString());
    when(file.charset()).thenReturn(StandardCharsets.UTF_8);
    when(file.contents()).thenReturn(source);
    return file;
  }
}
