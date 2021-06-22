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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6220")
public class SealedFunctionalInterfaceCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this \"sealed\" keyword if this interface is supposed to be functional.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    classTree.modifiers().modifiers().stream()
      .filter(modifierTree -> isSealedModifierOnFunctionalInterface(modifierTree, classTree))
      .forEach(sealedModifierTree -> reportIssue(sealedModifierTree, MESSAGE));
  }

  private static boolean isSealedModifierOnFunctionalInterface(ModifierKeywordTree modifierTree, ClassTree classTree) {
    if (modifierTree.modifier() != Modifier.SEALED || !classTree.superInterfaces().isEmpty()) {
      return false;
    }
    List<Tree> members = classTree.members();
    return members.size() == 1 &&
      members.get(0).is(Tree.Kind.METHOD) &&
      ((MethodTree) members.get(0)).block() == null;
  }

}
