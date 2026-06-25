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
package org.sonar.java.cfg;

import org.junit.jupiter.api.Test;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class CFGUtilsTest {

  @Test
  void detects_distinct_loop_continuation_after_try_finally_continue() {
    CFG cfg = CFGTestUtils.buildCFG("""
      void test(boolean condition1, boolean condition2) {
        while (condition1) {
          try {
            if (condition2) {
              continue;
            }
            foo();
          } finally {
            bar();
          }
          foo();
        }
      }""");

    Block continueBlock = blockWithTerminator(cfg, Tree.Kind.CONTINUE_STATEMENT);
    Block successor = CFGUtils.nonEmptySuccessor(continueBlock.successors().iterator().next());

    assertThat(CFGUtils.isJumpThroughFinallyWithDistinctContinuation(continueBlock.terminator(), successor)).isTrue();
  }

  @Test
  void ignores_redundant_continue_when_finally_has_no_distinct_continuation() {
    CFG cfg = CFGTestUtils.buildCFG("""
      void test(boolean condition1) {
        while (condition1) {
          try {
            foo();
            continue;
          } finally {
            bar();
          }
        }
      }""");

    Block continueBlock = blockWithTerminator(cfg, Tree.Kind.CONTINUE_STATEMENT);
    Block successor = CFGUtils.nonEmptySuccessor(continueBlock.successors().iterator().next());

    assertThat(CFGUtils.isJumpThroughFinallyWithDistinctContinuation(continueBlock.terminator(), successor)).isFalse();
  }

  private static Block blockWithTerminator(CFG cfg, Tree.Kind kind) {
    return cfg.blocks().stream()
      .filter(block -> block.terminator() != null && block.terminator().is(kind))
      .findFirst()
      .orElseThrow();
  }
}
