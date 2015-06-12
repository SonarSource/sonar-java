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
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.Iterator;
import java.util.List;

public class TypeCastExpressionTreeImpl extends AbstractTypedTree implements TypeCastTree {

  private InternalSyntaxToken openParenToken;
  private final TypeTree type;
  private final InternalSyntaxToken closeParenToken;
  private final ExpressionTree expression;

  public TypeCastExpressionTreeImpl(TypeTree type, InternalSyntaxToken closeParenToken, ExpressionTree expression) {
    super(Kind.TYPE_CAST);
    this.type = Preconditions.checkNotNull(type);
    this.closeParenToken = closeParenToken;
    this.expression = Preconditions.checkNotNull(expression);

    addChild((AstNode) type);
    addChild(closeParenToken);
    addChild((AstNode) expression);
  }

  public TypeCastExpressionTreeImpl(TypeTree type, InternalSyntaxToken closeParenToken, ExpressionTree expression, List<AstNode> children) {
    super(Kind.TYPE_CAST);
    this.type = Preconditions.checkNotNull(type);
    this.closeParenToken = closeParenToken;
    this.expression = Preconditions.checkNotNull(expression);

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public TypeCastExpressionTreeImpl complete(InternalSyntaxToken openParenToken) {
    Preconditions.checkState(this.openParenToken == null && closeParenToken != null);
    this.openParenToken = openParenToken;

    prependChildren(openParenToken);

    return this;
  }

  @Override
  public Kind getKind() {
    return Kind.TYPE_CAST;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTypeCast(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(
      type,
      expression
      );
  }

}
