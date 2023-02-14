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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5993")
public class PublicConstructorInAbstractClassCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.PRIVATE)) {
      return;
    }

    Optional<ModifierKeywordTree> abstractKeyword = ModifiersUtils.findModifier(classTree.modifiers(), Modifier.ABSTRACT);

    abstractKeyword.ifPresent(keyword -> {
      JavaFileScannerContext.Location keywordLocation = new JavaFileScannerContext.Location("This class is \"abstract\".", keyword);
      List<JavaFileScannerContext.Location> secondaryLocations = Collections.singletonList(keywordLocation);
      classTree.members().stream()
        .filter(PublicConstructorInAbstractClassCheck::isConstructor)
        .map(MethodTree.class::cast)
        .map(PublicConstructorInAbstractClassCheck::isPublic)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(modifier -> reportIssue(modifier, "Change the visibility of this constructor to \"protected\".",
          secondaryLocations, null));
    });
  }

  private static Optional<ModifierKeywordTree> isPublic(MethodTree methodTree) {
    return ModifiersUtils.findModifier(methodTree.modifiers(), Modifier.PUBLIC);
  }

  private static boolean isConstructor(Tree member) {
    return member.is(Tree.Kind.CONSTRUCTOR);
  }
}
