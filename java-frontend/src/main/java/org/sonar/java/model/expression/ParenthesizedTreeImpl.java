/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Arrays;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Objects;

public class ParenthesizedTreeImpl extends AssessableExpressionTree implements ParenthesizedTree {
  private final InternalSyntaxToken openParenToken;
  private final ExpressionTree expression;
  private final InternalSyntaxToken closeParenToken;

  public ParenthesizedTreeImpl(InternalSyntaxToken openParenToken, ExpressionTree expression, InternalSyntaxToken closeParenToken) {
    this.openParenToken = openParenToken;
    this.expression = Objects.requireNonNull(expression);
    this.closeParenToken = closeParenToken;
  }

  @Override
  public Kind kind() {
    return Kind.PARENTHESIZED_EXPRESSION;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitParenthesized(this);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(
      openParenToken,
      expression,
      closeParenToken);
  }

}
