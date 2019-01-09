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

import com.google.common.collect.Lists;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class ConditionalExpressionTreeImpl extends AbstractTypedTree implements ConditionalExpressionTree {

  private ExpressionTree condition;
  private final InternalSyntaxToken queryToken;
  private final ExpressionTree trueExpression;
  private final InternalSyntaxToken colonToken;
  private final ExpressionTree falseExpression;

  public ConditionalExpressionTreeImpl(InternalSyntaxToken queryToken, ExpressionTree trueExpression, InternalSyntaxToken colonToken, ExpressionTree falseExpression) {

    super(Kind.CONDITIONAL_EXPRESSION);
    this.queryToken = queryToken;
    this.trueExpression = trueExpression;
    this.colonToken = colonToken;
    this.falseExpression = falseExpression;
  }

  public ConditionalExpressionTreeImpl complete(ExpressionTree condition) {
    this.condition = condition;
    return this;
  }

  @Override
  public Kind kind() {
    return Kind.CONDITIONAL_EXPRESSION;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken questionToken() {
    return queryToken;
  }

  @Override
  public ExpressionTree trueExpression() {
    return trueExpression;
  }

  @Override
  public SyntaxToken colonToken() {
    return colonToken;
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
  public Iterable<Tree> children() {
    return Lists.newArrayList(
      condition,
      queryToken,
      trueExpression,
      colonToken,
      falseExpression
    );
  }
}
