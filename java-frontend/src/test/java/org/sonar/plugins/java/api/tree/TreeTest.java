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
package org.sonar.plugins.java.api.tree;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TreeTest {

  @Test
  void test() {
    assertThat(Tree.Kind.values()).hasSize(129);
  }

  @Test
  void class_kinds_contains_exactly_all_kinds_backed_by_class_tree() {
    Tree.Kind[] expected = Arrays.stream(Tree.Kind.values())
      .filter(kind -> kind.getAssociatedInterface() == ClassTree.class)
      .toArray(Tree.Kind[]::new);
    assertThat(Tree.Kind.CLASS_KINDS).containsExactlyInAnyOrder(expected);
  }

  @Test
  void assignment_kinds_contains_exactly_all_kinds_backed_by_assignment_expression_tree() {
    Tree.Kind[] expected = Arrays.stream(Tree.Kind.values())
      .filter(kind -> kind.getAssociatedInterface() == AssignmentExpressionTree.class)
      .toArray(Tree.Kind[]::new);
    assertThat(Tree.Kind.ASSIGNMENT_KINDS).containsExactlyInAnyOrder(expected);
  }

}
