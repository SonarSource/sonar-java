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
package org.sonar.java.ast.visitors;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

public class MethodHelper {

  private final AstNode astNode;

  public MethodHelper(AstNode astNode) {
    this.astNode = astNode;
  }

  public boolean isConstructor() {
    return astNode.is(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST);
  }

  public AstNode getName() {
    final AstNode methodNameNode;

    if (astNode.is(Kind.METHOD)) {
      methodNameNode = (AstNode) ((MethodTree) astNode).simpleName();
    } else {
      if (astNode.is(JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST)) {
        methodNameNode = astNode.getPreviousAstNode();
      } else {
        methodNameNode = astNode.getPreviousSibling();
      }
      Preconditions.checkState(methodNameNode.is(JavaTokenType.IDENTIFIER));
    }

    return methodNameNode;
  }
}
