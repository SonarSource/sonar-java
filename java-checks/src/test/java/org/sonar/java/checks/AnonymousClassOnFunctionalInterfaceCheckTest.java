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

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class AnonymousClassOnFunctionalInterfaceCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AnonymousClassOnFunctionalInterfaceCheckSample.java"))
      .withCheck(new AnonymousClassOnFunctionalInterfaceCheck())
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AnonymousClassOnFunctionalInterfaceCheckSampleWithoutSemantic.java"))
      .withCheck(new AnonymousClassOnFunctionalInterfaceCheck())
      .withJavaVersion(8)
      .withoutSemantic()
      .verifyIssues();
  }

  @Test
  void test_gwt_supporting_lambdas() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AnonymousClassOnFunctionalInterfaceCheckGwtSample.java"))
      .withCheck(new AnonymousClassOnFunctionalInterfaceCheck())
      .withJavaVersion(8)
      .withClassPath(List.of(new File("gwt-user-2.8.0.jar")))
      .verifyIssues();
  }

  @Test
  void test_gwt_not_supporting_lambdas() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AnonymousClassOnFunctionalInterfaceCheckGwtSample.java"))
      .withCheck(new AnonymousClassOnFunctionalInterfaceCheck())
      .withJavaVersion(8)
      .withClassPath(List.of(new File("gwt-user-2.7.0.jar")))
      .verifyNoIssues();
  }

  @Test
  void test_gwt_dev_not_supporting_lambdas() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AnonymousClassOnFunctionalInterfaceCheckGwtSample.java"))
      .withCheck(new AnonymousClassOnFunctionalInterfaceCheck())
      .withJavaVersion(8)
      .withClassPath(List.of(new File("gwt-user-2.8.0.jar"), new File("gwt-dev-2.6.1.jar")))
      .verifyNoIssues();
  }
}
