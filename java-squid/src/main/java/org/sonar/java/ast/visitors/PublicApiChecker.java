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
import com.google.common.collect.ImmutableList;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

public class PublicApiChecker {

  public boolean isPublicApi(ClassTree classTree) {
    return hasPublic(classTree.modifiers());
  }

  public boolean isPublicApi(ClassTree classTree, MethodTree methodTree) {
    Preconditions.checkNotNull(classTree);
    if (classTree.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)) {
      return true;
    } else if (isEmptyDefaultConstructor(methodTree) || hasOverrideAnnotation(methodTree)) {
      return false;
    }
    return hasPublic(methodTree.modifiers());
  }

  public boolean isPublicApi(VariableTree variableTree) {
    return !isStaticFinal(variableTree) && hasPublic(variableTree.modifiers());
  }


  private boolean hasPublic(ModifiersTree modifiers) {
    return hasModifier(modifiers, Modifier.PUBLIC);
  }

  public boolean isPublicApi(ClassTree currentClass, Tree tree) {
    if(tree.is(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.ANNOTATION_TYPE)) {
      return isPublicApi((ClassTree)tree);
    }else if(tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)){
      return isPublicApi(currentClass, (MethodTree) tree);
    } else if(tree.is(Tree.Kind.VARIABLE)) {
      return isPublicApi((VariableTree) tree);
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


  public static String getApiJavadoc(Tree tree) {
//    AstNode declaration = getDeclaration(astNode);
//    for (Trivia trivia : declaration.getToken().getTrivia()) {
//      if (trivia.isComment()) {
//        String value = trivia.getToken().getOriginalValue();
//        if (value.startsWith("/**")) {
//          return value;
//        }
//      }
//    }
    return null;
  }

private static class VisitTrivia extends SubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TRIVIA);
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {

  }
}
}
