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

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class PseudoRandomCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/PseudoRandomCheckSample.java"))
      .withCheck(new PseudoRandomCheck())
      .verifyIssues();
  }

  @Test
  void test_no_security_context() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/PseudoRandomCheckNoContextSample.java"))
      .withCheck(new PseudoRandomCheck())
      .verifyNoIssues();
  }

  @Test
  void test_security_keywords() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/PseudoRandomCheckSecurityKeywordsSample.java"))
      .withCheck(new PseudoRandomCheck())
      .verifyIssues();
  }

  @Test
  void test_crypto_import() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/PseudoRandomCheckCryptoImportSample.java"))
      .withCheck(new PseudoRandomCheck())
      .verifyIssues();
  }

  @Test
  void test_field_scope() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/PseudoRandomCheckFieldScopeSample.java"))
      .withCheck(new PseudoRandomCheck())
      .verifyIssues();
  }

  @Test
  void test_wildcard_crypto_import() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/PseudoRandomCheckWildcardImportSample.java"))
      .withCheck(new PseudoRandomCheck())
      .verifyIssues();
  }

  @Test
  void test_static_crypto_import() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/PseudoRandomCheckStaticImportSample.java"))
      .withCheck(new PseudoRandomCheck())
      .verifyIssues();
  }
}
