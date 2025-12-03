/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPathInModule;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.test.classpath.TestClasspathUtils.SPRING_32_MODULE;

class SQLInjectionCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/SQLInjection.java"))
      .withCheck(new SQLInjectionCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/SQLInjectionSample.java"))
      .withCheck(new SQLInjectionCheck())
      .verifyIssues();
  }

  @Test
  void test_with_spring_3_2() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPathInModule(SPRING_32_MODULE, "checks/SQLInjectionCheckSample.java"))
      .withCheck(new SQLInjectionCheck())
      .withClassPath(SPRING_32_MODULE.getClassPath())
      .verifyIssues();
  }

}
