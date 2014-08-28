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
package org.sonar.java.ast.parser;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;

import java.util.List;

public class ArgumentListTreeImpl extends ListTreeImpl<ExpressionTree> {

  private InternalSyntaxToken openParenToken;
  private InternalSyntaxToken closeParenToken;

  public ArgumentListTreeImpl(InternalSyntaxToken openParenToken, InternalSyntaxToken closeParenToken) {
    super(JavaGrammar.ARGUMENTS, ImmutableList.<ExpressionTree>of(), ImmutableList.<AstNode>of());

    this.openParenToken = openParenToken;
    this.closeParenToken = closeParenToken;

    addChild(openParenToken);
    addChild(closeParenToken);
  }

  public ArgumentListTreeImpl(InternalSyntaxToken openParenToken, ExpressionTree expression, InternalSyntaxToken closeParenToken) {
    super(JavaGrammar.ARGUMENTS, ImmutableList.of(expression), ImmutableList.<AstNode>of());

    this.openParenToken = openParenToken;
    this.closeParenToken = closeParenToken;

    addChild(openParenToken);
    addChild((AstNode) expression);
    addChild(closeParenToken);
  }

  public ArgumentListTreeImpl(List<ExpressionTree> expressions, List<AstNode> children) {
    super(JavaGrammar.ARGUMENTS, expressions, children);
  }

  public ArgumentListTreeImpl complete(InternalSyntaxToken openParenToken, InternalSyntaxToken closeParenToken) {
    this.openParenToken = openParenToken;
    this.closeParenToken = closeParenToken;

    prependChildren(openParenToken);
    addChild(closeParenToken);

    return this;
  }

  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

}
