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
package org.sonar.java.model.expression;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.ArrayDimensionTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class NewArrayTreeImpl extends AbstractTypedTree implements NewArrayTree {

  @Nullable
  private TypeTree type;
  @Nullable
  private SyntaxToken newKeyword;
  private List<ArrayDimensionTree> dimensions;
  @Nullable
  private SyntaxToken openCurlyBraceToken;
  private final ListTree<ExpressionTree> initializers;
  @Nullable
  private SyntaxToken closeCurlyBraceToken;

  public NewArrayTreeImpl(List<ArrayDimensionTree> dimensions, ListTree<ExpressionTree> initializers) {
    super(Kind.NEW_ARRAY);

    // TODO maybe type should not be null?
    this.type = null;
    this.dimensions = Objects.requireNonNull(dimensions);
    this.initializers = Objects.requireNonNull(initializers);
  }

  public NewArrayTreeImpl complete(TypeTree type) {
    Preconditions.checkState(this.type == null);
    this.type = type;

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

  public NewArrayTreeImpl completeDimensions(List<ArrayDimensionTree> arrayDimensions) {
    this.dimensions = ImmutableList.<ArrayDimensionTree>builder().addAll(arrayDimensions).addAll(dimensions).build();
    return this;
  }

  public NewArrayTreeImpl completeFirstDimension(List<AnnotationTreeImpl> annotations) {
    ((ArrayDimensionTreeImpl) this.dimensions.get(0)).completeAnnotations(annotations);
    return this;
  }

  @Override
  public Kind kind() {
    return Kind.NEW_ARRAY;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public List<ArrayDimensionTree> dimensions() {
    return dimensions;
  }

  @Nullable
  @Override
  public SyntaxToken openBraceToken() {
    return openCurlyBraceToken;
  }

  @Override
  public ListTree<ExpressionTree> initializers() {
    return initializers;
  }

  @Nullable
  @Override
  public SyntaxToken closeBraceToken() {
    return closeCurlyBraceToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitNewArray(this);
  }

  @Override
  public Iterable<Tree> children() {
    ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.builder();
    addIfNotNull(iteratorBuilder, newKeyword);
    addIfNotNull(iteratorBuilder, type);
    iteratorBuilder.addAll(dimensions);
    addIfNotNull(iteratorBuilder, openCurlyBraceToken);
    iteratorBuilder.add(initializers);
    addIfNotNull(iteratorBuilder, closeCurlyBraceToken);
    return iteratorBuilder.build();
  }

  @Override
  public SyntaxToken newKeyword() {
    return newKeyword;
  }

  private static ImmutableList.Builder<Tree> addIfNotNull(ImmutableList.Builder<Tree> builder, @Nullable Tree tree) {
    if (tree != null) {
      builder.add(tree);
    }
    return builder;
  }
}
