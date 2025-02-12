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
package org.sonar.java.checks;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.test.classpath.TestClasspathUtils;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPathInModule;

class WeakSSLContextCheckTest {

  private static final List<File> SPRING_3_2_CLASSPATH = TestClasspathUtils.loadFromFile(Constants.SPRING_3_2_CLASSPATH);

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/WeakSSLContextCheckJava8.java"))
      .withCheck(new WeakSSLContextCheck())
      .verifyIssues();
  }

  @Test
  void test_java_7() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/WeakSSLContextCheck.java"))
      .withCheck(new WeakSSLContextCheck())
      .withJavaVersion(7)
      .verifyIssues();
  }

  @Test
  void test_java_8() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/WeakSSLContextCheckJava8.java"))
      .withCheck(new WeakSSLContextCheck())
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void test_with_spring_3_2() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPathInModule(Constants.SPRING_3_2, "checks/WeakSSLContextCheckSample.java"))
      .withCheck(new WeakSSLContextCheck())
      .withClassPath(SPRING_3_2_CLASSPATH)
      .verifyIssues();
  }

}
