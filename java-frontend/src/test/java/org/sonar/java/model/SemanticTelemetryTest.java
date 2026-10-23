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

import com.sonar.sslr.api.RecognitionException;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.telemetry.DefaultTelemetry;
import org.sonar.java.telemetry.TelemetryKey;
import org.sonar.java.telemetry.TelemetryKey.JavaAnalysisKeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticTelemetryTest {

  private final DefaultTelemetry telemetry = new DefaultTelemetry();

  static Stream<JavaAnalysisKeys> scopes() {
    return Stream.of(TelemetryKey.JAVA_ANALYSIS_MAIN, TelemetryKey.JAVA_ANALYSIS_TEST, TelemetryKey.JAVA_ANALYSIS_GENERATED);
  }

  @ParameterizedTest
  @MethodSource("scopes")
  void counts_affected_files_and_modules_once(JavaAnalysisKeys keys) throws Exception {
    JavaAstScanner scanner = scanner(keys);
    scanSource(scanner, "class A { String value; }");
    scanSource(scanner, "class A { Missing a; Missing b; Other c; }");
    scanSource(scanner, "class A { Missing a; Missing b; Other c; }");

    var semantic = keys.semantic();
    assertThat(telemetry.toMap())
      .containsEntry(semantic.filesEvaluated().key(), "3")
      .containsEntry(semantic.filesWithResolutionErrors().key(), "2")
      .containsEntry(semantic.resolutionErrorCount().key(), "4")
      .containsEntry(semantic.modulesEvaluated().key(), "1")
      .containsEntry(semantic.modulesWithResolutionErrors().key(), "1")
      .containsEntry(keys.success().fileCountKey().key(), "3");

    scanSource(scanner(keys), "class A { String value; }");
    assertThat(telemetry.toMap())
      .containsEntry(semantic.modulesEvaluated().key(), "2")
      .containsEntry(semantic.modulesWithResolutionErrors().key(), "1");
  }

  @Test
  void preview_diagnostics_are_excluded_from_resolution_errors() throws Exception {
    var tree = mock(JavaTree.CompilationUnitTreeImpl.class);
    tree.sema = mock(JSema.class);
    when(tree.sema.undefinedTypes()).thenReturn(Set.of(new JProblem("Preview feature", JProblem.Type.PREVIEW_FEATURE_USED)));
    var result = mock(JParserConfig.Result.class);
    when(result.get()).thenReturn(tree);

    scanner(TelemetryKey.JAVA_ANALYSIS_MAIN).simpleScan(TestUtils.emptyInputFile("A.java"), result, ignored -> {});

    assertThat(telemetry.toMap())
      .containsEntry("java.analysis.main.semantic.files_evaluated", "1")
      .containsEntry("java.analysis.main.semantic.files_with_resolution_errors", "0")
      .containsEntry("java.analysis.main.semantic.resolution_error_count", "0")
      .containsEntry("java.analysis.main.semantic.modules_with_resolution_errors", "0")
      .containsEntry("java.analysis.main.success.type_error_count", "1");
  }

  @Test
  void failed_parsing_is_unobserved() throws Exception {
    var result = mock(JParserConfig.Result.class);
    when(result.get()).thenThrow(new RecognitionException(-1, "Invalid syntax"));
    scanner(TelemetryKey.JAVA_ANALYSIS_MAIN).simpleScan(TestUtils.emptyInputFile("A.java"), result, ignored -> {});

    assertThat(telemetry.toMap())
      .containsEntry("java.analysis.main.parse_errors.file_count", "1")
      .doesNotContainKey("java.analysis.main.semantic.files_evaluated");
  }

  @Test
  void parser_failure_is_unobserved() throws Exception {
    var result = mock(JParserConfig.Result.class);
    when(result.get()).thenThrow(new IllegalStateException("Parser failure"));
    scanner(TelemetryKey.JAVA_ANALYSIS_MAIN).simpleScan(TestUtils.emptyInputFile("A.java"), result, ignored -> {});

    assertThat(telemetry.toMap())
      .containsEntry("java.analysis.main.exceptions.file_count", "1")
      .doesNotContainKey("java.analysis.main.semantic.files_evaluated");
  }

  @Test
  void semantic_observation_survives_visitor_failure() throws Exception {
    var visitors = mock(VisitorsBridge.class);
    doThrow(new IllegalStateException("Visitor failure")).when(visitors).visitFile(any(), anyBoolean());
    var scanner = new JavaAstScanner(null, telemetry, TelemetryKey.JAVA_ANALYSIS_MAIN);
    scanner.setVisitorBridge(visitors);
    scanSource(scanner, "class A { Missing value; }");

    assertThat(telemetry.toMap())
      .containsEntry("java.analysis.main.semantic.files_evaluated", "1")
      .containsEntry("java.analysis.main.semantic.files_with_resolution_errors", "1")
      .containsEntry("java.analysis.main.exceptions.file_count", "1");
  }

  private JavaAstScanner scanner(JavaAnalysisKeys keys) {
    var scanner = new JavaAstScanner(null, telemetry, keys);
    scanner.setVisitorBridge(mock(VisitorsBridge.class));
    return scanner;
  }

  private static void scanSource(JavaAstScanner scanner, String source) throws Exception {
    var tree = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
    var result = mock(JParserConfig.Result.class);
    when(result.get()).thenReturn(tree);
    scanner.simpleScan(TestUtils.emptyInputFile("A.java"), result, ast -> ast.sema.getEnvironmentCleaner().run());
  }
}
