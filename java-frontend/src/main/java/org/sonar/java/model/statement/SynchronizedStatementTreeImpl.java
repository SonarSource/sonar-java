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
package org.sonar.java.model.statement;

import java.util.Arrays;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Objects;

public class SynchronizedStatementTreeImpl extends JavaTree implements SynchronizedStatementTree {
  private final ExpressionTree expression;
  private final BlockTree block;
  private final InternalSyntaxToken synchronizedKeyword;
  private final InternalSyntaxToken openParenToken;
  private final InternalSyntaxToken closeParenToken;

  public SynchronizedStatementTreeImpl(InternalSyntaxToken synchronizedKeyword, InternalSyntaxToken openParenToken,
    ExpressionTree expression, InternalSyntaxToken closeParenToken, BlockTreeImpl block) {
    this.expression = Objects.requireNonNull(expression);
    this.block = Objects.requireNonNull(block);
    this.synchronizedKeyword = synchronizedKeyword;
    this.openParenToken = openParenToken;
    this.closeParenToken = closeParenToken;
  }

  @Override
  public Kind kind() {
    return Kind.SYNCHRONIZED_STATEMENT;
  }

  @Override
  public SyntaxToken synchronizedKeyword() {
    return synchronizedKeyword;
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
  public BlockTree block() {
    return block;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitSynchronizedStatement(this);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(
      synchronizedKeyword,
      openParenToken,
      expression,
      closeParenToken,
      block);
  }

}
