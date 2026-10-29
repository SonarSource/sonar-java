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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.ProgressMonitor;
import org.sonar.java.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class FrontendLifecycleTest {

  private static final FrontendConfiguration CONFIGURATION = FrontendConfiguration.from(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false);
  private static final InputFile INPUT = TestUtils.inputFile("src/test/files/metrics/Classes.java");

  @Test
  void cleanup_is_canonical_idempotent_and_attempts_every_resource() {
    List<String> calls = new ArrayList<>();
    RuntimeException first = new IllegalStateException("first");
    Error second = new AssertionError("second");
    FrontendResources resources = new FrontendResources();
    Runnable raw = () -> calls.add("once");
    Runnable handle = resources.own(raw);
    assertThat(resources.own(raw)).isSameAs(handle).hasSameHashCodeAs(handle);
    assertThat(new FrontendResources().own(raw)).isEqualTo(handle);
    assertThat(handle).isNotEqualTo(resources.own(() -> calls.add("other"))).isNotEqualTo(null).isNotEqualTo("other");
    resources.own(() -> { calls.add("first"); throw first; });
    resources.own(() -> { calls.add("second"); throw second; });
    resources.own(() -> calls.add("last"));
    handle.run();
    assertThatThrownBy(resources::close).isSameAs(first).hasSuppressedException(second);
    resources.close();
    handle.run();
    assertThat(calls).containsExactly("once", "other", "first", "second", "last");
    assertThatThrownBy(() -> resources.own(raw)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void parse_failure_preserves_primary_and_suppresses_cleanup_failure() {
    RuntimeException primary = new IllegalArgumentException("parse");
    Error secondary = new AssertionError("cleanup");
    StubSession session = new StubSession();
    session.failure = primary;
    session.own(() -> { throw secondary; });
    assertThatThrownBy(() -> FrontendParser.parse(configuration -> session, CONFIGURATION, "A.java", ""))
      .isSameAs(primary).hasSuppressedException(secondary);
    assertThat(session.closed).isEqualTo(1);
    session.close();
    assertThat(session.closed).isEqualTo(1);
  }

  @Test
  void conversion_failure_is_delivered_without_losing_session_ownership() {
    StubSession session = new StubSession();
    RuntimeException conversion = new IllegalArgumentException("convert");
    session.conversionFailure = conversion;
    List<String> outcomes = new ArrayList<>();
    batch(configuration -> session).parse(List.of(INPUT), () -> false, new AnalysisProgress(1), (input, result) -> {
      assertThat(session.closed).isZero();
      assertThatThrownBy(result::get).isSameAs(conversion);
      outcomes.add(input.filename());
    });
    assertThat(outcomes).containsExactly(INPUT.filename());
    assertThat(session.closed).isEqualTo(1);
  }

  @Test
  void callback_failure_retries_pending_file_in_fresh_owned_session() {
    StubSession first = new StubSession();
    StubSession fallback = new StubSession();
    AtomicInteger opened = new AtomicInteger();
    AtomicInteger callbacks = new AtomicInteger();
    batch(configuration -> opened.getAndIncrement() == 0 ? first : fallback)
      .parse(List.of(INPUT), () -> false, new AnalysisProgress(1), (input, result) -> {
        if (callbacks.getAndIncrement() == 0) {
          throw new IllegalStateException("callback");
        }
        assertThat(first.closed).isEqualTo(1);
        assertThat(fallback.closed).isZero();
      });
    assertThat(opened).hasValue(2);
    assertThat(callbacks).hasValue(2);
    assertThat(fallback.closed).isEqualTo(1);
  }

  @Test
  void cancellation_and_error_before_first_result_close_without_fallback() {
    for (Throwable failure : List.of(new OperationCanceledException("cancelled"), new AssertionError("before first result"))) {
      StubSession session = new StubSession();
      session.failure = failure;
      AtomicInteger opened = new AtomicInteger();
      assertThatThrownBy(() -> batch(configuration -> { opened.incrementAndGet(); return session; })
        .parse(List.of(INPUT), () -> false, new AnalysisProgress(1), (input, result) -> { throw new AssertionError("unexpected callback"); }))
        .isSameAs(failure);
      assertThat(opened).hasValue(1);
      assertThat(session.closed).isEqualTo(1);
    }
  }

  @Test
  void callback_error_preserves_primary_when_close_fails() {
    StubSession session = new StubSession();
    Error primary = new AssertionError("callback");
    RuntimeException secondary = new IllegalStateException("close");
    session.own(() -> { throw secondary; });
    assertThatThrownBy(() -> batch(configuration -> session).parse(List.of(INPUT), () -> false, new AnalysisProgress(1),
      (input, result) -> { throw primary; })).isSameAs(primary).hasSuppressedException(secondary);
    assertThat(session.closed).isEqualTo(1);
  }

  @Test
  void ecj_registers_environment_before_syntax_conversion_failure() {
    FrontendSession session = spy(new EcjFrontendBackend(() -> EcjFrontendBackend.newParser(CONFIGURATION)).open(CONFIGURATION));
    try (session) {
      assertThatThrownBy(() -> session.parse("A.java", "class A { void m( }"))
        .isInstanceOf(com.sonar.sslr.api.RecognitionException.class);
      verify(session).own(any());
    }
  }

  @Test
  void overlapping_sessions_remain_alive_until_their_own_close() {
    StubSession first = new StubSession();
    StubSession second = new StubSession();
    try (FrontendParser.ParsedFile a = FrontendParser.parse(configuration -> first, CONFIGURATION, "A.java", "");
      FrontendParser.ParsedFile b = FrontendParser.parse(configuration -> second, CONFIGURATION, "B.java", "")) {
      a.close();
      assertThat(first.closed).isEqualTo(1);
      assertThat(second.closed).isZero();
      assertThat(b.tree()).isSameAs(second.tree);
    }
    assertThat(first.closed).isEqualTo(1);
    assertThat(second.closed).isEqualTo(1);
    try (FrontendParser.ParsedFile a = FrontendParser.parse(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), "A.java", "class A {}");
      FrontendParser.ParsedFile b = FrontendParser.parse(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), "B.java", "class B { java.util.List<String> value; }")) {
      a.close();
      var type = (org.sonar.plugins.java.api.tree.ClassTree) b.tree().types().get(0);
      var field = (org.sonar.plugins.java.api.tree.VariableTree) type.members().get(0);
      assertThat(field.symbol().type().fullyQualifiedName()).isEqualTo("java.util.List");
    }
  }

  @Test
  void scanner_cleans_after_visitor_or_modification_failure_and_preserves_primary() {
    for (boolean failModification : List.of(false, true)) {
      AtomicInteger cleaned = new AtomicInteger();
      org.sonar.java.AnalysisException primary = new org.sonar.java.AnalysisException("scan");
      IllegalStateException secondary = new IllegalStateException("cleanup");
      VisitorsBridge visitor = org.mockito.Mockito.mock(VisitorsBridge.class);
      if (!failModification) {
        org.mockito.Mockito.doThrow(primary).when(visitor).visitFile(any(), org.mockito.ArgumentMatchers.anyBoolean());
      }
      var scanner = new org.sonar.java.ast.JavaAstScanner(null, new org.sonar.java.telemetry.NoOpTelemetry(),
        org.sonar.java.telemetry.TelemetryKey.JAVA_ANALYSIS_MAIN);
      scanner.setVisitorBridge(visitor);
      assertThatThrownBy(() -> JParserConfig.Mode.FILE_BY_FILE.create(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of())
        .parse(List.of(INPUT), () -> false, new AnalysisProgress(1), (input, result) -> scanner.simpleScan(input, result,
          ast -> { cleaned.incrementAndGet(); throw secondary; }, ast -> { if (failModification) { throw primary; } })))
        .isSameAs(primary).hasSuppressedException(secondary);
      assertThat(cleaned).hasValue(1);
    }
  }

  @Test
  void batch_metadata_failures_propagate_before_opening_a_session_or_falling_back() {
    for (boolean failPath : List.of(false, true)) {
      InputFile input = org.mockito.Mockito.mock(InputFile.class);
      IllegalStateException failure = new IllegalStateException("input metadata");
      if (failPath) {
        org.mockito.Mockito.when(input.absolutePath()).thenThrow(failure);
      } else {
        org.mockito.Mockito.when(input.absolutePath()).thenReturn("/tmp/A.java");
        org.mockito.Mockito.when(input.charset()).thenThrow(failure);
      }
      AtomicInteger opened = new AtomicInteger();
      assertThatThrownBy(() -> batch(configuration -> { opened.incrementAndGet(); return new StubSession(); })
        .parse(List.of(input), () -> false, new AnalysisProgress(1), (file, result) -> { throw new AssertionError("unexpected callback"); }))
        .isSameAs(failure);
      assertThat(opened).hasValue(0);
    }
  }

  private static JParserConfig batch(FrontendBackend backend) {
    return new JParserConfig.Batch(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false) {
      @Override
      FrontendBackend backend() {
        return backend;
      }
    };
  }

  private static final class StubSession extends FrontendSession {
    private final JavaTree.CompilationUnitTreeImpl tree = org.mockito.Mockito.mock(JavaTree.CompilationUnitTreeImpl.class);
    private Throwable failure;
    private RuntimeException conversionFailure;
    private int closed;

    private StubSession() {
      own(() -> closed++);
    }

    @Override
    JavaTree.CompilationUnitTreeImpl parse(String unitName, String source) {
      failIfRequested();
      return tree;
    }

    @Override
    void parseBatch(FrontendBatch inputs, ProgressMonitor monitor, BiConsumer<InputFile, ParsedUnit> action) {
      failIfRequested();
      inputs.inputs().forEach(input -> action.accept(input, () -> {
        if (conversionFailure != null) {
          throw conversionFailure;
        }
        return tree;
      }));
    }

    private void failIfRequested() {
      if (failure instanceof RuntimeException exception) {
        throw exception;
      }
      if (failure instanceof Error error) {
        throw error;
      }
    }
  }
}
