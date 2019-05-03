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

import com.google.common.collect.Lists;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Objects;

public class ForEachStatementImpl extends JavaTree implements ForEachStatement {
  private final InternalSyntaxToken forKeyword;
  private final InternalSyntaxToken openParenToken;
  private final VariableTree variable;
  private final InternalSyntaxToken colonToken;
  private final ExpressionTree expression;
  private final InternalSyntaxToken closeParenToken;
  private final StatementTree statement;

  public ForEachStatementImpl(InternalSyntaxToken forKeyword, InternalSyntaxToken openParenToken, VariableTreeImpl variable, InternalSyntaxToken colonToken,
    ExpressionTree expression, InternalSyntaxToken closeParenToken, StatementTree statement) {
    super(Kind.FOR_EACH_STATEMENT);
    this.forKeyword = forKeyword;
    this.openParenToken = openParenToken;
    this.variable = Objects.requireNonNull(variable);
    this.colonToken = colonToken;
    this.expression = Objects.requireNonNull(expression);
    this.closeParenToken = closeParenToken;
    this.statement = Objects.requireNonNull(statement);
  }

  @Override
  public Kind kind() {
    return Kind.FOR_EACH_STATEMENT;
  }

  @Override
  public SyntaxToken forKeyword() {
    return forKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public VariableTree variable() {
    return variable;
  }

  @Override
  public SyntaxToken colonToken() {
    return colonToken;
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
  public StatementTree statement() {
    return statement;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitForEachStatement(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Lists.newArrayList(
      forKeyword,
      openParenToken,
      variable,
      colonToken,
      expression,
      closeParenToken,
      statement);
  }

}
