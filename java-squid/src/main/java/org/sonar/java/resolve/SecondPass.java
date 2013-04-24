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
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;

import java.util.List;

/**
 * Completes hierarchy of types.
 */
public class SecondPass implements Symbol.Completer {

  private final SemanticModel semanticModel;
  private final Resolve resolve;

  public SecondPass(SemanticModel semanticModel, Resolve resolve) {
    this.semanticModel = semanticModel;
    this.resolve = resolve;
  }

  @Override
  public void complete(Symbol symbol) {
    if (symbol.kind == Symbol.TYP) {
      complete((Symbol.TypeSymbol) symbol);
    } else if (symbol.kind == Symbol.MTH) {
      complete((Symbol.MethodSymbol) symbol);
    } else if (symbol.kind == Symbol.VAR) {
      complete((Symbol.VariableSymbol) symbol);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void complete(Symbol.TypeSymbol symbol) {
    if ("".equals(symbol.name)) {
      // Anonymous Class Declaration
      ((Type.ClassType) symbol.type).interfaces = ImmutableList.of();
      return;
    }

    AstNode astNode = semanticModel.getAstNode(symbol).getParent();
    Resolve.Env env = semanticModel.getEnv(symbol);

    AstNode superclassNode = astNode.getFirstChild(JavaGrammar.CLASS_TYPE);
    if (superclassNode != null) {
      ((Type.ClassType) symbol.type).supertype = resolveType(env, superclassNode).type;
    } else {
      // TODO superclass is java.lang.Object or java.lang.Enum
    }

    ImmutableList.Builder<Type> interfaces = ImmutableList.builder();
    if (astNode.hasDirectChildren(JavaGrammar.CLASS_TYPE_LIST)) {
      for (AstNode interfaceNode : astNode.getFirstChild(JavaGrammar.CLASS_TYPE_LIST).getChildren(JavaGrammar.CLASS_TYPE)) {
        Type interfaceType = castToTypeIfPossible(resolveType(env, interfaceNode));
        if (interfaceType != null) {
          interfaces.add(interfaceType);
        }
      }
    }
    // TODO interface of AnnotationType is java.lang.annotation.Annotation
    ((Type.ClassType) symbol.type).interfaces = interfaces.build();
  }

  public void complete(Symbol.MethodSymbol symbol) {
    AstNode identifierNode = semanticModel.getAstNode(symbol);
    Resolve.Env env = semanticModel.getEnv(symbol);

    AstNode throwsNode = identifierNode.getNextAstNode().getFirstChild(JavaKeyword.THROWS);
    ImmutableList.Builder<Symbol.TypeSymbol> thrown = ImmutableList.builder();
    if (throwsNode != null) {
      for (AstNode qualifiedIdentifier : throwsNode.getNextAstNode().getChildren(JavaGrammar.QUALIFIED_IDENTIFIER)) {
        Type thrownType = castToTypeIfPossible(resolveType(env, qualifiedIdentifier));
        if (thrownType != null) {
          thrown.add(((Type.ClassType) thrownType).symbol);
        }
      }
    }
    symbol.thrown = thrown.build();

    if ("<init>".equals(symbol.name)) {
      // no return type for constructor
      return;
    }
    AstNode typeNode = identifierNode.getPreviousAstNode();
    Preconditions.checkState(typeNode.is(JavaKeyword.VOID, JavaGrammar.TYPE));
    AstNode classTypeNode = typeNode.getFirstChild(JavaGrammar.CLASS_TYPE);
    if (classTypeNode == null) {
      // TODO JavaGrammar.BASIC_TYPE
      return;
    }
    symbol.type = ((Type.ClassType) castToTypeIfPossible(resolveType(env, classTypeNode))).symbol;
  }

  public void complete(Symbol.VariableSymbol symbol) {
    AstNode identifierNode = semanticModel.getAstNode(symbol);
    AstNode typeNode;
    if (identifierNode.getParent().is(JavaGrammar.VARIABLE_DECLARATOR)) {
      typeNode = identifierNode.getFirstAncestor(JavaGrammar.VARIABLE_DECLARATORS).getPreviousAstNode();
      Preconditions.checkState(typeNode.is(JavaGrammar.TYPE));
    } else if (identifierNode.getParent().is(JavaGrammar.VARIABLE_DECLARATOR_ID)) {
      typeNode = identifierNode.getParent().getPreviousAstNode();
      if (typeNode.is(JavaPunctuator.ELLIPSIS)) {
        // vararg
        typeNode = typeNode.getPreviousAstNode();
      }
      Preconditions.checkState(typeNode.is(JavaGrammar.TYPE, JavaGrammar.CLASS_TYPE, JavaGrammar.CATCH_TYPE));
    } else if (identifierNode.getParent().is(JavaGrammar.ENUM_CONSTANT)) {
      // Type of enum constant is enum
      semanticModel.getEnv(symbol).enclosingClass();
      return;
    } else if (identifierNode.getParent().is(JavaGrammar.CONSTANT_DECLARATOR)) {
      typeNode = identifierNode.getFirstAncestor(JavaGrammar.CONSTANT_DECLARATORS_REST).getPreviousAstNode().getPreviousAstNode();
      Preconditions.checkState(typeNode.is(JavaGrammar.TYPE));
    } else if (identifierNode.getParent().is(JavaGrammar.INTERFACE_METHOD_OR_FIELD_DECL, JavaGrammar.ANNOTATION_TYPE_ELEMENT_REST)) {
      typeNode = identifierNode.getPreviousAstNode();
      Preconditions.checkState(typeNode.is(JavaGrammar.TYPE));
    } else {
      throw new IllegalStateException();
    }
    resolveVariableType(symbol, typeNode);
  }

  private void resolveVariableType(Symbol.VariableSymbol symbol, AstNode typeNode) {
    if (typeNode.is(JavaGrammar.TYPE)) {
      typeNode = typeNode.getFirstChild(JavaGrammar.CLASS_TYPE);
      if (typeNode == null) {
        // TODO
        return;
      }
    } else if (typeNode.is(JavaGrammar.CLASS_TYPE)) {
      // nop
    } else if (typeNode.is(JavaGrammar.CATCH_TYPE)) {
      // TODO
      return;
    } else {
      throw new IllegalArgumentException();
    }

    Resolve.Env env = semanticModel.getEnv(symbol);
    symbol.type = castToTypeIfPossible(resolveType(env, typeNode));
  }

  private Symbol resolveType(Resolve.Env env, AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaGrammar.CLASS_TYPE, JavaGrammar.QUALIFIED_IDENTIFIER));

    env = env.dup();
    List<AstNode> identifiers = astNode.getChildren(JavaTokenType.IDENTIFIER);
    Symbol site = resolve.findIdent(env, identifiers.get(0).getTokenValue(), Symbol.TYP | Symbol.PCK);
    associateReference(identifiers.get(0), site);
    for (AstNode identifierNode : identifiers.subList(1, identifiers.size())) {
      if (site.kind >= Symbol.ERRONEOUS) {
        return site;
      }
      String name = identifierNode.getTokenValue();
      if (site.kind == Symbol.PCK) {
        env.packge = (Symbol.PackageSymbol) site;
        site = resolve.findIdentInPackage(env, site, name, Symbol.TYP | Symbol.PCK);
      } else {
        env.enclosingClass = (Symbol.TypeSymbol) site;
        site = resolve.findMemberType(env, (Symbol.TypeSymbol) site, name, (Symbol.TypeSymbol) site);
      }
      associateReference(identifierNode, site);
    }
    return site;
  }

  private Type castToTypeIfPossible(Symbol symbol) {
    return symbol instanceof Symbol.TypeSymbol ? ((Symbol.TypeSymbol) symbol).type : null;
  }

  private void associateReference(AstNode astNode, Symbol symbol) {
    if (symbol.kind < Symbol.ERRONEOUS) {
      semanticModel.associateReference(astNode, symbol);
    }
  }

}
