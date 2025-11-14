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

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPathInModule;
import static org.sonar.java.test.classpath.TestClasspathUtils.JAVA_17_MODULE;

class DisallowedThreadGroupCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DisallowedThreadGroupCheck.java"))
      .withCheck(new DisallowedThreadGroupCheck())
      .verifyIssues();
  }

  @Test
  void no_issue_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DisallowedThreadGroupCheck.java"))
      .withCheck(new DisallowedThreadGroupCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void test_java_api_methods_removed_in_java_21() {
    CheckVerifier.newVerifier()
      .withJavaVersion(17)
      .onFile(mainCodeSourcesPathInModule(JAVA_17_MODULE, "checks/DisallowedThreadGroupCheck.java"))
      .withCheck(new DisallowedThreadGroupCheck())
      .verifyIssues();
  }

}
