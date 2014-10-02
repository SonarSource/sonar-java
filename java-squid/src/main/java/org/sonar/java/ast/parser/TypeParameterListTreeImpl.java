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
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.plugins.java.api.tree.SyntaxToken;

import java.util.List;

public class TypeParameterListTreeImpl extends ListTreeImpl<TypeParameterTreeImpl> {

  private final InternalSyntaxToken openBracketToken;
  private final InternalSyntaxToken closeBracketToken;

  public TypeParameterListTreeImpl(InternalSyntaxToken openBracketToken, List<TypeParameterTreeImpl> typeParameters, List<AstNode> children, InternalSyntaxToken closeBracketToken) {
    super(JavaLexer.TYPE_PARAMETERS, typeParameters, ImmutableList.<AstNode>of());

    this.openBracketToken = openBracketToken;
    this.closeBracketToken = closeBracketToken;

    addChild(openBracketToken);
    for (AstNode child : children) {
      addChild(child);
    }
    addChild(closeBracketToken);
  }

  public SyntaxToken openBracketToken() {
    return openBracketToken;
  }

  public SyntaxToken closeBracketToken() {
    return closeBracketToken;
  }

}
