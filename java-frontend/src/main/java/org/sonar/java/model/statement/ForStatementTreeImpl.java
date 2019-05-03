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

import com.google.common.collect.ImmutableList;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Objects;

public class ForStatementTreeImpl extends JavaTree implements ForStatementTree {
  private final InternalSyntaxToken forKeyword;
  private final InternalSyntaxToken openParenToken;
  private final ListTree<StatementTree> initializer;
  private final InternalSyntaxToken firstSemicolonToken;
  @Nullable
  private final ExpressionTree condition;
  private final InternalSyntaxToken secondSemicolonToken;
  private final ListTree<StatementTree> update;
  private final InternalSyntaxToken closeParenToken;
  private final StatementTree statement;

  public ForStatementTreeImpl(InternalSyntaxToken forKeyword, InternalSyntaxToken openParenToken, ListTree<StatementTree> initializer,
    InternalSyntaxToken firstSemicolonToken, ExpressionTree condition, InternalSyntaxToken secondSemicolonToken, ListTree<StatementTree> update,
    InternalSyntaxToken closeParenToken, StatementTree statement) {
    super(Kind.FOR_STATEMENT);
    this.forKeyword = forKeyword;
    this.openParenToken = openParenToken;
    this.initializer = Objects.requireNonNull(initializer);
    this.firstSemicolonToken = firstSemicolonToken;
    this.condition = condition;
    this.secondSemicolonToken = secondSemicolonToken;
    this.update = Objects.requireNonNull(update);
    this.closeParenToken = closeParenToken;
    this.statement = Objects.requireNonNull(statement);
  }

  @Override
  public Kind kind() {
    return Kind.FOR_STATEMENT;
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
  public ListTree<StatementTree> initializer() {
    return initializer;
  }

  @Override
  public SyntaxToken firstSemicolonToken() {
    return firstSemicolonToken;
  }

  @Nullable
  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken secondSemicolonToken() {
    return secondSemicolonToken;
  }

  @Override
  public ListTree<StatementTree> update() {
    return update;
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
    visitor.visitForStatement(this);
  }

  @Override
  public Iterable<Tree> children() {
    ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.builder();
    iteratorBuilder.add(forKeyword, openParenToken);
    iteratorBuilder.add(initializer);
    iteratorBuilder.add(firstSemicolonToken);
    if (condition != null) {
      iteratorBuilder.add(condition);
    }
    iteratorBuilder.add(secondSemicolonToken);
    iteratorBuilder.add(update);
    iteratorBuilder.add(closeParenToken, statement);

    return iteratorBuilder.build();
  }

}
