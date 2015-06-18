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
package org.sonar.java.model.expression;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

public class NewArrayTreeImpl extends AbstractTypedTree implements NewArrayTree {

  @Nullable
  private Tree type;
  @Nullable
  private SyntaxToken newKeyword;
  private final List<ExpressionTree> dimensions;
  @Nullable
  private SyntaxToken openCurlyBraceToken;
  private final List<ExpressionTree> initializers;
  @Nullable
  private SyntaxToken closeCurlyBraceToken;

  public NewArrayTreeImpl(List<ExpressionTree> dimensions, List<ExpressionTree> initializers, List<AstNode> children) {
    super(Kind.NEW_ARRAY);

    // TODO maybe type should not be null?
    this.type = null;
    this.dimensions = Preconditions.checkNotNull(dimensions);
    this.initializers = Preconditions.checkNotNull(initializers);

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public NewArrayTreeImpl complete(Tree type, AstNode... children) {
    Preconditions.checkState(this.type == null);
    this.type = type;

    prependChildren(children);

    return this;
  }

  public NewArrayTreeImpl completeWithNewKeyword(SyntaxToken newKeyword) {
    this.newKeyword = newKeyword;
    return this;
  }

  public NewArrayTreeImpl completeWithCurlyBraces(SyntaxToken openCurlyBraceToken, SyntaxToken closeCurlyBraceToken) {
    this.openCurlyBraceToken = openCurlyBraceToken;
    this.closeCurlyBraceToken = closeCurlyBraceToken;
    return this;
  }

  @Override
  public Kind getKind() {
    return Kind.NEW_ARRAY;
  }

  @Override
  public Tree type() {
    return type;
  }

  @Override
  public List<ExpressionTree> dimensions() {
    return dimensions;
  }

  @Override
  public List<ExpressionTree> initializers() {
    return initializers;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitNewArray(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.<Tree>builder();
    addIfNotNull(iteratorBuilder, newKeyword);
    addIfNotNull(iteratorBuilder, type);
    // TODO SONARJAVA-547 Brackets of dimensions are missing
    iteratorBuilder.addAll(dimensions);
    addIfNotNull(iteratorBuilder, openCurlyBraceToken);
    iteratorBuilder.addAll(initializers);
    addIfNotNull(iteratorBuilder, closeCurlyBraceToken);
    return iteratorBuilder.build().iterator();
  }

  @Override
  public SyntaxToken newKeyword() {
    return newKeyword;
  }

  private static ImmutableList.Builder<Tree> addIfNotNull(ImmutableList.Builder<Tree> builder, Tree tree) {
    if (tree != null) {
      builder.add(tree);
    }
    return builder;
  }
}
