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
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;

public class ReturnStatementTreeImpl extends JavaTree implements ReturnStatementTree {
  private final InternalSyntaxToken returnKeyword;
  @Nullable
  private final ExpressionTree expression;
  private final InternalSyntaxToken semicolonToken;

  public ReturnStatementTreeImpl(InternalSyntaxToken returnKeyword, @Nullable ExpressionTree expression, InternalSyntaxToken semicolonToken) {
    super(Kind.RETURN_STATEMENT);
    this.returnKeyword = returnKeyword;
    this.expression = expression;
    this.semicolonToken = semicolonToken;
  }

  @Override
  public Kind kind() {
    return Kind.RETURN_STATEMENT;
  }

  @Override
  public SyntaxToken returnKeyword() {
    return returnKeyword;
  }

  @Nullable
  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitReturnStatement(this);
  }

  @Override
  public Iterable<Tree> children() {
    ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.<Tree>builder().add(returnKeyword);
    if (expression != null) {
      iteratorBuilder.add(expression);
    }
    iteratorBuilder.add(semicolonToken);
    return iteratorBuilder.build();
  }

}
