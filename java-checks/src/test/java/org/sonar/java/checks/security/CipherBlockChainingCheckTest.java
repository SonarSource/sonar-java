/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.security;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class CipherBlockChainingCheckTest {
  private static final String BASE_PATH = "checks/security";
  private static final String DEFAULT_SOURCE_PATH = BASE_PATH + "/CipherBlockChainingCheck.java";
  private static final String CUSTOM_IV_FACTORY_DETECTION_SOURCE_PATH = BASE_PATH + "/CipherBlockChainingCheckShouldDetectCustomIVFactories.java";
  private static final String STATE_RESET_TEST_FILE_SOURCE_PATH = BASE_PATH + "/CipherBlockChainingCheckShouldResetState.java";

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(DEFAULT_SOURCE_PATH))
      .withCheck(new CipherBlockChainingCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(DEFAULT_SOURCE_PATH))
      .withCheck(new CipherBlockChainingCheck())
      .verifyIssues();
  }

  @Test
  void should_detect_custom_iv_factories() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(CUSTOM_IV_FACTORY_DETECTION_SOURCE_PATH))
      .withCheck(new CipherBlockChainingCheck())
      .verifyIssues();
  }

  @Test
  void should_detect_custom_iv_factories_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(CUSTOM_IV_FACTORY_DETECTION_SOURCE_PATH))
      .withCheck(new CipherBlockChainingCheck())
      .verifyIssues();
  }

  @Test
  void should_reset_state_between_class_and_file_analyses() {
    // CipherBlockChainingCheck needs to properly reset its state in between analyzing classes/files.
    // Otherwise, it might not refresh its knowledge about secure factory methods that exist in a given class.
    // This test validates the reset, by analyzing another file before and after the main test source file.

    // First, lets verify that the additional testing file passes the verifier on its own
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(STATE_RESET_TEST_FILE_SOURCE_PATH))
      .withCheck(new CipherBlockChainingCheck())
      .verifyIssues();

    // Then lets interleave its analysis with the main file from above
    CheckVerifier.newVerifier()
      .onFiles(mainCodeSourcesPath(STATE_RESET_TEST_FILE_SOURCE_PATH), mainCodeSourcesPath(CUSTOM_IV_FACTORY_DETECTION_SOURCE_PATH))
      .withCheck(new CipherBlockChainingCheck())
      .verifyIssues();

    CheckVerifier.newVerifier()
      .onFiles(mainCodeSourcesPath(CUSTOM_IV_FACTORY_DETECTION_SOURCE_PATH), mainCodeSourcesPath(STATE_RESET_TEST_FILE_SOURCE_PATH))
      .withCheck(new CipherBlockChainingCheck())
      .verifyIssues();
  }
}
