/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
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
  private ExpressionTree condition;

  @Nullable
  private final ExpressionTree detail;

  private AstNode colonToken;
  private AstNode detailAstNode;

  public AssertStatementTreeImpl(ExpressionTree condition, AstNode assertToken, AstNode expression, AstNode semicolonToken) {
    super(JavaGrammar.ASSERT_STATEMENT);
    this.condition = Preconditions.checkNotNull(condition);
    this.detail = null;

    addChild(assertToken);
    addChild(expression);
    addChild(semicolonToken);
  }

  public AssertStatementTreeImpl(ExpressionTree detail, AstNode colonToken, AstNode expression) {
    super(JavaGrammar.ASSERT_STATEMENT);
    this.detail = Preconditions.checkNotNull(detail);

    this.colonToken = colonToken;
    this.detailAstNode = expression;
  }

  public AssertStatementTreeImpl complete(ExpressionTree condition, AstNode assertToken, AstNode expression, AstNode semicolonToken) {
    this.condition = Preconditions.checkNotNull(condition);

    addChild(assertToken);
    addChild(expression);
    addChild(colonToken);
    addChild(detailAstNode);
    addChild(semicolonToken);

    return this;
  }

  @Override
  public Kind getKind() {
    return Kind.ASSERT_STATEMENT;
  }

  @Override
  public SyntaxToken assertKeyword() {
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaKeyword.ASSERT).getToken());
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Nullable
  @Override
  public SyntaxToken colonToken() {
    return detail == null ? null : new InternalSyntaxToken(getAstNode().getFirstChild(JavaPunctuator.COLON).getToken());
  }

  @Nullable
  @Override
  public ExpressionTree detail() {
    return detail;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaPunctuator.SEMI).getToken());
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAssertStatement(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.<Tree>forArray(
      condition,
      detail);
  }

}
