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
package org.sonar.java.model;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class JVariableSymbolTest {

  @Test
  void isLocalVariable() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() { String a; } String field; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl localVariable = (VariableTreeImpl) method.block().body().get(0);
    JVariableSymbol variableSymbol = cu.sema.variableSymbol(localVariable.variableBinding);
    assertThat(variableSymbol.isLocalVariable()).isTrue();
    VariableTreeImpl classVariable = (VariableTreeImpl) c.members().get(1);
    variableSymbol = cu.sema.variableSymbol(classVariable.variableBinding);
    assertThat(variableSymbol.isLocalVariable()).isFalse();
  }

  @Test
  void isLocalVariable_inLambda() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { java.util.function.IntSupplier s = () -> { int a = 34; return a + 56; }; }");
    var c = (ClassTree) cu.types().get(0);
    var classVariable = (VariableTree) c.members().get(0);
    assertThat(classVariable.symbol().isLocalVariable()).isFalse();

    var lambda = (LambdaExpressionTree) classVariable.initializer();
    var variableA = (VariableTree) ((BlockTree) lambda.body()).body().get(0);
    assertThat(variableA.symbol().isLocalVariable()).isTrue();
  }

  @Test
  void isParameter() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m(int p) { String a; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl localVariable = (VariableTreeImpl) method.block().body().get(0);

    JVariableSymbol variableSymbol = cu.sema.variableSymbol(localVariable.variableBinding);
    assertThat(variableSymbol.isParameter()).isFalse();

    VariableTreeImpl parameter = (VariableTreeImpl) method.parameters().get(0);
    variableSymbol = cu.sema.variableSymbol(parameter.variableBinding);
    assertThat(variableSymbol.isParameter()).isTrue();
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }
}
