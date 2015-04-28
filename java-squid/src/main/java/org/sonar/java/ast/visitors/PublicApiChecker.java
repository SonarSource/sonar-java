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
import org.sonar.api.utils.ParsingUtils;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
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

  private final Deque<ClassTree> classTrees = new LinkedList<ClassTree>();
  private final Deque<Tree> currentParents = new LinkedList<Tree>();
  private double publicApi;
  private double documentedPublicApi;
  private final boolean separateAccessorsFromMethods;
  private final AccessorVisitor accessorVisitor;

  public static PublicApiChecker newInstanceWithAccessorsHandledAsMethods() {
    return new PublicApiChecker(false);
  }

  public static PublicApiChecker newInstanceWithAccessorsSeparatedFromMethods() {
    return new PublicApiChecker(true);
  }

  private PublicApiChecker(boolean separateAccessorsFromMethods) {
    this.separateAccessorsFromMethods = separateAccessorsFromMethods;
    this.accessorVisitor = new AccessorVisitor();
  }

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

  public boolean isPublicApi(ClassTree currentClass, ClassTree classTree) {
    return (currentClass != null && isPublicInterface(currentClass)) || hasPublic(classTree.modifiers());
  }

  public boolean isPublicApi(ClassTree classTree, MethodTree methodTree) {
    Preconditions.checkNotNull(classTree);
    if (separateAccessorsFromMethods && accessorVisitor.isAccessor(classTree, methodTree)) {
      return false;
    } else if (isPublicInterface(classTree)) {
      return !hasOverrideAnnotation(methodTree);
    } else if (isEmptyDefaultConstructor(methodTree) || hasOverrideAnnotation(methodTree) || classTree.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)) {
      return false;
    }
    return hasPublic(methodTree.modifiers());
  }

  public boolean isPublicApi(ClassTree classTree, VariableTree variableTree) {
    return !isPublicInterface(classTree) && !isStaticFinal(variableTree) && hasPublic(variableTree.modifiers());
  }

  private boolean hasPublic(ModifiersTree modifiers) {
    return hasModifier(modifiers, Modifier.PUBLIC);
  }

  private boolean isPublicInterface(ClassTree currentClass) {
    return currentClass.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE) && !hasModifier(currentClass.modifiers(), Modifier.PRIVATE);
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
    return constructor.is(Tree.Kind.CONSTRUCTOR) && constructor.parameters().isEmpty() && constructor.block().body().isEmpty();
  }

  @Nullable
  public String getApiJavadoc(Tree tree) {
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

  private String getCommentFromMethod(MethodTree methodTree) {
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
      SyntaxToken syntaxToken = ((TypeParameterListTreeImpl) ((JavaTree) methodTree.typeParameters().get(0)).getAstNode().getParent()).openBracketToken();
      return getCommentFromSyntaxToken(syntaxToken);
    }
  }

  private String getCommentFromTree(Tree tokenTree) {
    Token token = ((JavaTree) tokenTree).getToken();
    return getCommentFromToken(token);
  }

  private ModifiersTree getModifierTrees(Tree tree) {
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

  public double getPublicApi() {
    return publicApi;
  }

  public double getUndocumentedPublicApi() {
    return publicApi - documentedPublicApi;
  }

  public double getDocumentedPublicApiDensity() {
    if (Double.doubleToRawLongBits(publicApi) == 0L) {
      return 100.0;
    }
    return ParsingUtils.scaleValue(documentedPublicApi / publicApi * 100, 2);
  }
}
