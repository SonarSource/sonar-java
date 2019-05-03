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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;

public class IfStatementTreeImpl extends JavaTree implements IfStatementTree {

  private InternalSyntaxToken ifKeyword;
  private InternalSyntaxToken openParenToken;
  private ExpressionTree condition;
  private InternalSyntaxToken closeParenToken;
  private StatementTree thenStatement;
  @Nullable
  private final InternalSyntaxToken elseKeyword;
  @Nullable
  private final StatementTree elseStatement;

  public IfStatementTreeImpl(InternalSyntaxToken elseKeyword, StatementTree elseStatement) {
    super(Kind.IF_STATEMENT);
    this.elseKeyword = elseKeyword;
    this.elseStatement = Objects.requireNonNull(elseStatement);
  }

  public IfStatementTreeImpl(InternalSyntaxToken ifKeyword, InternalSyntaxToken openParenToken, ExpressionTree condition, InternalSyntaxToken closeParenToken,
    StatementTree thenStatement) {

    super(Kind.IF_STATEMENT);
    this.ifKeyword = ifKeyword;
    this.openParenToken = openParenToken;
    this.condition = Objects.requireNonNull(condition);
    this.closeParenToken = closeParenToken;
    this.thenStatement = Objects.requireNonNull(thenStatement);
    this.elseStatement = null;
    this.elseKeyword = null;
  }

  public IfStatementTreeImpl complete(InternalSyntaxToken ifKeyword, InternalSyntaxToken openParenToken, ExpressionTree condition, InternalSyntaxToken closeParenToken,
    StatementTree thenStatement) {
    Preconditions.checkState(this.condition == null, "Already completed");
    this.ifKeyword = ifKeyword;
    this.openParenToken = openParenToken;
    this.condition = Objects.requireNonNull(condition);
    this.closeParenToken = closeParenToken;
    this.thenStatement = Objects.requireNonNull(thenStatement);

    return this;
  }

  @Override
  public Kind kind() {
    return Kind.IF_STATEMENT;
  }

  @Override
  public SyntaxToken ifKeyword() {
    return ifKeyword;
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
  public StatementTree thenStatement() {
    return thenStatement;
  }

  @Nullable
  @Override
  public SyntaxToken elseKeyword() {
    return elseKeyword;
  }

  @Nullable
  @Override
  public StatementTree elseStatement() {
    return elseStatement;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitIfStatement(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Iterables.concat(
      Lists.newArrayList(ifKeyword, openParenToken, condition, closeParenToken, thenStatement),
      elseKeyword != null ? Lists.newArrayList(elseKeyword, elseStatement) : Collections.<Tree>emptyList());
  }
}
