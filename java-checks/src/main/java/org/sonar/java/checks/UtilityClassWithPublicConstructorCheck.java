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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1118")
public class UtilityClassWithPublicConstructorCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!hasSemantic() || !isUtilityClass(classTree) || isPrivateInnerClass(classTree)) {
      return;
    }
    boolean hasImplicitPublicConstructor = true;
    for (MethodTree explicitConstructor : getExplicitConstructors(classTree)) {
      hasImplicitPublicConstructor = false;
      if (isPublicConstructor(explicitConstructor)) {
        reportIssue(explicitConstructor.simpleName(), "Hide this public constructor.");
      }
    }
    if (hasImplicitPublicConstructor) {
      reportIssue(classTree.simpleName(), "Add a private constructor to hide the implicit public one.");
    }
  }

  private static boolean isPrivateInnerClass(ClassTree classTree) {
    return !classTree.symbol().owner().isPackageSymbol() &&
      ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.PRIVATE);
  }

  private static boolean isUtilityClass(ClassTree classTree) {
    return hasOnlyStaticMembers(classTree) && !anonymousClass(classTree) && !extendsAnotherClassOrImplementsSerializable(classTree)
      && !containsMainMethod(classTree);
  }

  private static boolean containsMainMethod(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .anyMatch(method -> MethodTreeUtils.isMainMethod((MethodTree) method));
  }

  private static boolean anonymousClass(ClassTree classTree) {
    return classTree.simpleName() == null;
  }

  private static boolean extendsAnotherClassOrImplementsSerializable(ClassTree classTree) {
    return classTree.superClass() != null || classTree.symbol().type().isSubtypeOf("java.io.Serializable");
  }

  private static boolean hasOnlyStaticMembers(ClassTree classTree) {
    List<Tree> members = classTree.members();
    if (noStaticMember(members)) {
      return false;
    }
    return members.stream().allMatch(member -> isConstructor(member) || isStatic(member) || member.is(Tree.Kind.EMPTY_STATEMENT));
  }

  private static boolean noStaticMember(List<Tree> members) {
    return members.stream().noneMatch(UtilityClassWithPublicConstructorCheck::isStatic);
  }

  private static boolean isStatic(Tree member) {
    if (member.is(Tree.Kind.STATIC_INITIALIZER)) {
      return true;
    } else if (member.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) member;
      return hasStaticModifier(variableTree.modifiers());
    } else if (member.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) member;
      return hasStaticModifier(methodTree.modifiers());
    } else if (isClassTree(member)) {
      ClassTree classTree = (ClassTree) member;
      return hasStaticModifier(classTree.modifiers());
    }
    return false;
  }

  private static boolean isClassTree(Tree member) {
    return member.is(Tree.Kind.CLASS) || member.is(Tree.Kind.ANNOTATION_TYPE) || member.is(Tree.Kind.INTERFACE) || member.is(Tree.Kind.ENUM);
  }

  private static boolean hasStaticModifier(ModifiersTree modifiers) {
    return ModifiersUtils.hasModifier(modifiers, Modifier.STATIC);
  }

  private static List<MethodTree> getExplicitConstructors(ClassTree classTree) {
    ImmutableList.Builder<MethodTree> builder = ImmutableList.builder();
    for (Tree member : classTree.members()) {
      if (isConstructor(member)) {
        builder.add((MethodTree) member);
      }
    }
    return builder.build();
  }

  private static boolean isConstructor(Tree tree) {
    return tree.is(Tree.Kind.CONSTRUCTOR);
  }

  private static boolean isPublicConstructor(Tree tree) {
    return isConstructor(tree) && hasPublicModifier((MethodTree) tree);
  }

  private static boolean hasPublicModifier(MethodTree methodTree) {
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PUBLIC);
  }

}
