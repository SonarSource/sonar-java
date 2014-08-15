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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;

import java.util.Iterator;

public class IfStatementTreeImpl extends JavaTree implements IfStatementTree {

  private ExpressionTree condition;
  private StatementTree thenStatement;

  @Nullable
  private final StatementTree elseStatement;

  public IfStatementTreeImpl(ExpressionTree condition, StatementTree thenStatement, AstNode... children) {
    super(JavaGrammar.IF_STATEMENT);
    this.condition = Preconditions.checkNotNull(condition);
    this.thenStatement = Preconditions.checkNotNull(thenStatement);
    this.elseStatement = null;

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public IfStatementTreeImpl(StatementTree elseStatement, AstNode... children) {
    super(JavaGrammar.IF_STATEMENT);
    this.elseStatement = Preconditions.checkNotNull(elseStatement);

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public IfStatementTreeImpl complete(ExpressionTree condition, StatementTree thenStatement, AstNode... children) {
    Preconditions.checkState(this.condition == null, "Already completed");

    this.condition = Preconditions.checkNotNull(condition);
    this.thenStatement = Preconditions.checkNotNull(thenStatement);

    prependChildren(children);

    return this;
  }

  @Override
  public Kind getKind() {
    return Kind.IF_STATEMENT;
  }

  @Override
  public SyntaxToken ifKeyword() {
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaKeyword.IF));
  }

  @Override
  public SyntaxToken openParenToken() {
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaGrammar.PAR_EXPRESSION).getFirstChild(JavaPunctuator.LPAR));
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaGrammar.PAR_EXPRESSION).getFirstChild(JavaPunctuator.RPAR));
  }

  @Override
  public StatementTree thenStatement() {
    return thenStatement;
  }

  @Nullable
  @Override
  public SyntaxToken elseKeyword() {
    return elseStatement == null ? null : new InternalSyntaxToken(getAstNode().getFirstChild(JavaKeyword.ELSE));
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
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(
      condition,
      thenStatement,
      elseStatement);
  }

}
