/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.se;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph.Block;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CheckerDispatcherTest {

  @Test
  void test_only_one_execution_of_post_statement_by_check() {
    List<SECheck> checks = Arrays.asList(new NullDereferenceCheck(), new CheckTest(), new CheckTest(), new CheckTest());
    CheckerDispatcher checkerDispatcher = new CheckerDispatcher(mockExplodedGraphWalker(), checks, null);
    checkerDispatcher.executeCheckPostStatement(mock(Tree.class));
    for (SECheck check : checks) {
      if(check instanceof CheckTest) {
        assertThat(((CheckTest) check).postStatementExecution).isEqualTo(1);
      }
    }
  }

  private static ExplodedGraphWalker mockExplodedGraphWalker() {
    ExplodedGraphWalker explodedGraphWalker = mock(ExplodedGraphWalker.class);
    explodedGraphWalker.programPosition = new ProgramPoint(mock(Block.class));
    explodedGraphWalker.programState = mock(ProgramState.class);
    explodedGraphWalker.node = new ExplodedGraph().node(explodedGraphWalker.programPosition, explodedGraphWalker.programState);
    return explodedGraphWalker;
  }

  private static class CheckTest extends SECheck {
    int postStatementExecution = 0;

    @Override
    public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
      postStatementExecution++;
      return mock(ProgramState.class);
    }
  }
}
