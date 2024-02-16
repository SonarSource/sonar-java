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
package org.sonar.java.model.expression;

import org.sonar.java.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

public class NewArrayTreeImpl extends AssessableExpressionTree implements NewArrayTree {

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
    this.dimensions = Stream.of(arrayDimensions, dimensions).flatMap(List::stream).toList();
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
  public List<Tree> children() {
    List<Tree> list = new ArrayList<>();
    addIfNotNull(list, newKeyword);
    addIfNotNull(list, type);
    list.addAll(dimensions);
    addIfNotNull(list, openCurlyBraceToken);
    list.add(initializers);
    addIfNotNull(list, closeCurlyBraceToken);
    return Collections.unmodifiableList(list);
  }

  @Override
  public SyntaxToken newKeyword() {
    return newKeyword;
  }

  private static List<Tree> addIfNotNull(List<Tree> list, @Nullable Tree tree) {
    if (tree != null) {
      list.add(tree);
    }
    return list;
  }
}
