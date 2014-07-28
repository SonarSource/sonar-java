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
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;

public class SynchronizedStatementTreeImpl extends JavaTree implements SynchronizedStatementTree {
  private final ExpressionTree expression;
  private final BlockTree block;

  public SynchronizedStatementTreeImpl(ExpressionTree expression, BlockTree block, AstNode... children) {
    super(JavaGrammar.SYNCHRONIZED_STATEMENT);
    this.expression = Preconditions.checkNotNull(expression);
    this.block = Preconditions.checkNotNull(block);

    for (AstNode child : children) {
      addChild(child);
    }
  }

  @Override
  public Kind getKind() {
    return Kind.SYNCHRONIZED_STATEMENT;
  }

  @Override
  public SyntaxToken synchronizedKeyword() {
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaKeyword.SYNCHRONIZED).getToken());
  }

  @Override
  public SyntaxToken openParenToken() {
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaGrammar.PAR_EXPRESSION).getFirstChild(JavaPunctuator.LPAR).getToken());
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaGrammar.PAR_EXPRESSION).getFirstChild(JavaPunctuator.RPAR).getToken());
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
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(
      expression,
      block);
  }

}
