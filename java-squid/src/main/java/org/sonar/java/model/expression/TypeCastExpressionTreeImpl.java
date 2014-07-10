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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import java.util.Iterator;

public class TypeCastExpressionTreeImpl extends AbstractTypedTree implements TypeCastTree {
  private final Tree type;
  private final ExpressionTree expression;

  public TypeCastExpressionTreeImpl(AstNode astNode, Tree type, ExpressionTree expression) {
    super(astNode);
    this.type = Preconditions.checkNotNull(type);
    this.expression = Preconditions.checkNotNull(expression);
  }

  @Override
  public Kind getKind() {
    return Kind.TYPE_CAST;
  }

  @Override
  public SyntaxToken openParenToken() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.LPAR).getToken());
  }

  @Override
  public Tree type() {
    return type;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.RPAR).getToken());
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
