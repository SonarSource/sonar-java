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
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Trivia;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.squidbridge.measures.Metric;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

public class PublicApiVisitor extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribe(this);
  }

  public static void subscribe(SquidCheck<LexerlessGrammar> visitor) {
    visitor.subscribeTo(
      JavaGrammar.CLASS_DECLARATION,
      JavaGrammar.INTERFACE_DECLARATION,
      JavaGrammar.ENUM_DECLARATION,
      JavaGrammar.ANNOTATION_TYPE_DECLARATION,

      JavaGrammar.FIELD_DECLARATION,
      // TODO seems that it was missed in previous implementation: grammar.constantDeclaratorsRest

      // Same as in MethodVisitor
      JavaGrammar.CONSTRUCTOR_DECLARATOR_REST,
      JavaGrammar.METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_METHOD_DECLARATOR_REST,
      JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST,
      JavaGrammar.ANNOTATION_METHOD_REST);
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

  public static boolean isPublicApi(AstNode astNode) {
    return isPublic(astNode)
      && !isStaticFinalVariable(astNode)
      && !isMethodWithOverrideAnnotation(astNode)
      && !isEmptyDefaultConstructor(astNode);
  }

  public static String getType(AstNode node) {
    String type;

    if (node.is(JavaGrammar.CLASS_DECLARATION)) {
      type = "class";
    } else if (node.is(JavaGrammar.INTERFACE_DECLARATION)) {
      type = "interface";
    } else if (node.is(JavaGrammar.ENUM_DECLARATION)) {
      type = "enum";
    } else if (node.is(JavaGrammar.ANNOTATION_TYPE_DECLARATION)) {
      type = "annotation";
    } else if (node.is(JavaGrammar.FIELD_DECLARATION)) {
      type = "field";
    } else if (node.is(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST)) {
      type = "constructor";
    } else {
      type = "method";
    }

    return type;
  }

  private static boolean isEmptyDefaultConstructor(AstNode astNode) {
    if (astNode.is(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST)) {
      MethodHelper method = new MethodHelper(astNode);
      return !method.hasParameters() && method.getStatements().isEmpty();
    }
    return false;
  }

  private static boolean isMethodWithOverrideAnnotation(AstNode astNode) {
    if (isMethod(astNode)) {
      return hasAnnotation(astNode, "Override")
        || hasAnnotation(astNode, "java.lang.Override");
    }
    return false;
  }

  private static boolean hasAnnotation(AstNode astNode, String expected) {
    AstNode declaration = getDeclaration(astNode);
    for (AstNode modifier : getModifiers(declaration)) {
      AstNode annotation = modifier.getFirstChild(JavaGrammar.ANNOTATION);
      if (annotation != null) {
        StringBuilder value = new StringBuilder();
        for (AstNode identifier : annotation.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER).getChildren()) {
          value.append(identifier.getTokenValue());
        }
        if (value.toString().equals(expected)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isMethod(AstNode astNode) {
    return astNode.is(JavaGrammar.METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_METHOD_DECLARATOR_REST,
      JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST,
      JavaGrammar.ANNOTATION_METHOD_REST);
  }

  private static boolean isStaticFinalVariable(AstNode astNode) {
    AstNode declaration = getDeclaration(astNode);
    return astNode.is(JavaGrammar.FIELD_DECLARATION, JavaGrammar.CONSTANT_DECLARATORS_REST)
      && hasModifier(declaration, JavaKeyword.STATIC)
      && hasModifier(declaration, JavaKeyword.FINAL);
  }

  public static boolean isDocumentedApi(AstNode astNode) {
    return getApiJavadoc(astNode) != null;
  }

  public static String getApiJavadoc(AstNode astNode) {
    AstNode declaration = getDeclaration(astNode);
    for (Trivia trivia : declaration.getToken().getTrivia()) {
      if (trivia.isComment()) {
        String value = trivia.getToken().getOriginalValue();
        if (value.startsWith("/**")) {
          return value;
        }
      }
    }
    return null;
  }

  private static boolean isPublic(AstNode astNode) {
    AstNode declaration = getDeclaration(astNode);
    return declaration.is(JavaGrammar.ANNOTATION_TYPE_ELEMENT_DECLARATION)
      || declaration.is(JavaGrammar.INTERFACE_BODY_DECLARATION)
      || hasModifier(declaration, JavaKeyword.PUBLIC);
  }

  private static boolean hasModifier(AstNode declaration, AstNodeType astNodeType) {
    for (AstNode modifier : getModifiers(declaration)) {
      if (modifier.getFirstChild().is(astNodeType)) {
        return true;
      }
    }
    return false;
  }

  private static List<AstNode> getModifiers(AstNode declaration) {
    return declaration.getChildren(JavaGrammar.MODIFIER);
  }

  public static AstNode getDeclaration(AstNode astNode) {
    AstNode declaration;
    if (astNode.getParent().is(JavaGrammar.MEMBER_DECL)) {
      declaration = astNode.getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.CLASS_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.GENERIC_METHOD_OR_CONSTRUCTOR_REST)) {
      declaration = astNode.getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.CLASS_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.INTERFACE_MEMBER_DECL)) {
      declaration = astNode.getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.INTERFACE_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.INTERFACE_METHOD_OR_FIELD_REST)) {
      declaration = astNode.getParent().getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.INTERFACE_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.INTERFACE_GENERIC_METHOD_DECL)) {
      declaration = astNode.getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.INTERFACE_BODY_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.ANNOTATION_METHOD_OR_CONSTANT_REST)) {
      declaration = astNode.getParent().getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.ANNOTATION_TYPE_ELEMENT_DECLARATION));
    } else if (astNode.getParent().is(JavaGrammar.TYPE_DECLARATION)) {
      declaration = astNode.getParent();
    } else if (astNode.getParent().is(JavaGrammar.BLOCK_STATEMENT)) {
      declaration = astNode.getParent();
    } else if (astNode.getParent().is(JavaGrammar.ANNOTATION_TYPE_ELEMENT_REST)) {
      declaration = astNode.getParent().getParent();
      Preconditions.checkState(declaration.is(JavaGrammar.ANNOTATION_TYPE_ELEMENT_DECLARATION));
    } else {
      throw new IllegalStateException(astNode.getParent().getType().toString());
    }
    return declaration;
  }
}
