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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class JWarningTest {

  private static final Tree TREE = new InternalSyntaxToken(1, 0, "x", List.of(), false);
  private static final Tree OTHER_TREE = new InternalSyntaxToken(1, 0, "x", List.of(), false);

  @Test
  void warnings_are_equal_only_on_the_same_tree() {
    JWarning warning = new JWarning("m", ProblemKind.UNUSED_IMPORT, TREE);

    assertThat(warning.syntaxTree()).isSameAs(TREE);
    assertThat(warning)
      .isEqualTo(new JWarning("m", ProblemKind.UNUSED_IMPORT, TREE))
      .hasSameHashCodeAs(new JWarning("m", ProblemKind.UNUSED_IMPORT, TREE))
      .isNotEqualTo(new JWarning("m", ProblemKind.UNUSED_IMPORT, OTHER_TREE))
      .isNotEqualTo(new JWarning("other", ProblemKind.UNUSED_IMPORT, TREE))
      .isNotEqualTo(new JProblem("m", ProblemKind.UNUSED_IMPORT));
  }
}
