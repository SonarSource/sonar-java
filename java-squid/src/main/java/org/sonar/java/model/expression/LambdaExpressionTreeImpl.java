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

import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class LambdaExpressionTreeImpl extends AbstractTypedTree implements LambdaExpressionTree {

  private final List<VariableTree> parameters;
  private final Tree body;

  public LambdaExpressionTreeImpl(AstNode astNode, List<VariableTree> parameters, Tree body) {
    super(astNode);
    this.parameters = parameters;
    this.body = body;
  }

  @Override
  public Kind getKind() {
    return Kind.LAMBDA_EXPRESSION;
  }

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<VariableTree> parameters() {
    return parameters;
  }

  @Nullable
  @Override
  public SyntaxToken closeParenToken() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Tree body() {
    return body;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitLambdaExpression(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      parameters.iterator(),
      Iterators.singletonIterator(body)
    );
  }
}
