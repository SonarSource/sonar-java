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
import org.sonar.java.model.declaration.VariableTreeImpl;

import javax.annotation.Nullable;

import java.util.List;

public class LambdaParameterListTreeImpl extends ListTreeImpl<VariableTreeImpl> {

  private InternalSyntaxToken openParenToken;
  private InternalSyntaxToken closeParenToken;

  public LambdaParameterListTreeImpl(@Nullable InternalSyntaxToken openParenToken, List<VariableTreeImpl> params,
    @Nullable InternalSyntaxToken closeParenToken, List<AstNode> children) {
    super(JavaGrammar.LAMBDA_PARAMETERS, params, ImmutableList.<AstNode>of());

    if (openParenToken != null) {
      this.openParenToken = openParenToken;
    }
    for (AstNode child : children) {
      addChild(child);
    }
    if (closeParenToken != null) {
      this.closeParenToken = closeParenToken;
    }
  }

  @Nullable
  public InternalSyntaxToken openParenToken() {
    return openParenToken;
  }

  @Nullable
  public InternalSyntaxToken closeParenToken() {
    return closeParenToken;
  }

}
