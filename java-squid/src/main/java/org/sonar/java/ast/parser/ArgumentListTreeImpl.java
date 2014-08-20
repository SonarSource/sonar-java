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

  private InternalSyntaxToken openParenthesisToken;
  private InternalSyntaxToken closeParenthesisToken;

  public ArgumentListTreeImpl(InternalSyntaxToken openParenthesisToken, InternalSyntaxToken closeParenthesisToken) {
    super(JavaGrammar.ARGUMENTS, ImmutableList.<ExpressionTree>of(), ImmutableList.<AstNode>of());

    this.openParenthesisToken = openParenthesisToken;
    this.closeParenthesisToken = closeParenthesisToken;

    addChild(openParenthesisToken);
    addChild(closeParenthesisToken);
  }

  public ArgumentListTreeImpl(List<ExpressionTree> expressions, List<AstNode> children) {
    super(JavaGrammar.ARGUMENTS, expressions, children);
  }

  public ArgumentListTreeImpl complete(InternalSyntaxToken openParenthesisToken, InternalSyntaxToken closeParenthesisToken) {
    this.openParenthesisToken = openParenthesisToken;
    this.closeParenthesisToken = closeParenthesisToken;

    prependChildren(openParenthesisToken);
    addChild(closeParenthesisToken);

    return this;
  }

  public SyntaxToken openParenthesisToken() {
    return openParenthesisToken;
  }

  public SyntaxToken closeParenthesisToken() {
    return closeParenthesisToken;
  }

}
