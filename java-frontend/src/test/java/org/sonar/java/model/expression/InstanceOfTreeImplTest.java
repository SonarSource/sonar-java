/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.model.expression;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class InstanceOfTreeImplTest {

  private static final String CLASS_WITH_INSTANCE_OF = "class A {\n"
    + "  void foo(Object o) {\n"
    + "    if(%s) { }\n"
    + "  }\n"
    + "}\n";

  @Test
  void test_PatternInstanceOfTree() {
    InstanceOfTreeImpl ioti = instanceOf("o instanceof String s");
    assertThat(ioti.is(Tree.Kind.PATTERN_INSTANCE_OF)).isTrue();

    PatternInstanceOfTree piot = ioti;
    assertThat(piot.expression()).isNotNull();
    assertThat(piot.instanceofKeyword()).isNotNull();
    assertThat(piot.variable()).isNotNull();

    InstanceOfVisitor visitor = new InstanceOfVisitor();

    piot.accept(visitor);
    assertThat(visitor.visited).containsExactly(true, false);
  }

  @Test
  void test_InstanceOfTree() {
    InstanceOfTreeImpl ioti = instanceOf("o instanceof String");
    assertThat(ioti.is(Tree.Kind.INSTANCE_OF)).isTrue();

    InstanceOfTree iot = ioti;
    assertThat(iot.expression()).isNotNull();
    assertThat(iot.instanceofKeyword()).isNotNull();
    assertThat(iot.type()).isNotNull();

    InstanceOfVisitor visitor = new InstanceOfVisitor();

    iot.accept(visitor);
    assertThat(visitor.visited).containsExactly(false, true);
  }

  private static class InstanceOfVisitor extends BaseTreeVisitor {
    private final boolean[] visited = {false, false};

    @Override
    public void visitPatternInstanceOf(PatternInstanceOfTree tree) {
      super.visitPatternInstanceOf(tree);
      visited[0] = true;
    }

    @Override
    public void visitInstanceOf(org.sonar.plugins.java.api.tree.InstanceOfTree tree) {
      super.visitInstanceOf(tree);
      visited[1] = true;
    }
  }

  private static InstanceOfTreeImpl instanceOf(String instanceofExpression) {
    CompilationUnitTree cut = JParserTestUtils.parse(String.format(CLASS_WITH_INSTANCE_OF, instanceofExpression));
    ClassTree classTree = (ClassTree) cut.types().get(0);
    MethodTree methodTree = (MethodTree) classTree.members().get(0);
    IfStatementTree ifStatementTree = (IfStatementTree) methodTree.block().body().get(0);
    return (InstanceOfTreeImpl) ifStatementTree.condition();
  }

}
