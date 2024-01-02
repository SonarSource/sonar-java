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
package org.sonar.java.model.declaration;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.ast.parser.ListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListTreeImplTest {


  @Test
  void separators_order_in_children_iteration() throws Exception {
    Tree tree1 = new EmptyStatementTreeImpl(null);
    Tree tree2 = new EmptyStatementTreeImpl(null);
    Tree tree3 = new EmptyStatementTreeImpl(null);
    List<Tree> trees = Arrays.asList(tree1, tree2, tree3);
    SyntaxToken token1 = createToken("token1");
    SyntaxToken token2 = createToken("token2");
    List<SyntaxToken> separators = Arrays.asList(token1, token2);
    ListTreeImpl<Tree> listTree = new MyList(trees, separators);

    assertThat(listTree.children()).containsExactly(tree1, token1, tree2, token2, tree3);
  }

  @Test
  void emptySeparators() throws Exception {
    Tree tree1 = new EmptyStatementTreeImpl(null);
    List<Tree> trees = Collections.singletonList(tree1);
    List<SyntaxToken> separators = new ArrayList<>();
    ListTreeImpl<Tree> listTree = new MyList(trees, separators);

    assertThat(listTree.children()).containsExactly(tree1);
  }

  private static class MyList extends ListTreeImpl<Tree> {
    public MyList(List<Tree> list, List<SyntaxToken> tokens) {
      super(list, tokens);
    }
  }

  private SyntaxToken createToken(String value) {
    return new InternalSyntaxToken(1, 1, value, new ArrayList<>(), false);
  }
}
