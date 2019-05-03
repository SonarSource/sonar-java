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
package org.sonar.java.model.statement;

import com.google.common.collect.Lists;
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

    super(Kind.DO_STATEMENT);
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
  public Iterable<Tree> children() {
    return Lists.newArrayList(
      doKeyword,
      statement,
      whileKeyword,
      openParenToken,
      condition,
      closeParenToken,
      semicolonToken);
  }

}
