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
import com.sonar.sslr.api.Token;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;


public class PublicApiChecker {

  public static final Tree.Kind[] CLASS_KINDS = {
      Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.ANNOTATION_TYPE
  };

  public static final Tree.Kind[] METHOD_KINDS = {
      Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR
  };

  public static final Tree.Kind[] API_KINDS = {
      Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.ANNOTATION_TYPE,
      Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR,
      Tree.Kind.VARIABLE
  };


  public boolean isPublicApi(ClassTree currentClass, ClassTree classTree) {
    return (currentClass != null && currentClass.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)) || hasPublic(classTree.modifiers());
  }

  public boolean isPublicApi(ClassTree classTree, MethodTree methodTree) {
    Preconditions.checkNotNull(classTree);
    if (classTree.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)) {
      return !hasOverrideAnnotation(methodTree);
    } else if (isEmptyDefaultConstructor(methodTree) || hasOverrideAnnotation(methodTree)) {
      return false;
    }
    return hasPublic(methodTree.modifiers());
  }

  public boolean isPublicApi(ClassTree classTree, VariableTree variableTree) {
    return !classTree.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE) && !isStaticFinal(variableTree) && hasPublic(variableTree.modifiers());
  }

  private boolean hasPublic(ModifiersTree modifiers) {
    return hasModifier(modifiers, Modifier.PUBLIC);
  }

  public boolean isPublicApi(Tree currentParent, Tree tree) {
    if (tree.is(CLASS_KINDS) && (currentParent == null || currentParent.is(PublicApiChecker.CLASS_KINDS))) {
      return isPublicApi((ClassTree) currentParent, (ClassTree) tree);
    } else if (tree.is(METHOD_KINDS)) {
      return isPublicApi((ClassTree) currentParent, (MethodTree) tree);
    } else if (tree.is(Tree.Kind.VARIABLE) && !currentParent.is(METHOD_KINDS)) {
      return isPublicApi((ClassTree) currentParent, (VariableTree) tree);
    }
    return false;
  }

  private boolean hasOverrideAnnotation(MethodTree method) {
    for (AnnotationTree annotationTree : method.modifiers().annotations()) {
      Tree annotationType = annotationTree.annotationType();
      if (annotationType.is(Tree.Kind.IDENTIFIER) && "Override".equals(((IdentifierTree) annotationType).name())) {
        return true;
      }
    }
    return false;
  }

  private boolean isStaticFinal(VariableTree variableTree) {
    ModifiersTree modifiersTree = variableTree.modifiers();
    return hasModifier(modifiersTree, Modifier.STATIC) && hasModifier(modifiersTree, Modifier.FINAL);
  }

  private boolean hasModifier(ModifiersTree modifiersTree, Modifier modifier) {
    return modifiersTree.modifiers().contains(modifier);
  }

  private boolean isEmptyDefaultConstructor(MethodTree constructor) {
    return constructor.is(Tree.Kind.CONSTRUCTOR) && constructor.parameters().size() == 0 && constructor.block().body().size() == 0;
  }

  public String getApiJavadoc(Tree tree) {
    if (tree.is(API_KINDS)) {
      ModifiersTree modifiersTree = null;
      if (tree.is(CLASS_KINDS)) {
        modifiersTree = ((ClassTree) tree).modifiers();
      } else if (tree.is(METHOD_KINDS)) {
        modifiersTree = ((MethodTree) tree).modifiers();
      } else if (tree.is(Tree.Kind.VARIABLE)) {
        modifiersTree = ((VariableTree) tree).modifiers();
      }
      //FIXME token should be retrieved in a much simpler way.
      Tree tokenTree = null;
      if (modifiersTree != null && !(modifiersTree.modifiers().isEmpty() && modifiersTree.annotations().isEmpty())) {
        tokenTree = modifiersTree;
      }
      if (tokenTree == null && tree.is(Tree.Kind.METHOD)) {
        MethodTree methodTree = (MethodTree) tree;
        if (methodTree.typeParameters().isEmpty()) {
          tokenTree = methodTree.returnType();
          while (tokenTree != null && tokenTree.is(Tree.Kind.ARRAY_TYPE, Tree.Kind.PARAMETERIZED_TYPE, Tree.Kind.MEMBER_SELECT)) {
            if (tokenTree.is(Tree.Kind.ARRAY_TYPE)) {
              tokenTree = ((ArrayTypeTree) tokenTree).type();
            } else if (tokenTree.is(Tree.Kind.MEMBER_SELECT)) {
              tokenTree = ((MemberSelectExpressionTree) tokenTree).expression();
            } else if (tokenTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
              tokenTree = ((ParameterizedTypeTree) tokenTree).type();
            }
          }
        } else {
          SyntaxToken syntaxToken = ((TypeParameterListTreeImpl) ((JavaTree) methodTree.typeParameters().get(0)).getAstNode().getParent()).openBracketToken();
          return getCommentFromSyntaxToken(syntaxToken);
        }
      }
      if (tokenTree == null) {
        tokenTree = tree;
      }
      Token token = ((JavaTree) tokenTree).getToken();
      return getCommentFromToken(token);
    }
    return null;
  }

  public String getCommentFromToken(Token token) {
    SyntaxToken syntaxToken = new InternalSyntaxToken(token);
    return getCommentFromSyntaxToken(syntaxToken);
  }

  private String getCommentFromSyntaxToken(SyntaxToken syntaxToken) {
    for (SyntaxTrivia syntaxTrivia : syntaxToken.trivias()) {
      if (syntaxTrivia.comment().startsWith("/**")) {
        return syntaxTrivia.comment();
      }
    }
    return null;
  }
}
