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
    super(Kind.SYNCHRONIZED_STATEMENT);
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
  public Iterable<Tree> children() {
    return Lists.newArrayList(
      synchronizedKeyword,
      openParenToken,
      expression,
      closeParenToken,
      block);
  }

}
