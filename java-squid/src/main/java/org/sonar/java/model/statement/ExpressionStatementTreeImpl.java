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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;

public class ExpressionStatementTreeImpl extends JavaTree implements ExpressionStatementTree {
  private final ExpressionTree expression;

  public ExpressionStatementTreeImpl(AstNode astNode, ExpressionTree expression) {
    super(astNode);
    this.expression = Preconditions.checkNotNull(expression);
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
  public SyntaxToken semicolonToken() {
    if (astNode.is(JavaGrammar.EXPRESSION_STATEMENT)) {
      return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.SEMI).getToken());
    } else if (astNode.is(JavaGrammar.STATEMENT_EXPRESSION)) {
      return new InternalSyntaxToken(astNode.getParent().getFirstChild(JavaPunctuator.SEMI).getToken());
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitExpressionStatement(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.<Tree>singletonIterator(
      expression
    );
  }
}
