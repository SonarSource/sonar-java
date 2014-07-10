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
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;

public class ArrayAccessExpressionTreeImpl extends AbstractTypedTree implements ArrayAccessExpressionTree {
  private final ExpressionTree expression;
  private final ExpressionTree index;

  public ArrayAccessExpressionTreeImpl(AstNode astNode, ExpressionTree expression, ExpressionTree index) {
    super(astNode);
    this.expression = Preconditions.checkNotNull(expression);
    this.index = Preconditions.checkNotNull(index);
  }

  @Override
  public Kind getKind() {
    return Kind.ARRAY_ACCESS_EXPRESSION;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken openBracketToken() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExpressionTree index() {
    return index;
  }

  @Override
  public SyntaxToken closeBracketToken() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitArrayAccessExpression(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.<Tree>forArray(
      expression,
      index
    );
  }
}
