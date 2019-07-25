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

import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S2039")
public class FieldModifierCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    classTree.members().stream()
      .filter(FieldModifierCheck::isConsentWithCheck)
      .forEach(member -> {
        IdentifierTree simpleName = ((VariableTree) member).simpleName();
        reportIssue(simpleName, "Explicitly declare the visibility for \"" + simpleName.name() + "\".");
      });
  }

  private static boolean isConsentWithCheck(Tree member) {
    return member.is(Tree.Kind.VARIABLE)
      && hasNoVisibilityModifier((VariableTree) member)
      && !isVisibleForTesting((VariableTree) member);
  }

  private static boolean hasNoVisibilityModifier(VariableTree variableTree) {
    ModifiersTree modifiers = variableTree.modifiers();
    return !(ModifiersUtils.hasModifier(modifiers, Modifier.PUBLIC)
      || ModifiersUtils.hasModifier(modifiers, Modifier.PRIVATE)
      || ModifiersUtils.hasModifier(modifiers, Modifier.PROTECTED));
  }

  private static boolean isVisibleForTesting(VariableTree variableTree) {
    return variableTree.modifiers().annotations().stream()
      .anyMatch(annotation -> "VisibleForTesting".equals(annotation.annotationType().lastToken().text()));
  }

}
