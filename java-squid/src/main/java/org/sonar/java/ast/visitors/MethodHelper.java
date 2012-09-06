/*
 * Sonar Java
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
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.squid.SquidAstVisitor;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.api.JavaKeyword;

import java.util.Collections;
import java.util.List;

public class MethodHelper {

  private final JavaGrammar grammar;
  private final AstNode astNode;

  public MethodHelper(JavaGrammar grammar, AstNode astNode) {
    this.grammar = grammar;
    this.astNode = astNode;
  }

  public static void subscribe(SquidAstVisitor<JavaGrammar> visitor) {
    JavaGrammar grammar = visitor.getContext().getGrammar();
    visitor.subscribeTo(
        grammar.methodDeclaratorRest,
        grammar.voidMethodDeclaratorRest,
        grammar.constructorDeclaratorRest,
        grammar.interfaceMethodDeclaratorRest,
        grammar.voidInterfaceMethodDeclaratorsRest,
        grammar.annotationMethodRest);
  }

  public boolean isPublic() {
    final AstNode node;
    if (astNode.is(grammar.methodDeclaratorRest, grammar.voidMethodDeclaratorRest, grammar.constructorDeclaratorRest)) {
      node = astNode.findFirstParent(grammar.classBodyDeclaration);
    } else if (astNode.is(grammar.interfaceMethodDeclaratorRest, grammar.voidInterfaceMethodDeclaratorsRest)) {
      node = astNode.findFirstParent(grammar.interfaceBodyDeclaration);
    } else if (astNode.is(grammar.annotationMethodRest)) {
      node = astNode.findFirstParent(grammar.annotationTypeElementDeclaration);
    } else {
      throw new IllegalStateException();
    }
    for (AstNode modifierNode : node.findDirectChildren(grammar.modifier)) {
      if (modifierNode.getChild(0).is(JavaKeyword.PUBLIC)) {
        return true;
      }
    }
    return false;
  }

  public AstNode getReturnType() {
    final AstNode typeNode = getName().previousSibling();
    Preconditions.checkState(typeNode.is(JavaKeyword.VOID, grammar.type));
    return typeNode;
  }

  public AstNode getName() {
    final AstNode methodNameNode;
    if (astNode.is(grammar.interfaceMethodDeclaratorRest)) {
      methodNameNode = astNode.getParent().previousSibling();
    } else if (astNode.is(grammar.annotationMethodRest)) {
      methodNameNode = astNode.getChild(0);
    } else {
      methodNameNode = astNode.previousSibling();
    }
    Preconditions.checkState(methodNameNode.is(GenericTokenType.IDENTIFIER));
    return methodNameNode;
  }

  public List<AstNode> getParameters() {
    AstNode node = astNode.findFirstDirectChild(grammar.formalParameters);
    return node.findChildren(grammar.formalParameterDecls);
  }

  public boolean hasParameters() {
    return getParameters().size() > 0;
  }

  public List<AstNode> getStatements() {
    AstNode node = astNode.findFirstDirectChild(grammar.methodBody);
    if (node == null) {
      return Collections.emptyList();
    }
    return node.findFirstChild(grammar.blockStatements).getChildren();
  }

}
