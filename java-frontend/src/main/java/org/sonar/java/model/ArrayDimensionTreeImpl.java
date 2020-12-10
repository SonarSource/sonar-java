/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.model;

import java.util.ArrayList;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class ArrayDimensionTreeImpl extends JavaTree implements ArrayDimensionTree {

  private List<AnnotationTree> annotations;
  private final SyntaxToken openBracketToken;
  @Nullable
  private final ExpressionTree expression;
  private final SyntaxToken closeBracketToken;

  public ArrayDimensionTreeImpl(SyntaxToken openBracketToken, @Nullable ExpressionTree expression, SyntaxToken closeBracketToken) {
    this.annotations = Collections.emptyList();
    this.openBracketToken = openBracketToken;
    this.expression = expression;
    this.closeBracketToken = closeBracketToken;
  }

  public ArrayDimensionTreeImpl completeAnnotations(List<AnnotationTree> annotations) {
    this.annotations = Collections.unmodifiableList(annotations);
    return this;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitArrayDimension(this);
  }

  @Override
  public List<AnnotationTree> annotations() {
    return annotations;
  }

  @Override
  public SyntaxToken openBracketToken() {
    return openBracketToken;
  }

  @Nullable
  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken closeBracketToken() {
    return closeBracketToken;
  }

  @Override
  public Tree.Kind kind() {
    return Tree.Kind.ARRAY_DIMENSION;
  }

  @Override
  public List<Tree> children() {
    List<Tree> list = new ArrayList<>(annotations);
    list.add(openBracketToken);
    if (expression != null) {
      list.add(expression);
    }
    list.add(closeBracketToken);
    return Collections.unmodifiableList(list);
  }
}
