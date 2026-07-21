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
package org.sonar.java.checks.quarkus;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPathInModule;
import static org.sonar.java.test.classpath.TestClasspathUtils.QUARKUS_ARC_315_MODULE;

class SingletonInsteadOfApplicationScopedCheckTest {

  private static final String SAMPLE = mainCodeSourcesPathInModule(QUARKUS_ARC_315_MODULE, "checks/quarkus/SingletonInsteadOfApplicationScopedCheckSample.java");

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(SAMPLE)
      .withCheck(new SingletonInsteadOfApplicationScopedCheck())
      .withClassPath(QUARKUS_ARC_315_MODULE.getClassPath())
      .verifyIssues();
  }

  @Test
  void testWithoutSemantic() {
    CheckVerifier.newVerifier()
      .onFile(SAMPLE)
      .withCheck(new SingletonInsteadOfApplicationScopedCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void testWithoutQuarkusOnClasspath() {
    CheckVerifier.newVerifier()
      .onFile(SAMPLE)
      .withCheck(new SingletonInsteadOfApplicationScopedCheck())
      .withClassPath(java.util.List.of())
      .verifyNoIssues();
  }
}
