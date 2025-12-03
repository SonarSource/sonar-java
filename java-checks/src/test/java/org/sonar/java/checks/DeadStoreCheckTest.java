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
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

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
    EraseSymbols eraser = new EraseSymbols();
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/DeadStoreCheckBrokenSemantic.java"))
      .withCheck(new DeadStoreCheck())
      .withCompilationUnitModifier(eraser::visitCompilationUnit)
      .verifyNoIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/DeadStoreCheckSample.java"))
      .withCheck(new DeadStoreCheck())
      .verifyIssues();
  }

  private static class EraseSymbols extends BaseTreeVisitor {

    @Override
    public void visitVariable(VariableTree tree) {
      if (tree instanceof VariableTreeImpl varImpl) {
        varImpl.variableBinding = null;
      }
      super.visitVariable(tree);
    }
  }
}
