/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.resolve;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

public class ConstantTest {

  @Test
  public void constant() {
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
            value = ((JavaSymbol.VariableJavaSymbol) symbol).constantValue().orElse(null);
          }
          valuesByFieldName.put(variableTree.simpleName().name(), value);
        }
      }), Collections.singletonList(bytecodeDir), null));
    assertThat(valuesByFieldName.keySet()).contains("CONST1", "nonStatic", "nonFinal", "BOOLEAN_TRUE", "BOOLEAN_FALSE");
    assertThat(valuesByFieldName.get("CONST1")).isEqualTo("CONST_VALUE");
    assertThat(valuesByFieldName.get("nonStatic")).isNull();
    assertThat(valuesByFieldName.get("nonFinal")).isNull();
    assertThat(valuesByFieldName.get("BOOLEAN_TRUE")).isEqualTo(true);
    assertThat(valuesByFieldName.get("BOOLEAN_FALSE")).isEqualTo(false);
  }

}
