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
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;

import java.util.Iterator;

public class AssertStatementTreeImpl extends JavaTree implements AssertStatementTree {

  private InternalSyntaxToken assertToken;
  private ExpressionTree condition;
  @Nullable
  private final InternalSyntaxToken colonToken;
  @Nullable
  private final ExpressionTree detail;
  private InternalSyntaxToken semicolonToken;

  public AssertStatementTreeImpl(InternalSyntaxToken assertToken, ExpressionTree condition, InternalSyntaxToken semicolonToken) {
    super(Kind.ASSERT_STATEMENT);
    this.assertToken = assertToken;
    this.condition = Preconditions.checkNotNull(condition);
    this.colonToken = null;
    this.detail = null;
    this.semicolonToken = semicolonToken;
  }

  public AssertStatementTreeImpl(InternalSyntaxToken colonToken, ExpressionTree detail) {
    super(Kind.ASSERT_STATEMENT);
    this.colonToken = colonToken;
    this.detail = Preconditions.checkNotNull(detail);
  }

  public AssertStatementTreeImpl complete(InternalSyntaxToken assertToken, ExpressionTree condition, InternalSyntaxToken semicolonToken) {
    this.assertToken = assertToken;
    this.condition = Preconditions.checkNotNull(condition);
    this.semicolonToken = semicolonToken;

    return this;
  }

  @Override
  public Kind getKind() {
    return Kind.ASSERT_STATEMENT;
  }

  @Override
  public SyntaxToken assertKeyword() {
    return assertToken;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Nullable
  @Override
  public SyntaxToken colonToken() {
    return colonToken;
  }

  @Nullable
  @Override
  public ExpressionTree detail() {
    return detail;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAssertStatement(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    Iterator<Tree> detailIterator = colonToken != null ? Iterators.<Tree>forArray(colonToken, detail) : Iterators.<Tree>emptyIterator();
    return Iterators.<Tree>concat(
      Iterators.<Tree>forArray(assertToken, condition),
      detailIterator,
      Iterators.<Tree>singletonIterator(semicolonToken));
  }

}
