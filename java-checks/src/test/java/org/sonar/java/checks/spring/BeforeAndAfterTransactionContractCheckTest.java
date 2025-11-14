/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.spring;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class BeforeAndAfterTransactionContractCheckTest {

  private static final String SAMPLE_FILE = mainCodeSourcesPath("checks/spring/BeforeAndAfterTransactionContractCheckSample.java");
  private static final BeforeAndAfterTransactionContractCheck CHECK = new BeforeAndAfterTransactionContractCheck();

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(SAMPLE_FILE)
      .withCheck(CHECK)
      .verifyIssues();
  }

  @Test
  void test_no_semantics() {
    CheckVerifier.newVerifier()
      .onFile(SAMPLE_FILE)
      .withCheck(CHECK)
      .withoutSemantic()
      .verifyNoIssues();
  }

}
