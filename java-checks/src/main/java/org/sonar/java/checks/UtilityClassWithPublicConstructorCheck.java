/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1118",
  name = "Utility classes should not have public constructors",
  tags = {"design"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("30min")
public class UtilityClassWithPublicConstructorCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!extendsAnotherClass(classTree) && hasOnlyStaticMembers(classTree)) {
      boolean hasImplicitPublicConstructor = true;
      for (Tree explicitConstructor : getExplicitConstructors(classTree)) {
        hasImplicitPublicConstructor = false;
        if (isPublicConstructor(explicitConstructor)) {
          addIssue(explicitConstructor, "Hide this public constructor.");
        }
      }
      if (hasImplicitPublicConstructor) {
        addIssue(classTree, "Add a private constructor to hide the implicit public one.");
      }
    }
  }

  private static boolean extendsAnotherClass(ClassTree classTree) {
    return classTree.superClass() != null;
  }

  private static boolean hasOnlyStaticMembers(ClassTree classTree) {
    if (classTree.members().isEmpty()) {
      return false;
    }
    for (Tree member : classTree.members()) {
      if (!isConstructor(member) && !isStatic(member) && !member.is(Tree.Kind.EMPTY_STATEMENT)) {
        return false;
      }
    }
    return true;
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

  private static List<Tree> getExplicitConstructors(ClassTree classTree) {
    ImmutableList.Builder<Tree> builder = ImmutableList.builder();
    for (Tree member : classTree.members()) {
      if (isConstructor(member)) {
        builder.add(member);
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
