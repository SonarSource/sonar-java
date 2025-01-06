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
package org.sonar.java.resolve;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantTest {

  @Test
  void constant() {
    File bytecodeDir = new File("target/test-classes");
    Map<String,Object> valuesByFieldName = new HashMap<>();
    JavaAstScanner.scanSingleFileForTests(
      TestUtils.inputFile("src/test/java/org/sonar/java/resolve/targets/ClassWithConstants.java"),
      new VisitorsBridge(Collections.singleton(new SubscriptionVisitor() {
        @Override
        public List<Tree.Kind> nodesToVisit() {
          return Collections.singletonList(Tree.Kind.VARIABLE);
        }

        @Override
        public void visitNode(Tree tree) {
          VariableTree variableTree = (VariableTree) tree;
          Object value = null;
          Symbol symbol = variableTree.symbol();
          if (symbol.isVariableSymbol()) {
            value = ((Symbol.VariableSymbol) symbol).constantValue().orElse(null);
          }
          valuesByFieldName.put(variableTree.simpleName().name(), value);
        }
      }), Collections.singletonList(bytecodeDir), null));

    assertThat(valuesByFieldName)
      .containsKeys("CONST1", "nonStatic", "nonFinal", "BOOLEAN_TRUE", "BOOLEAN_FALSE")
      .containsEntry("CONST1", "CONST_VALUE")
      .containsEntry("nonStatic", null)
      .containsEntry("nonFinal", null)
      .containsEntry("BOOLEAN_TRUE", Boolean.TRUE)
      .containsEntry("BOOLEAN_FALSE", Boolean.FALSE);

    // See Java 13 Virtual Machine Specification 4.7.2:
    // constant values of short, char and byte types are stored in bytecode as CONSTANT_Integer
    assertThat(valuesByFieldName.get("INT"))
      .isInstanceOf(Integer.class);
    assertThat(valuesByFieldName.get("SHORT"))
      .isInstanceOf(Integer.class);
    assertThat(valuesByFieldName.get("CHAR"))
      .isInstanceOf(Integer.class);
    assertThat(valuesByFieldName.get("BYTE"))
      .isInstanceOf(Integer.class);

    assertThat(valuesByFieldName.get("FLOAT"))
      .isInstanceOf(Float.class);
    assertThat(valuesByFieldName.get("LONG"))
      .isInstanceOf(Long.class);
    assertThat(valuesByFieldName.get("DOUBLE"))
      .isInstanceOf(Double.class);
  }

}
