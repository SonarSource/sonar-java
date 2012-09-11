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
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Trivia;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.measures.Metric;

import java.util.List;

public class PublicApiVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    subscribe(this);
  }

  public static void subscribe(JavaAstVisitor visitor) {
    JavaGrammar grammar = visitor.getContext().getGrammar();
    visitor.subscribeTo(
        grammar.classDeclaration,
        grammar.interfaceDeclaration,
        grammar.enumDeclaration,
        grammar.annotationTypeDeclaration,

        grammar.fieldDeclaration,
        // TODO seems that it was missed in previous implementation: grammar.constantDeclaratorsRest

        // Same as in MethodVisitor
        grammar.constructorDeclaratorRest,
        grammar.methodDeclaratorRest,
        grammar.voidMethodDeclaratorRest,
        grammar.interfaceMethodDeclaratorRest,
        grammar.voidInterfaceMethodDeclaratorsRest,
        grammar.annotationMethodRest);
  }

  @Override
  public void visitNode(AstNode astNode) {
    SourceCode currentResource = getContext().peekSourceCode();
    if (isPublicApi(astNode)) {
      currentResource.add(Metric.PUBLIC_API, 1);
      if (isDocumentedApi(astNode)) {
        currentResource.add(Metric.PUBLIC_DOC_API, 1);
      }
    }
  }

  private boolean isPublicApi(AstNode astNode) {
    return isPublic(astNode)
        && !isStaticFinalVariable(astNode)
        && !isMethodWithOverrideAnnotation(astNode)
        && !isEmptyDefaultConstructor(astNode);
  }

  private boolean isEmptyDefaultConstructor(AstNode astNode) {
    if (astNode.is(getContext().getGrammar().constructorDeclaratorRest)) {
      MethodHelper method = new MethodHelper(getContext().getGrammar(), astNode);
      return !method.hasParameters() && method.getStatements().isEmpty();
    }
    return false;
  }

  private boolean isMethodWithOverrideAnnotation(AstNode astNode) {
    if (isMethod(astNode)) {
      return hasAnnotation(astNode, "Override")
          || hasAnnotation(astNode, "java.lang.Override");
    }
    return false;
  }

  private boolean hasAnnotation(AstNode astNode, String expected) {
    AstNode declaration = getDeclaration(astNode);
    for (AstNode modifier : getModifiers(declaration)) {
      AstNode annotation = modifier.findFirstDirectChild(getContext().getGrammar().annotation);
      if (annotation != null) {
        StringBuilder value = new StringBuilder();
        for (AstNode identifier : annotation.findFirstDirectChild(getContext().getGrammar().qualifiedIdentifier).getChildren()) {
          value.append(identifier.getTokenValue());
        }
        if (value.toString().equals(expected)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isMethod(AstNode astNode) {
    JavaGrammar grammar = getContext().getGrammar();
    return astNode.is(grammar.methodDeclaratorRest,
        grammar.voidMethodDeclaratorRest,
        grammar.interfaceMethodDeclaratorRest,
        grammar.voidInterfaceMethodDeclaratorsRest,
        grammar.annotationMethodRest);
  }

  private boolean isStaticFinalVariable(AstNode astNode) {
    AstNode declaration = getDeclaration(astNode);
    return astNode.is(getContext().getGrammar().fieldDeclaration, getContext().getGrammar().constantDeclaratorsRest)
        && hasModifier(declaration, JavaKeyword.STATIC)
        && hasModifier(declaration, JavaKeyword.FINAL);
  }

  private boolean isDocumentedApi(AstNode astNode) {
    // TODO verify
    AstNode declaration = getDeclaration(astNode);
    for (Trivia trivia : declaration.getToken().getTrivia()) {
      if (trivia.isComment()) {
        String value = trivia.getToken().getOriginalValue();
        if (value.startsWith("/**")) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isPublic(AstNode astNode) {
    AstNode declaration = getDeclaration(astNode);
    return declaration.is(getContext().getGrammar().annotationTypeElementDeclaration)
        || declaration.is(getContext().getGrammar().interfaceBodyDeclaration)
        || hasModifier(declaration, JavaKeyword.PUBLIC);
  }

  private boolean hasModifier(AstNode declaration, AstNodeType astNodeType) {
    for (AstNode modifier : getModifiers(declaration)) {
      if (modifier.getFirstChild().is(astNodeType)) {
        return true;
      }
    }
    return false;
  }

  private List<AstNode> getModifiers(AstNode declaration) {
    return declaration.findDirectChildren(getContext().getGrammar().modifier);
  }

  private AstNode getDeclaration(AstNode astNode) {
    return getDeclaration(getContext().getGrammar(), astNode);
  }

  public static AstNode getDeclaration(JavaGrammar grammar, AstNode astNode) {
    AstNode declaration;
    if (astNode.getParent().is(grammar.memberDecl)) {
      declaration = astNode.getParent().getParent();
      Preconditions.checkState(declaration.is(grammar.classBodyDeclaration));
    } else if (astNode.getParent().is(grammar.genericMethodOrConstructorRest)) {
      declaration = astNode.getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(grammar.classBodyDeclaration));
    } else if (astNode.getParent().is(grammar.interfaceMemberDecl)) {
      declaration = astNode.getParent().getParent();
      Preconditions.checkState(declaration.is(grammar.interfaceBodyDeclaration));
    } else if (astNode.getParent().is(grammar.interfaceMethodOrFieldRest)) {
      declaration = astNode.getParent().getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(grammar.interfaceBodyDeclaration));
    } else if (astNode.getParent().is(grammar.interfaceGenericMethodDecl)) {
      declaration = astNode.getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(grammar.interfaceBodyDeclaration));
    } else if (astNode.getParent().is(grammar.annotationMethodOrConstantRest)) {
      declaration = astNode.getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(grammar.annotationTypeElementDeclaration));
    } else if (astNode.getParent().is(grammar.typeDeclaration)) {
      declaration = astNode.getParent();
    } else if (astNode.getParent().is(grammar.blockStatement)) {
      declaration = astNode.getParent();
    } else if (astNode.getParent().is(grammar.annotationTypeElementRest)) {
      declaration = astNode.getParent().getParent();
      Preconditions.checkState(declaration.is(grammar.annotationTypeElementDeclaration));
    } else {
      throw new IllegalStateException(astNode.getParent().getType().toString());
    }
    return declaration;
  }
}
