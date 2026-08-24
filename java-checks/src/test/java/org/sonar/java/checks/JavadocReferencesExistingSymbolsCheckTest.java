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
package org.sonar.java.checks;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class JavadocReferencesExistingSymbolsCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/JavadocReferencesExistingSymbolsCheckSample.java"))
      .withCheck(new JavadocReferencesExistingSymbolsCheck())
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/JavadocReferencesExistingSymbolsCheckSample.java"))
      .withCheck(new JavadocReferencesExistingSymbolsCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void extractSeeReferences_extracts_see_tags() {
    List<String> refs = JavadocReferencesExistingSymbolsCheck.extractSeeReferences(
      "/** @see java.util.List */");
    assertThat(refs).containsExactly("java.util.List");
  }

  static Stream<Arguments> nonReferenceInputs() {
    return Stream.of(
      Arguments.of("URLs", "/** @see http://example.com @see https://example.com */"),
      Arguments.of("HTML anchors", "/** @see <a href=\"http://example.com\">Example</a> */"),
      Arguments.of("quoted strings", "/** @see \"The Java Programming Language\" */")
    );
  }

  @ParameterizedTest(name = "extractSeeReferences skips {0}")
  @MethodSource("nonReferenceInputs")
  void extractSeeReferences_skips_non_references(String description, String javadoc) {
    List<String> refs = JavadocReferencesExistingSymbolsCheck.extractSeeReferences(javadoc);
    assertThat(refs).isEmpty();
  }

  @Test
  void extractSeeReferences_extracts_link_tags() {
    List<String> refs = JavadocReferencesExistingSymbolsCheck.extractSeeReferences(
      "/** {@link java.util.List} */");
    assertThat(refs).containsExactly("java.util.List");
  }

  @Test
  void extractSeeReferences_extracts_linkplain_tags() {
    List<String> refs = JavadocReferencesExistingSymbolsCheck.extractSeeReferences(
      "/** {@linkplain java.util.Map} */");
    assertThat(refs).containsExactly("java.util.Map");
  }

  @Test
  void extractSeeReferences_handles_mixed_tags() {
    List<String> refs = JavadocReferencesExistingSymbolsCheck.extractSeeReferences(
      "/** @see java.util.List {@link java.util.Map} */");
    assertThat(refs).containsExactly("java.util.List", "java.util.Map");
  }

  @Test
  void stripMemberReference_returns_null_for_method_references() {
    assertThat(JavadocReferencesExistingSymbolsCheck.stripMemberReference("#myMethod")).isNull();
  }

  @Test
  void stripMemberReference_returns_fully_qualified_name_as_is() {
    assertThat(JavadocReferencesExistingSymbolsCheck.stripMemberReference("java.util.List")).isEqualTo("java.util.List");
  }

  @Test
  void stripMemberReference_strips_method_signature() {
    assertThat(JavadocReferencesExistingSymbolsCheck.stripMemberReference("java.util.List#size()")).isEqualTo("java.util.List");
  }

  @Test
  void stripMemberReference_strips_member_reference() {
    assertThat(JavadocReferencesExistingSymbolsCheck.stripMemberReference("java.util.List#EMPTY_LIST")).isEqualTo("java.util.List");
  }

  @Test
  void stripMemberReference_returns_simple_name() {
    assertThat(JavadocReferencesExistingSymbolsCheck.stripMemberReference("MyClass")).isEqualTo("MyClass");
  }
}
