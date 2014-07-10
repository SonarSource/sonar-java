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
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Iterator;

public class ForEachStatementImpl extends JavaTree implements ForEachStatement {
  private final VariableTree variable;
  private final ExpressionTree expression;
  private final StatementTree statement;

  public ForEachStatementImpl(AstNode astNode, VariableTree variable, ExpressionTree expression, StatementTree statement) {
    super(astNode);
    this.variable = Preconditions.checkNotNull(variable);
    this.expression = Preconditions.checkNotNull(expression);
    this.statement = Preconditions.checkNotNull(statement);
  }

  @Override
  public Kind getKind() {
    return Kind.FOR_EACH_STATEMENT;
  }

  @Override
  public SyntaxToken forKeyword() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaKeyword.FOR).getToken());
  }

  @Override
  public SyntaxToken openParenToken() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.LPAR).getToken());
  }

  @Override
  public VariableTree variable() {
    return variable;
  }

  @Override
  public SyntaxToken colonToken() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.COLON).getToken());
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.RPAR).getToken());
  }

  @Override
  public StatementTree statement() {
    return statement;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitForEachStatement(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(
      variable,
      expression,
      statement
    );
  }
}
