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
package org.sonar.java.se;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CheckerDispatcherTest {

  @Test
  public void test_only_one_execution_of_post_statement_by_check() {
    List<SECheck> checks = Lists.newArrayList(new NullDereferenceCheck(), new CheckTest(), new CheckTest(), new CheckTest());
    CheckerDispatcher checkerDispatcher = new CheckerDispatcher(mockExplodedGraphWalker(), checks);
    checkerDispatcher.executeCheckPostStatement(mock(Tree.class));
    for (SECheck check : checks) {
      if(check instanceof CheckTest) {
        assertThat(((CheckTest) check).postStatementExecution).isEqualTo(1);
      }
    }
  }

  private static ExplodedGraphWalker mockExplodedGraphWalker() {
    ExplodedGraphWalker explodedGraphWalker = mock(ExplodedGraphWalker.class);
    explodedGraphWalker.programPosition = new ProgramPoint(new CFG.Block(1));
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
