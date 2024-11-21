/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.expression;

import java.util.Objects;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

public abstract class InternalUnaryExpression extends AssessableExpressionTree implements UnaryExpressionTree {

  protected final Kind kind;
  protected final InternalSyntaxToken operatorToken;
  protected final ExpressionTree expression;

  InternalUnaryExpression(Kind kind, InternalSyntaxToken operatorToken, ExpressionTree expression) {
    this.kind = Objects.requireNonNull(kind);
    this.operatorToken = operatorToken;
    this.expression = Objects.requireNonNull(expression);
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
}
