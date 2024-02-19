/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.util.Set;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import static org.assertj.core.api.Assertions.assertThat;

class TreeHelperTest extends JParserTestUtils {

  @Test
  void testFindClosestParentOfKind() {
    String code = newCode("int bar() {",
      "  boolean a = foo();",
      "  while (true) {",
      "    if (condition) {",
      "      bar();",
      "    }",
      "    for (var a: b) {",
      "      baz();",
      "    }",
      "  }",
      "}");

    MethodTree method = methodTree(code);
    var whileBody = ((BlockTree) ((WhileStatementTree) method.block().body().get(1)).statement()).body();
    var barCall = ((BlockTreeImpl) ((IfStatementTree) whileBody.get(0)).thenStatement()).body().get(0);
    var bazCall = ((BlockTreeImpl) (((ForEachStatement) whileBody.get(1)).statement())).body().get(0);

    assertFoundNode(barCall, Set.of(Tree.Kind.IF_STATEMENT), Tree.Kind.IF_STATEMENT);
    assertFoundNode(barCall, Set.of(Tree.Kind.WHILE_STATEMENT), Tree.Kind.WHILE_STATEMENT);
    assertFoundNode(barCall, Set.of(Tree.Kind.IF_STATEMENT, Tree.Kind.WHILE_STATEMENT), Tree.Kind.IF_STATEMENT);
    assertFoundNode(barCall, Set.of(Tree.Kind.METHOD), Tree.Kind.METHOD);
    assertFoundNode(barCall, Set.of(Tree.Kind.CLASS), Tree.Kind.CLASS);
    assertFoundNode(barCall, Set.of(Tree.Kind.DO_STATEMENT), null);
    assertFoundNode(barCall, Set.of(Tree.Kind.DO_STATEMENT, Tree.Kind.WHILE_STATEMENT), Tree.Kind.WHILE_STATEMENT);
    assertFoundNode(barCall, Set.of(Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.WHILE_STATEMENT),
      Tree.Kind.WHILE_STATEMENT);
    assertFoundNode(bazCall, Set.of(Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.WHILE_STATEMENT),
      Tree.Kind.FOR_EACH_STATEMENT);
  }

  private void assertFoundNode(Tree tree, Set<Tree.Kind> kinds, @Nullable Tree.Kind expected) {
    var actual = TreeHelper.findClosestParentOfKind(tree, kinds);
    if (expected != null) {
      assertThat(actual).isNotNull();
      assertThat(actual.kind()).isEqualTo(expected);
    } else {
      assertThat(actual).isNull();
    }
  }
}
