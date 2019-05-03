/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.ast.visitors;

import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

public class PublicApiChecker extends BaseTreeVisitor {

  private static final Tree.Kind[] CLASS_KINDS = {
      Tree.Kind.CLASS,
      Tree.Kind.INTERFACE,
      Tree.Kind.ENUM,
      Tree.Kind.ANNOTATION_TYPE
  };

  private static final Tree.Kind[] METHOD_KINDS = {
      Tree.Kind.METHOD,
      Tree.Kind.CONSTRUCTOR
  };

  private static final Tree.Kind[] API_KINDS = {
      Tree.Kind.CLASS,
      Tree.Kind.INTERFACE,
      Tree.Kind.ENUM,
      Tree.Kind.ANNOTATION_TYPE,
      Tree.Kind.METHOD,
      Tree.Kind.CONSTRUCTOR,
      Tree.Kind.VARIABLE
  };

  private final Deque<ClassTree> classTrees = new LinkedList<>();
  private final Deque<Tree> currentParents = new LinkedList<>();
  private int publicApi;
  private int documentedPublicApi;

  public static Kind[] classKinds() {
    return CLASS_KINDS.clone();
  }

  public static Kind[] methodKinds() {
    return METHOD_KINDS.clone();
  }

  public static Kind[] apiKinds() {
    return API_KINDS.clone();
  }

  public void scan(CompilationUnitTree tree) {
    classTrees.clear();
    currentParents.clear();
    publicApi = 0;
    documentedPublicApi = 0;
    super.scan(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    // don't visit anonymous classes, nothing in an anonymous class is part of public api.
  }

  @Override
  public void visitClass(ClassTree tree) {
    visitNode(tree);
    super.visitClass(tree);
    classTrees.pop();
    currentParents.pop();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    visitNode(tree);
    super.visitVariable(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    visitNode(tree);
    super.visitMethod(tree);
    currentParents.pop();
  }

  private void visitNode(Tree tree) {
    Tree currentParent = currentParents.peek();
    if (tree.is(PublicApiChecker.CLASS_KINDS)) {
      classTrees.push((ClassTree) tree);
      currentParents.push(tree);
    } else if (tree.is(PublicApiChecker.METHOD_KINDS)) {
      currentParents.push(tree);
    }

    if (isPublicApi(currentParent, tree)) {
      publicApi++;
      if (getApiJavadoc(tree) != null) {
        documentedPublicApi++;
      }
    }
  }

  public static boolean isPublicApi(@Nullable Tree currentParent, Tree tree) {
    if (currentParent == null) {
      return tree.is(CLASS_KINDS) && isPublicApi((ClassTree) tree);
    } else if (tree.is(CLASS_KINDS) && currentParent.is(PublicApiChecker.CLASS_KINDS)) {
      return isPublicApi((ClassTree) currentParent, (ClassTree) tree);
    } else if (tree.is(METHOD_KINDS)) {
      return isPublicApi((ClassTree) currentParent, (MethodTree) tree);
    } else if (tree.is(Tree.Kind.VARIABLE) && !currentParent.is(METHOD_KINDS)) {
      return isPublicApi((ClassTree) currentParent, (VariableTree) tree);
    }
    return false;
  }

  private static boolean isPublicApi(ClassTree classTree) {
    return isPublicApi(null, classTree);
  }

  private static boolean isPublicApi(@Nullable ClassTree currentClass, ClassTree classTree) {
    return (currentClass != null && isPublicInterface(currentClass)) || hasPublic(classTree.modifiers());
  }

  private static boolean isPublicInterface(ClassTree currentClass) {
    return currentClass.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE) && !ModifiersUtils.hasModifier(currentClass.modifiers(), Modifier.PRIVATE);
  }

  private static boolean hasPublic(ModifiersTree modifiers) {
    return ModifiersUtils.hasModifier(modifiers, Modifier.PUBLIC);
  }

  private static boolean isPublicApi(ClassTree classTree, MethodTree methodTree) {
    Objects.requireNonNull(classTree);
    if (isPublicInterface(classTree)) {
      return !hasOverrideAnnotation(methodTree);
    } else if (isEmptyDefaultConstructor(methodTree)
      || hasOverrideAnnotation(methodTree)
      || classTree.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)
      || constructorOfNonPublicClass(methodTree, classTree)) {
      return false;
    }
    return hasPublic(methodTree.modifiers());
  }

