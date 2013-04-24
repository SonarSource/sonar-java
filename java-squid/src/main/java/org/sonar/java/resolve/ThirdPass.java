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
package org.sonar.java.resolve;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.visitors.JavaAstVisitor;

import java.util.List;

public class ThirdPass extends JavaAstVisitor {

  private final SemanticModel semanticModel;
  private final Resolve resolve;

  public ThirdPass(SemanticModel semanticModel, Resolve resolve) {
    this.semanticModel = semanticModel;
    this.resolve = resolve;
  }

  @Override
  public void init() {
    subscribeTo(JavaGrammar.PRIMARY);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (astNode.is(JavaGrammar.PRIMARY)) {
      visitPrimary(astNode);
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode.getType());
    }
  }

  private void visitPrimary(AstNode astNode) {
    AstNode qualifiedIdentifierNode = astNode.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
    if (qualifiedIdentifierNode != null) {
      AstNode identifierSuffixNode = astNode.getFirstChild(JavaGrammar.IDENTIFIER_SUFFIX);
      if (identifierSuffixNode != null) {
        if (identifierSuffixNode.hasDirectChildren(JavaGrammar.ARGUMENTS)) {
          resolveMethodCall(qualifiedIdentifierNode);
        }
      } else {
        resolve(qualifiedIdentifierNode);
      }
    }
  }

  private void resolveMethodCall(AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.QUALIFIED_IDENTIFIER));
    if (astNode.getNumberOfChildren() > 1) {
      // not a simple method call
      return;
    }
    Resolve.Env env = semanticModel.getEnv(astNode).dup();
    AstNode identifierNode = astNode.getFirstChild();
    Symbol symbol = resolve.findMethod(env, env.enclosingClass(), identifierNode.getTokenValue());
    associateReference(identifierNode, symbol);
  }

  private Symbol resolve(AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.QUALIFIED_IDENTIFIER));

    Resolve.Env env = semanticModel.getEnv(astNode).dup();
    List<AstNode> identifiers = astNode.getChildren(JavaTokenType.IDENTIFIER);

    Symbol site = resolve.findIdent(env, identifiers.get(0).getTokenValue(), Symbol.VAR | Symbol.TYP | Symbol.PCK);
    associateReference(identifiers.get(0), site);
    for (AstNode identifierNode : identifiers.subList(1, identifiers.size())) {
      if (site.kind >= Symbol.ERRONEOUS) {
        return site;
      }
      String name = identifierNode.getTokenValue();
      if (site.kind == Symbol.VAR) {
        Type type = ((Symbol.VariableSymbol) site).type;
        // TODO avoid null
        if (type == null) {
          return null;
        }
        site = resolve.findIdentInType(env, type.symbol, name, Symbol.VAR | Symbol.TYP);
      } else if (site.kind == Symbol.TYP) {
        site = resolve.findIdentInType(env, (Symbol.TypeSymbol) site, name, Symbol.VAR | Symbol.TYP);
      } else if (site.kind == Symbol.PCK) {
        site = resolve.findIdentInPackage(env, site, name, Symbol.VAR | Symbol.PCK);
      } else {
        throw new IllegalStateException();
      }
      associateReference(identifierNode, site);
    }
    return site;
  }

  private void associateReference(AstNode astNode, Symbol symbol) {
    if (symbol.kind < Symbol.ERRONEOUS) {
      // symbol exists
      semanticModel.associateReference(astNode, symbol);
    }
  }

}
