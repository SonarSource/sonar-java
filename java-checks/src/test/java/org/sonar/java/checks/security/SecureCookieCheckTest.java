/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.Constants;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.test.classpath.TestClasspathUtils;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPathInModule;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class SecureCookieCheckTest {

  private static final String SOURCE_PATH = "checks/security/SecureCookieCheckSample.java";
  private static final String TEST_SOURCE_PATH = mainCodeSourcesPath(SOURCE_PATH);
  private static final String NON_COMPILING_TEST_SOURCE_PATH = nonCompilingTestSourcesPath(SOURCE_PATH);
  public static final List<File> SPRING_3_2_CLASSPATH = TestClasspathUtils.loadFromFile(Constants.SPRING_3_2_CLASSPATH);

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TEST_SOURCE_PATH)
      .withCheck(new SecureCookieCheck())
      .verifyIssues();
  }

  @Test
  void test_jakarta() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/security/SecureCookieCheckJakarta.java"))
      .withCheck(new SecureCookieCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(NON_COMPILING_TEST_SOURCE_PATH)
      .withCheck(new SecureCookieCheck())
      .verifyIssues();
  }

  @Test
  void test_with_spring_3_2() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPathInModule(Constants.SPRING_3_2, "checks/SecureCookieCheckSample.java"))
      .withCheck(new SecureCookieCheck())
      .withClassPath(SPRING_3_2_CLASSPATH)
      .verifyIssues();
  }

}
