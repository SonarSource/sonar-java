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
import org.junit.jupiter.api.Test;
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
  void extractSeeReferences_returns_empty_for_null_or_empty() {
    assertThat(JavadocReferencesExistingSymbolsCheck.extractSeeReferences(null)).isEmpty();
    assertThat(JavadocReferencesExistingSymbolsCheck.extractSeeReferences("")).isEmpty();
  }

  @Test
  void extractSeeReferences_extracts_see_tags() {
    List<String> refs = JavadocReferencesExistingSymbolsCheck.extractSeeReferences(
      "/** @see java.util.List */");
    assertThat(refs).containsExactly("java.util.List");
  }

  @Test
  void extractSeeReferences_skips_urls() {
    List<String> refs = JavadocReferencesExistingSymbolsCheck.extractSeeReferences(
      "/** @see http://example.com @see https://example.com */");
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
  void resolveReference_returns_null_for_null_or_empty() {
    JavadocReferencesExistingSymbolsCheck check = new JavadocReferencesExistingSymbolsCheck();
    assertThat(check.resolveReference(null)).isNull();
    assertThat(check.resolveReference("")).isNull();
  }

  @Test
  void resolveReference_returns_null_for_method_references() {
    JavadocReferencesExistingSymbolsCheck check = new JavadocReferencesExistingSymbolsCheck();
    assertThat(check.resolveReference("#myMethod")).isNull();
  }

  @Test
  void resolveReference_returns_fully_qualified_name_as_is() {
    JavadocReferencesExistingSymbolsCheck check = new JavadocReferencesExistingSymbolsCheck();
    assertThat(check.resolveReference("java.util.List")).isEqualTo("java.util.List");
  }

  @Test
  void resolveReference_strips_method_signature() {
    JavadocReferencesExistingSymbolsCheck check = new JavadocReferencesExistingSymbolsCheck();
    assertThat(check.resolveReference("java.util.List#size()")).isEqualTo("java.util.List");
  }

  @Test
  void resolveReference_strips_member_reference() {
    JavadocReferencesExistingSymbolsCheck check = new JavadocReferencesExistingSymbolsCheck();
    assertThat(check.resolveReference("java.util.List#EMPTY_LIST")).isEqualTo("java.util.List");
  }

  @Test
  void resolveReference_returns_simple_name_without_package() {
    JavadocReferencesExistingSymbolsCheck check = new JavadocReferencesExistingSymbolsCheck();
    assertThat(check.resolveReference("MyClass")).isEqualTo("MyClass");
  }
}
