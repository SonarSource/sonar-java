/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.model.statement;

import java.util.Arrays;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Objects;

public class ForEachStatementImpl extends JavaTree implements ForEachStatement {
  private final InternalSyntaxToken forKeyword;
  private final InternalSyntaxToken openParenToken;
  private final VariableTree variable;
  private final InternalSyntaxToken colonToken;
  private final ExpressionTree expression;
  private final InternalSyntaxToken closeParenToken;
  private final StatementTree statement;

  public ForEachStatementImpl(InternalSyntaxToken forKeyword, InternalSyntaxToken openParenToken, VariableTreeImpl variable, InternalSyntaxToken colonToken,
    ExpressionTree expression, InternalSyntaxToken closeParenToken, StatementTree statement) {
    this.forKeyword = forKeyword;
    this.openParenToken = openParenToken;
    this.variable = Objects.requireNonNull(variable);
    this.colonToken = colonToken;
    this.expression = Objects.requireNonNull(expression);
    this.closeParenToken = closeParenToken;
    this.statement = Objects.requireNonNull(statement);
  }

  @Override
  public Kind kind() {
    return Kind.FOR_EACH_STATEMENT;
  }

  @Override
  public SyntaxToken forKeyword() {
    return forKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public VariableTree variable() {
    return variable;
  }

  @Override
  public SyntaxToken colonToken() {
    return colonToken;
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
  public StatementTree statement() {
    return statement;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitForEachStatement(this);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(
      forKeyword,
      openParenToken,
      variable,
      colonToken,
      expression,
      closeParenToken,
      statement);
  }

}
