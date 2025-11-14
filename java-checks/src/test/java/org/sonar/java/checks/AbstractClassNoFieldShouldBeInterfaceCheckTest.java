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
package org.sonar.java.checks;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class AbstractClassNoFieldShouldBeInterfaceCheckTest {

  private static final String TEST_FILE = "checks/AbstractClassNoFieldShouldBeInterfaceCheckSample.java";
  private static final String TEST_FILE_NO_JAVA_VERSION = "checks/AbstractClassNoFieldShouldBeInterfaceCheckSample_no_version.java";
  private static final String TEST_FILE_JAVA_9 = "checks/AbstractClassNoFieldShouldBeInterfaceCheckSample_java9.java";

  @Test
  void test_no_version() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(TEST_FILE_NO_JAVA_VERSION))
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .verifyIssues();
  }

  @Test
  void test_with_java_7() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FILE))
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(7)
      .verifyNoIssues();
  }

  @Test
  void test_with_java_8() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FILE))
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void test_with_java_9() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(TEST_FILE_JAVA_9))
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(9)
      .verifyIssues();
  }

  @Test
  void test_no_version_without_semantics() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(TEST_FILE_NO_JAVA_VERSION))
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withClassPath(Collections.emptyList())
      .verifyIssues();
  }

  @Test
  void test_with_java_7_without_semantics() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(TEST_FILE))
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(7)
      .withClassPath(Collections.emptyList())
      .verifyNoIssues();
  }

  @Test
  void test_with_java_8_without_semantics() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(TEST_FILE))
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(8)
      .withClassPath(Collections.emptyList())
      .verifyIssues();
  }


  @Test
  void test_with_java_9_without_semantics() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath(TEST_FILE_JAVA_9))
      .withCheck(new AbstractClassNoFieldShouldBeInterfaceCheck())
      .withJavaVersion(9)
      .withClassPath(Collections.emptyList())
      .verifyIssues();
  }
}
