/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model.statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class TryStatementTreeImpl extends JavaTree implements TryStatementTree {

  private InternalSyntaxToken tryToken;

  @Nullable
  private final InternalSyntaxToken openParenToken;
  private final List<VariableTree> resources;
  @Nullable
  private final InternalSyntaxToken closeParenToken;

  private BlockTree block;
  private final List<CatchTreeImpl> catches;

  @Nullable
  private final BlockTreeImpl finallyBlock;

  public TryStatementTreeImpl(List<CatchTreeImpl> catches, @Nullable BlockTreeImpl finallyBlock) {
    super(Kind.TRY_STATEMENT);

    this.openParenToken = null;
    this.resources = ImmutableList.<VariableTree>of();
    this.closeParenToken = null;

    this.catches = catches;
    this.finallyBlock = finallyBlock;

    for (CatchTreeImpl catch_ : catches) {
      addChild(catch_);
    }

    if (finallyBlock != null) {
      addChild(finallyBlock);
    }
  }

  public TryStatementTreeImpl(BlockTreeImpl finallyBlock) {
    this(ImmutableList.<CatchTreeImpl>of(), finallyBlock);
  }

  public TryStatementTreeImpl(
    InternalSyntaxToken tryToken,
    InternalSyntaxToken openParenToken, ResourceListTreeImpl resources, InternalSyntaxToken closeParenToken,
    BlockTreeImpl block,
    List<CatchTreeImpl> catches, @Nullable BlockTreeImpl finallyBlock) {

    super(Kind.TRY_STATEMENT);

    this.tryToken = tryToken;
    this.openParenToken = openParenToken;
    this.resources = (List) resources;
    this.closeParenToken = closeParenToken;
    this.block = block;
    this.catches = catches;
    this.finallyBlock = finallyBlock;

    addChild(tryToken);
    addChild(openParenToken);
    addChild(resources);
    addChild(closeParenToken);
    addChild(block);

    for (CatchTreeImpl catch_ : catches) {
      addChild(catch_);
    }

    if (finallyBlock != null) {
      addChild(finallyBlock);
    }
  }

  public TryStatementTreeImpl completeStandardTry(InternalSyntaxToken tryToken, BlockTreeImpl block) {
    this.tryToken = tryToken;
    this.block = block;

    prependChildren(tryToken, block);

    return this;
  }

  @Override
  public Kind getKind() {
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
  public List<VariableTree> resources() {
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
    return (List) catches;
  }

  @Nullable
  @Override
  public SyntaxToken finallyKeyword() {
    if (finallyBlock == null) {
      return null;
    }

    return InternalSyntaxToken.createLegacy(finallyBlock.getFirstChild(JavaKeyword.FINALLY));
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
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      resources.iterator(),
      Iterators.singletonIterator(block),
      catches.iterator(),
      Iterators.singletonIterator(finallyBlock));
  }

}
