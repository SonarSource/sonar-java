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
import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;
import java.util.List;

public class NewArrayTreeImpl extends AbstractTypedTree implements NewArrayTree {
  private final Tree type;
  private final List<ExpressionTree> dimensions;
  private final List<ExpressionTree> initializers;

  public NewArrayTreeImpl(AstNode astNode, Tree type, List<ExpressionTree> dimensions, List<ExpressionTree> initializers) {
    super(astNode);
    // TODO maybe type should not be null?
    this.type = type;
    this.dimensions = Preconditions.checkNotNull(dimensions);
    this.initializers = Preconditions.checkNotNull(initializers);
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
    return Iterators.concat(
      Iterators.singletonIterator(type),
      dimensions.iterator(),
      initializers.iterator()
    );
  }
}
