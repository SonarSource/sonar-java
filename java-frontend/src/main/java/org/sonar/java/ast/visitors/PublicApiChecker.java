/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.java.collections.ListUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class PublicApiChecker {

  private PublicApiChecker() {
    // Utility class
  }

  private static final Tree.Kind[] CLASS_KINDS = {
    Tree.Kind.CLASS,
    Tree.Kind.INTERFACE,
    Tree.Kind.ENUM,
    Tree.Kind.ANNOTATION_TYPE,
    Tree.Kind.RECORD
  };

  private static final Tree.Kind[] METHOD_KINDS = {
    Tree.Kind.METHOD,
    Tree.Kind.CONSTRUCTOR
  };

  private static final Tree.Kind[] API_KINDS = ListUtils.concat(
    Arrays.asList(CLASS_KINDS),
    Arrays.asList(METHOD_KINDS),
    Collections.singletonList(Tree.Kind.VARIABLE)).toArray(new Tree.Kind[0]);

  public static Tree.Kind[] classKinds() {
    return CLASS_KINDS.clone();
  }

  public static Tree.Kind[] methodKinds() {
    return METHOD_KINDS.clone();
  }

  public static Tree.Kind[] apiKinds() {
    return API_KINDS.clone();
  }

  public static boolean isPublicApi(@Nullable Tree currentParent, Tree tree) {
    if (currentParent == null || currentParent.is(Tree.Kind.COMPILATION_UNIT)) {
      return tree.is(CLASS_KINDS) && isPublicApi(null, (ClassTree) tree);
    }
    if (tree.is(CLASS_KINDS) && currentParent.is(PublicApiChecker.CLASS_KINDS)) {
      return isPublicApi((ClassTree) currentParent, (ClassTree) tree);
    }
    if (tree.is(METHOD_KINDS)) {
      return isPublicApi((ClassTree) currentParent, (MethodTree) tree);
    }
    if (tree.is(Tree.Kind.VARIABLE) && !currentParent.is(METHOD_KINDS)) {
      return isPublicApi((ClassTree) currentParent, (VariableTree) tree);
    }
    return false;
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
      return !Boolean.TRUE.equals(methodTree.isOverriding());
    }
    if (isEmptyDefaultConstructor(methodTree)
      || (Boolean.TRUE.equals(methodTree.isOverriding()) && !isDefaultConstructor(methodTree))
      || classTree.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)
      || constructorOfNonPublicClass(methodTree, classTree)) {
      return false;
    }
    return hasPublic(methodTree.modifiers());
  }

  private static boolean constructorOfNonPublicClass(MethodTree methodTree, ClassTree classTree) {
    return methodTree.is(Tree.Kind.CONSTRUCTOR) && !hasPublic(classTree.modifiers());
  }

  private static boolean isEmptyDefaultConstructor(MethodTree methodTree) {
    return isDefaultConstructor(methodTree) && methodTree.block().body().isEmpty();
  }

  private static boolean isDefaultConstructor(MethodTree methodTree) {
    return methodTree.is(Tree.Kind.CONSTRUCTOR) && methodTree.parameters().isEmpty();
  }

  private static boolean isPublicApi(ClassTree classTree, VariableTree variableTree) {
    return !isPublicInterface(classTree) && !isStaticFinal(variableTree) && hasPublic(variableTree.modifiers());
  }

  private static boolean isStaticFinal(VariableTree variableTree) {
    ModifiersTree modifiersTree = variableTree.modifiers();
    return ModifiersUtils.hasModifier(modifiersTree, Modifier.STATIC)
      && ModifiersUtils.hasModifier(modifiersTree, Modifier.FINAL);
  }

  public static Optional<String> getApiJavadoc(Tree tree) {
    if (!tree.is(API_KINDS)) {
      return Optional.empty();
    }
    return tree.firstToken()
      .trivias()
      .stream()
      .map(SyntaxTrivia::comment)
      .filter(PublicApiChecker::isJavadoc)
      .findFirst();
  }

  private static boolean isJavadoc(String comment) {
    return comment.startsWith("/**");
  }
}
