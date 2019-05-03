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
package org.sonar.java.ast.parser;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ListTreeImplTest {


  @Test
  public void separators_order_in_children_iteration() throws Exception {
    Tree tree1 = new EmptyStatementTreeImpl(null);
    Tree tree2 = new EmptyStatementTreeImpl(null);
    Tree tree3 = new EmptyStatementTreeImpl(null);
    List<Tree> trees = Lists.newArrayList(tree1, tree2, tree3);
    SyntaxToken token1 = createToken("token1");
    SyntaxToken token2 = createToken("token2");
    List<SyntaxToken> separators = Lists.newArrayList(token1, token2);
    ListTreeImpl<Tree> listTree = new MyList(trees, separators);
    Iterable<Tree> result = listTree.children();
    assertThat(Lists.newArrayList(result)).containsExactly(tree1, token1, tree2, token2, tree3);
  }

  @Test
  public void emptySeparators() throws Exception {
    Tree tree1 = new EmptyStatementTreeImpl(null);
    List<Tree> trees = Lists.newArrayList(tree1);
    List<SyntaxToken> separators = new ArrayList<>();
    ListTreeImpl<Tree> listTree = new MyList(trees, separators);
    Iterable<Tree> result = listTree.children();
    assertThat(Lists.newArrayList(result)).containsExactly(tree1);
  }

  private static class MyList extends ListTreeImpl<Tree> {
    public MyList(List<Tree> list, List<SyntaxToken> tokens) {
      super(null, list, tokens);
    }
  }

  private SyntaxToken createToken(String value) {
    return new InternalSyntaxToken(1,1, value, new ArrayList<>(), 0,0,false);
  }
}
