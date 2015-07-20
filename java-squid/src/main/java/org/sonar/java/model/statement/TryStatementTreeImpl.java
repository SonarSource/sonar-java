/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

public class TryStatementTreeImpl extends JavaTree implements TryStatementTree {

  private InternalSyntaxToken tryToken;

  @Nullable
  private InternalSyntaxToken openParenToken;
  private ListTree<VariableTree> resources;
  @Nullable
  private InternalSyntaxToken closeParenToken;

  private BlockTree block;
  private List<CatchTree> catches;

  @Nullable
  private final InternalSyntaxToken finallyKeyword;
  @Nullable
  private final BlockTreeImpl finallyBlock;

  public TryStatementTreeImpl(List<CatchTreeImpl> catches, @Nullable InternalSyntaxToken finallyKeyword, @Nullable BlockTreeImpl finallyBlock) {
    super(Kind.TRY_STATEMENT);

    this.openParenToken = null;
    this.resources = ResourceListTreeImpl.emptyList();
    this.closeParenToken = null;

    this.catches = getCatches(catches);
    this.finallyKeyword = finallyKeyword;
    this.finallyBlock = finallyBlock;
  }

  public TryStatementTreeImpl(InternalSyntaxToken finallyKeyword, BlockTreeImpl finallyBlock) {
    this(ImmutableList.<CatchTreeImpl>of(), finallyKeyword, finallyBlock);
  }

  public TryStatementTreeImpl(
    InternalSyntaxToken tryToken,
    InternalSyntaxToken openParenToken, ResourceListTreeImpl resources, InternalSyntaxToken closeParenToken,
    BlockTreeImpl block,
    List<CatchTreeImpl> catches) {

    super(Kind.TRY_STATEMENT);

    this.tryToken = tryToken;
    this.openParenToken = openParenToken;
    this.resources = resources;
    this.closeParenToken = closeParenToken;
    this.block = block;
    this.catches = getCatches(catches);
    this.finallyKeyword = null;
    this.finallyBlock = null;
  }

  public TryStatementTreeImpl completeWithCatches(List<CatchTreeImpl> catches) {
    this.catches = getCatches(catches);

    return this;
  }

  public TryStatementTreeImpl completeStandardTry(InternalSyntaxToken tryToken, BlockTreeImpl block) {
    this.tryToken = tryToken;
    this.block = block;

    return this;
  }

  public TryStatementTreeImpl completeTryWithResources(InternalSyntaxToken tryToken, InternalSyntaxToken openParenToken, ResourceListTreeImpl resources,
    InternalSyntaxToken closeParenToken, BlockTreeImpl block, List<CatchTreeImpl> catches) {
    this.tryToken = tryToken;
    this.openParenToken = openParenToken;
    this.resources = resources;
    this.closeParenToken = closeParenToken;
    this.block = block;
    this.catches = getCatches(catches);

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
  public ListTree<VariableTree> resources() {
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
  public Iterator<Tree> childrenIterator() {
    ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.builder();
    iteratorBuilder.add(tryToken);
    if (openParenToken != null) {
      iteratorBuilder.add(openParenToken);
      iteratorBuilder.add(resources);
      iteratorBuilder.add(closeParenToken);
    }
    iteratorBuilder.add(block);
    iteratorBuilder.addAll(catches);
    if (finallyKeyword != null) {
      iteratorBuilder.add(finallyKeyword, finallyBlock);
    }
    return iteratorBuilder.build().iterator();
  }

  private static List<CatchTree> getCatches(List<CatchTreeImpl> catches) {
    return TryStatementTreeImpl.<CatchTree>getList(catches);
  }

  private static <T> List<T> getList(List<? extends T> list) {
    return ImmutableList.<T>builder().addAll(list).build();
  }
}
