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
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;

public class ConditionalExpressionTreeImpl extends AbstractTypedTree implements ConditionalExpressionTree {
  private final ExpressionTree condition;
  private final ExpressionTree trueExpression;
  private final ExpressionTree falseExpression;

  public ConditionalExpressionTreeImpl(AstNode astNode, ExpressionTree condition, ExpressionTree trueExpression, ExpressionTree falseExpression) {
    super(astNode);
    this.condition = Preconditions.checkNotNull(condition);
    this.trueExpression = Preconditions.checkNotNull(trueExpression);
    this.falseExpression = Preconditions.checkNotNull(falseExpression);
  }

  @Override
  public Kind getKind() {
    return Kind.CONDITIONAL_EXPRESSION;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken questionToken() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.QUERY).getToken());
  }

  @Override
  public ExpressionTree trueExpression() {
    return trueExpression;
  }

  @Override
  public SyntaxToken colonToken() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.COLON).getToken());
  }

  @Override
  public ExpressionTree falseExpression() {
    return falseExpression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitConditionalExpression(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.<Tree>forArray(
      condition,
      trueExpression,
      falseExpression
    );
  }
}
