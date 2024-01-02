/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

import javax.annotation.Nullable;
import java.util.Arrays;

public class YieldStatementTreeImpl extends JavaTree implements YieldStatementTree {

  @Nullable
  private final InternalSyntaxToken yieldKeyword;
  private final ExpressionTree expression;
  private final InternalSyntaxToken semicolonToken;

  public YieldStatementTreeImpl(
    @Nullable InternalSyntaxToken yieldKeyword,
    ExpressionTree expression,
    InternalSyntaxToken semicolonToken
  ) {
    this.yieldKeyword = yieldKeyword;
    this.expression = expression;
    this.semicolonToken = semicolonToken;
  }

  @Override
  public Kind kind() {
    return Kind.YIELD_STATEMENT;
  }

  @Nullable
  @Override
  public SyntaxToken yieldKeyword() {
    return yieldKeyword;
  }

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
    visitor.visitYieldStatement(this);
  }

  @Override
  protected List<Tree> children() {
    if (yieldKeyword != null) {
      return Arrays.asList(yieldKeyword, expression, semicolonToken);
    } else {
      return Arrays.asList(expression, semicolonToken);
    }
  }

}
