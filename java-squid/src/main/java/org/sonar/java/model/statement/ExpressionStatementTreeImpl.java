/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model.statement;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;

import java.util.Iterator;

public class ExpressionStatementTreeImpl extends JavaTree implements ExpressionStatementTree {

  private final ExpressionTree expression;
  private final InternalSyntaxToken semicolonToken;

  public ExpressionStatementTreeImpl(ExpressionTree expression, /* FIXME */@Nullable InternalSyntaxToken semicolonToken) {
    super(Kind.EXPRESSION_STATEMENT);

    this.expression = Preconditions.checkNotNull(expression);
    this.semicolonToken = semicolonToken;
  }

  @Override
  public Kind getKind() {
    return Kind.EXPRESSION_STATEMENT;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  // FIXME There isn't always a semicolon, for example within "for" initializers
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitExpressionStatement(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.<Tree>concat(
      Iterators.<Tree>singletonIterator(expression),
      semicolonToken != null ? Iterators.<Tree>singletonIterator(semicolonToken) : Iterators.<Tree>emptyIterator());
  }

}
