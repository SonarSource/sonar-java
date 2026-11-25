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
package org.sonar.java.semsitter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class EffectivelyFinalVariablesTest {

  private static final String SOURCE = """
    class A {
      void m(boolean c, int k) {
        int ifElse; if (c) ifElse = 1; else ifElse = 2;
        int sequence; sequence = 1; sequence = 2;
        int loop; while (c) { loop = 1; }
        int arrows; switch (k) { case 1 -> arrows = 1; default -> arrows = 2; }
        int colons; switch (k) { case 1: colons = 1; default: colons = 2; }
        int sameArm; if (c) { sameArm = 1; sameArm = 2; }
        int noElse; if (c) noElse = 1; noElse = 2;
        for (int init; c;) { init = 1; }
        boolean nested; if (nested = (nested = c)) { } else { }
      }
    }
    """;

  @TempDir
  Path baseDir;

  private final Map<String, VariableTree> variables = new HashMap<>();
  private final Map<String, List<Tree>> assignments = new HashMap<>();

  @BeforeEach
  void parse() throws Exception {
    CompilationUnitTree unit = (CompilationUnitTree) Replays.parseWithEcj(baseDir, "A", SOURCE).get();
    unit.accept(new BaseTreeVisitor() {
      @Override
      public void visitVariable(VariableTree tree) {
        variables.put(tree.simpleName().name(), tree);
        super.visitVariable(tree);
      }

      @Override
      public void visitAssignmentExpression(AssignmentExpressionTree tree) {
        assignments.computeIfAbsent(((IdentifierTree) tree.variable()).name(), name -> new ArrayList<>()).add(tree);
        super.visitAssignmentExpression(tree);
      }
    });
  }

  @Test
  void assignments_in_exclusive_branches_count_once() {
    assertThat(assignedOnce("ifElse")).isTrue();
    assertThat(assignedOnce("arrows")).isTrue();
  }

  @Test
  void repeated_or_sequential_assignments_do_not() {
    assertThat(assignedOnce("sequence")).isFalse();
    assertThat(assignedOnce("loop")).isFalse();
    assertThat(assignedOnce("colons")).isFalse();
    assertThat(assignedOnce("sameArm")).isFalse();
    assertThat(assignedOnce("noElse")).isFalse();
    assertThat(assignedOnce("init")).isFalse();
    assertThat(assignedOnce("nested")).isFalse();
  }

  @Test
  void a_for_initializer_declares_a_local() {
    assertThat(EffectivelyFinalVariables.isLocalDeclaration(variables.get("init"))).isTrue();
    assertThat(EffectivelyFinalVariables.isLocalDeclaration(variables.get("k"))).isFalse();
  }

  private boolean assignedOnce(String name) {
    return EffectivelyFinalVariables.assignedOnce(variables.get(name), assignments.get(name));
  }
}
