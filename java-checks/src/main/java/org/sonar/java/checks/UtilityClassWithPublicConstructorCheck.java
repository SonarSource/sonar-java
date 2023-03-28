/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ClassPattternsUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S1118")
public class UtilityClassWithPublicConstructorCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!ClassPattternsUtils.isUtilityClass(classTree) || ClassPattternsUtils.isPrivateInnerClass(classTree)) {
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

  private static List<MethodTree> getExplicitConstructors(ClassTree classTree) {
    return Collections.unmodifiableList(
      classTree.members().stream()
        .filter(UtilityClassWithPublicConstructorCheck::isConstructor)
        .map(MethodTree.class::cast)
        .collect(Collectors.toList()));
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
