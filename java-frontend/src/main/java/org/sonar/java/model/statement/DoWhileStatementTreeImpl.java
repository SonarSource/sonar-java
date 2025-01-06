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
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Objects;

public class DoWhileStatementTreeImpl extends JavaTree implements DoWhileStatementTree {
  private final StatementTree statement;
  private final ExpressionTree condition;
  private final InternalSyntaxToken semicolonToken;
  private final InternalSyntaxToken doKeyword;
  private final InternalSyntaxToken whileKeyword;
  private final InternalSyntaxToken openParenToken;
  private final InternalSyntaxToken closeParenToken;

  public DoWhileStatementTreeImpl(InternalSyntaxToken doKeyword, StatementTree statement,
    InternalSyntaxToken whileKeyword, InternalSyntaxToken openParenToken, ExpressionTree condition, InternalSyntaxToken closeParenToken,
    InternalSyntaxToken semicolonToken) {
    this.statement = Objects.requireNonNull(statement);
    this.condition = Objects.requireNonNull(condition);
    this.doKeyword = doKeyword;
    this.whileKeyword = whileKeyword;
    this.openParenToken = openParenToken;
    this.semicolonToken = semicolonToken;
    this.closeParenToken = closeParenToken;
  }

  @Override
  public Kind kind() {
    return Kind.DO_STATEMENT;
  }

  @Override
  public SyntaxToken doKeyword() {
    return doKeyword;
  }

  @Override
  public StatementTree statement() {
    return statement;
  }

  @Override
  public SyntaxToken whileKeyword() {
    return whileKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitDoWhileStatement(this);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(
      doKeyword,
      statement,
      whileKeyword,
      openParenToken,
      condition,
      closeParenToken,
      semicolonToken);
  }

}
