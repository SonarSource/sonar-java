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
package org.sonar.java.model.expression;

import java.util.Arrays;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.Objects;

public class InternalPostfixUnaryExpression extends AssessableExpressionTree implements UnaryExpressionTree {

  private final Kind kind;
  private final ExpressionTree expression;
  private final InternalSyntaxToken operatorToken;

  public InternalPostfixUnaryExpression(Kind kind, ExpressionTree expression, InternalSyntaxToken operatorToken) {
    this.kind = Objects.requireNonNull(kind);
    this.expression = Objects.requireNonNull(expression);
    this.operatorToken = operatorToken;
  }

  @Override
  public Kind kind() {
    return kind;
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
    visitor.visitUnaryExpression(this);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(
      expression,
      operatorToken
    );
  }

}
