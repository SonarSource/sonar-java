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
package org.sonar.java.model.expression;

import com.google.common.collect.Lists;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Objects;

public class ParenthesizedTreeImpl extends AbstractTypedTree implements ParenthesizedTree {
  private final InternalSyntaxToken openParenToken;
  private final ExpressionTree expression;
  private final InternalSyntaxToken closeParenToken;

  public ParenthesizedTreeImpl(InternalSyntaxToken openParenToken, ExpressionTree expression, InternalSyntaxToken closeParenToken) {
    super(Kind.PARENTHESIZED_EXPRESSION);
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
  public Iterable<Tree> children() {
    return Lists.newArrayList(
      openParenToken,
      expression,
      closeParenToken);
  }

}
