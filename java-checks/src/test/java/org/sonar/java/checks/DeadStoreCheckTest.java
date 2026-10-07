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
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.model.JavaTree;

class DeadStoreCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/DeadStoreCheckSample.java"))
      .withCheck(new DeadStoreCheck())
      .verifyIssues();
  }

  @Test
  void test_variable_identifier_with_unknown_symbol() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/DeadStoreCheckBrokenSemantic.java"))
      .withCheck(new DeadStoreCheck())
      .withCompilationUnitModifier(tree -> ((JavaTree.CompilationUnitTreeImpl) tree).javacSema = null)
      .verifyNoIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/DeadStoreCheckSample.java"))
      .withCheck(new DeadStoreCheck())
      .verifyIssues();
  }

  @Test
  void test_incomplete_semantic() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/UnusedVariablesFPCheck.java"))
      .withCheck(new DeadStoreCheck())
      .verifyNoIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/DeadStoreCheckSample.java"))
      .withCheck(new DeadStoreCheck())
      .withoutSemantic()
      .verifyIssues();
  }
}
