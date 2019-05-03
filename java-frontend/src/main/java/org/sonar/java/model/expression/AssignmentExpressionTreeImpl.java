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
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Objects;

public class AssignmentExpressionTreeImpl extends AbstractTypedTree implements AssignmentExpressionTree {

  private final Kind kind;

  private final ExpressionTree variable;
  private final InternalSyntaxToken operatorToken;
  private final ExpressionTree expression;

  public AssignmentExpressionTreeImpl(Kind kind, ExpressionTree variable, InternalSyntaxToken operatorToken, ExpressionTree expression) {
    super(kind);
    this.kind = kind;

    this.variable = variable;
    this.operatorToken = operatorToken;
    this.expression = Objects.requireNonNull(expression);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  public ExpressionTree variable() {
    return variable;
  }

  @Override
  public SyntaxToken operatorToken() {
    return operatorToken;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAssignmentExpression(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Lists.newArrayList(
      variable,
      operatorToken,
      expression
    );
  }

}
