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
package org.sonar.java.psi;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PsiLanguageValidatorTest {
  static Stream<Arguments> boundaries() {
    return Stream.of(
      Arguments.of("class Example { String s = \"\"\"\ntext\n\"\"\"; }", 14, 15, "Text Blocks"),
      Arguments.of("class Example { boolean m(Object o) { return o instanceof String s; } }", 15, 16, "Type Patterns"),
      Arguments.of("class Example { record R(int i) {} boolean m(Object o) { return o instanceof R(int i); } }", 20, 21, "Record Pattern"),
      Arguments.of("class Example { int m(Object o) { return switch(o) { case String s -> 1; default -> 0; }; } }", 20, 21, "Pattern Matching in Switch"),
      Arguments.of("import module java.sql; class Example {}", 24, 25, "Module Import Declarations"),
      Arguments.of("class Example { Example() { int i = 1; super(); } }", 24, 25, "Flexible Constructor Bodies"),
      Arguments.of("void main() {}", 24, 25, "Compact Source Files and Instance Main Methods"));
  }

  @ParameterizedTest
  @MethodSource("boundaries")
  void exposes_feature_diagnostics_without_making_them_fatal(String source, int before, int after, String feature) {
    var result = validate(source, before, false);
    assertFalse(result.hasFatalError());
    assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.kind() == PsiLanguageValidator.Kind.PREVIEW_FEATURE_USED
      && diagnostic.message().equals("The Java feature '" + feature + "' is only available with source level " + after + " and above")));
    assertTrue(validate(source, after, false).diagnostics().isEmpty());
  }

  @Test
  void keyword_failures_preserve_the_original_message_and_utf16_range() {
    var source = "sealed class Example permits Child {} final class Child extends Example {}";
    var result = validate(source, 16, false);
    var diagnostic = result.diagnostics().getFirst();
    assertEquals(PsiLanguageValidator.Kind.FATAL, diagnostic.kind());
    assertEquals("Syntax error on token \"class\", . expected", diagnostic.message());
    assertEquals(7, diagnostic.start());
    assertEquals(11, diagnostic.end());
    assertTrue(validate("class Example { void m() { int _ = 1; } }", 21, false).hasFatalError());
    assertFalse(validate("class Example { void m() { int _ = 1; } }", 22, false).hasFatalError());
  }

  @Test
  void distinguishes_local_var_from_lambda_var() {
    var local = "class Example { void m() { var x = 1; } }";
    assertEquals("var cannot be resolved to a type", validate(local, 9, false).diagnostics().getFirst().message());
    assertTrue(validate(local, 10, false).diagnostics().isEmpty());
    var lambda = "class Example { java.util.function.IntUnaryOperator f = (var x) -> x; }";
    assertEquals("'var' is not allowed here", validate(lambda, 10, false).diagnostics().getFirst().message());
    assertFalse(validate(lambda, 11, false).hasFatalError());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {21, 25, 26})
  void primitive_instanceof_patterns_expose_a_diagnostic_when_preview_is_disabled(int version) {
    var source = "class Example { boolean m(Object o) { return o instanceof int i; } }";
    var result = validate(source, version, false);
    assertFalse(result.hasFatalError());
    assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.kind() == PsiLanguageValidator.Kind.PREVIEW_FEATURE_USED
      && diagnostic.message().equals("Incompatible conditional operand types Object and int")));
    assertTrue(validate(source, 26, true).diagnostics().isEmpty());
  }

  @Test
  void exposes_recovered_record_and_switch_selector_diagnostics() {
    var record = validate("record Example(int i) {}", 15, false);
    assertTrue(record.diagnostics().stream().anyMatch(diagnostic -> diagnostic.message().equals("Implicitly declared class must have a candidate main method")));
    var nested = validate("class Example { record Pair(int i) {} boolean m(Object o) { return o instanceof Pair(int i); } }", 15, false);
    assertTrue(nested.diagnostics().stream().anyMatch(diagnostic -> diagnostic.kind() == PsiLanguageValidator.Kind.UNDEFINED_TYPE
      && diagnostic.message().equals("Pair cannot be resolved to a type")));
    var selector = validate("class Example { int m(Object o) { return switch(o) { case String s -> 1; default -> 0; }; } }", 20, false);
    assertTrue(selector.diagnostics().stream().anyMatch(diagnostic -> diagnostic.message().equals("Cannot switch on a value of type Object. Only convertible int values, strings or enum variables are permitted")));
  }

  static Stream<Arguments> malformedHeaders() {
    return Stream.of(
      Arguments.of("class C { void m( }", 8),
      Arguments.of("class C { C( }", 8),
      Arguments.of("class C { String m( }", 8),
      Arguments.of("class C { <T> T m( }", 8),
      Arguments.of("class C { void m(int x }", 8),
      Arguments.of("class C { void good() {} void bad( }", 23),
      Arguments.of("class Élève {\r\n void méthode( }", 12),
      Arguments.of("class C { void m(", 8),
      Arguments.of("class C { void m(/*text*/ }", 8));
  }

  @ParameterizedTest
  @MethodSource("malformedHeaders")
  void preserves_ecj_top_level_method_header_recovery(String source, int anchor) {
    var result = validate(source, 26, false);
    assertTrue(result.hasFatalError());
    var diagnostic = result.rawSyntaxErrors().getFirst();
    assertEquals("Syntax error, insert \"}\" to complete ClassBody", diagnostic.message());
    assertEquals(anchor, diagnostic.start());
    assertEquals(anchor, diagnostic.end());
  }

  @Test
  void ignores_feature_names_in_comments_and_strings_and_keeps_malformed_syntax_fatal() {
    var source = "class Example { String s = \"sealed record var import module\"; }";
    assertTrue(validate(source, 6, false).diagnostics().isEmpty());
    assertTrue(validate("class Example { void m( }", 26, false).hasFatalError());
  }

  private static PsiLanguageValidator.Result validate(String source, int version, boolean preview) {
    var configuration = new JavaPsiConfiguration(null, List.of(), version, preview, false);
    var snapshot = new JavaPsiEnvironment.SourceSnapshot(Path.of("Example.java"), source, StandardCharsets.UTF_8);
    try (var environment = JavaPsiEnvironment.open(configuration, List.of(snapshot))) {
      return environment.read((project, files) -> PsiLanguageValidator.validate(files.getFirst(), configuration));
    }
  }
}