  private static boolean constructorOfNonPublicClass(MethodTree methodTree, ClassTree classTree) {
    return methodTree.is(Kind.CONSTRUCTOR) && !hasPublic(classTree.modifiers());
  }

  private static boolean isEmptyDefaultConstructor(MethodTree constructor) {
    return constructor.is(Tree.Kind.CONSTRUCTOR) && constructor.parameters().isEmpty() && constructor.block().body().isEmpty();
  }

  private static boolean hasOverrideAnnotation(MethodTree method) {
    for (AnnotationTree annotationTree : method.modifiers().annotations()) {
      Tree annotationType = annotationTree.annotationType();
      if (annotationType.is(Tree.Kind.IDENTIFIER) && "Override".equals(((IdentifierTree) annotationType).name())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isPublicApi(ClassTree classTree, VariableTree variableTree) {
    return !isPublicInterface(classTree) && !isStaticFinal(variableTree) && hasPublic(variableTree.modifiers());
  }

  private static boolean isStaticFinal(VariableTree variableTree) {
    ModifiersTree modifiersTree = variableTree.modifiers();
    return ModifiersUtils.hasModifier(modifiersTree, Modifier.STATIC) && ModifiersUtils.hasModifier(modifiersTree, Modifier.FINAL);
  }

  @Nullable
  public static String getApiJavadoc(Tree tree) {
    if (!tree.is(API_KINDS)) {
      return null;
    }
    ModifiersTree modifiersTree = getModifierTrees(tree);
    // FIXME token should be retrieved in a much simpler way.
    if (modifiersTree != null && !(modifiersTree.modifiers().isEmpty() && modifiersTree.annotations().isEmpty())) {
      return getCommentFromTree(modifiersTree);
    }
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      return getCommentFromMethod(methodTree);
    }
    return getCommentFromTree(tree);
  }

  private static String getCommentFromMethod(MethodTree methodTree) {
    if (methodTree.typeParameters().isEmpty()) {
      Tree tokenTree = methodTree.returnType();
      while (tokenTree != null && tokenTree.is(Kind.ARRAY_TYPE, Kind.PARAMETERIZED_TYPE, Kind.MEMBER_SELECT)) {
        if (tokenTree.is(Kind.ARRAY_TYPE)) {
          tokenTree = ((ArrayTypeTree) tokenTree).type();
        } else if (tokenTree.is(Kind.MEMBER_SELECT)) {
          tokenTree = ((MemberSelectExpressionTree) tokenTree).expression();
        } else if (tokenTree.is(Kind.PARAMETERIZED_TYPE)) {
          tokenTree = ((ParameterizedTypeTree) tokenTree).type();
        }
      }
      return getCommentFromTree(tokenTree);
    } else {
      return getCommentFromSyntaxToken(methodTree.typeParameters().openBracketToken());
    }
  }

  private static String getCommentFromTree(Tree tokenTree) {
    return getCommentFromSyntaxToken(tokenTree.firstToken());
  }

  private static ModifiersTree getModifierTrees(Tree tree) {
    ModifiersTree modifiersTree = null;
    if (tree.is(CLASS_KINDS)) {
      modifiersTree = ((ClassTree) tree).modifiers();
    } else if (tree.is(METHOD_KINDS)) {
      modifiersTree = ((MethodTree) tree).modifiers();
    } else if (tree.is(Kind.VARIABLE)) {
      modifiersTree = ((VariableTree) tree).modifiers();
    }
    return modifiersTree;
  }

  private static String getCommentFromSyntaxToken(SyntaxToken syntaxToken) {
    for (SyntaxTrivia syntaxTrivia : syntaxToken.trivias()) {
      if (syntaxTrivia.comment().startsWith("/**")) {
        return syntaxTrivia.comment();
      }
    }
    return null;
  }

  public int getPublicApi() {
    return publicApi;
  }

  public int getUndocumentedPublicApi() {
    return publicApi - documentedPublicApi;
  }
}
