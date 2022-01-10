/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.model.statement;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TryStatementTree;

public class TryStatementTreeImpl extends JavaTree implements TryStatementTree {

  private InternalSyntaxToken tryToken;

  @Nullable
  private InternalSyntaxToken openParenToken;
  private ListTree<Tree> resources;
  @Nullable
  private InternalSyntaxToken closeParenToken;

  private BlockTree block;
  private List<CatchTree> catches;

  @Nullable
  private final InternalSyntaxToken finallyKeyword;
  @Nullable
  private final BlockTreeImpl finallyBlock;

  public TryStatementTreeImpl(
    InternalSyntaxToken tryToken,
    @Nullable InternalSyntaxToken openParenToken,
    ResourceListTreeImpl resources,
    @Nullable InternalSyntaxToken closeParenToken,
    BlockTreeImpl block,
    List<CatchTree> catches,
    @Nullable InternalSyntaxToken finallyKeyword,
    @Nullable BlockTreeImpl finallyBlock
  ) {
    this.tryToken = tryToken;
    this.openParenToken = openParenToken;
    this.resources = resources;
    this.closeParenToken = closeParenToken;
    this.block = block;
    this.catches = catches;
    this.finallyKeyword = finallyKeyword;
    this.finallyBlock = finallyBlock;
  }

  @Override
  public Kind kind() {
    return Kind.TRY_STATEMENT;
  }

  @Override
  public SyntaxToken tryKeyword() {
    return tryToken;
  }

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public ListTree<Tree> resourceList() {
    return resources;
  }

  @Nullable
  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public BlockTree block() {
    return block;
  }

  @Override
  public List<CatchTree> catches() {
    return catches;
  }

  @Nullable
  @Override
  public SyntaxToken finallyKeyword() {
    if (finallyBlock == null) {
      return null;
    }

    return finallyKeyword;
  }

  @Nullable
  @Override
  public BlockTree finallyBlock() {
    return finallyBlock;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTryStatement(this);
  }

  @Override
  public List<Tree> children() {
    List<Tree> list = new ArrayList<>();
    list.add(tryToken);
    if (openParenToken != null) {
      list.add(openParenToken);
      list.add(resources);
      list.add(closeParenToken);
    }
    list.add(block);
    list.addAll(catches);
    if (finallyKeyword != null) {
      list.add(finallyKeyword);
      list.add(finallyBlock);
    }
    return list;
  }
}
