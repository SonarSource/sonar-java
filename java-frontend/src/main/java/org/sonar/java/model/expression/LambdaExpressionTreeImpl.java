/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class LambdaExpressionTreeImpl extends AbstractTypedTree implements LambdaExpressionTree {

  @Nullable
  private final InternalSyntaxToken openParenToken;
  private final List<VariableTree> parameters;
  @Nullable
  private final InternalSyntaxToken closeParenToken;
  private final InternalSyntaxToken arrowToken;
  private final Tree body;

  public LambdaExpressionTreeImpl(@Nullable InternalSyntaxToken openParenToken, List<VariableTree> parameters, @Nullable InternalSyntaxToken closeParenToken,
    InternalSyntaxToken arrowToken, Tree body) {
    super(JavaLexer.LAMBDA_EXPRESSION);
    this.openParenToken = openParenToken;
    this.parameters = parameters;
    this.closeParenToken = closeParenToken;
    this.arrowToken = arrowToken;
    this.body = body;
  }

  @Override
  public Kind kind() {
    return Kind.LAMBDA_EXPRESSION;
  }

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public List<VariableTree> parameters() {
    return parameters;
  }

  @Nullable
  @Override
  public SyntaxToken closeParenToken() {
    return  closeParenToken;
  }

  @Override
  public SyntaxToken arrowToken() {
    return arrowToken;
  }

  @Override
  public Tree body() {
    return body;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitLambdaExpression(this);
  }

  @Override
  public Iterable<Tree> children() {
    boolean hasParentheses = openParenToken != null;
    List<Tree> res = new ArrayList<>();
    if(hasParentheses) {
      res.add(openParenToken);
    }
    res.addAll(parameters);
    if(hasParentheses) {
      res.add(closeParenToken);
    }
    res.add(arrowToken);
    res.add(body);
    return res;
  }

}
