/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5786")
public class TestClassAndMethodVisibilityCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    ModifiersTree modifiers;
    Symbol symbol;
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      symbol = methodTree.symbol();
      modifiers = methodTree.modifiers();
    } else {
      ClassTree classTree = (ClassTree) tree;
      symbol = classTree.symbol();
      modifiers = classTree.modifiers();
    }

    if (symbol.metadata().isAnnotatedWith("org.junit.jupiter.api.Test") && !symbol.isPackageVisibility()) {
      Tree questionableNode = modifiers.modifiers().parallelStream()
        .filter(keywordTree -> {
          Modifier modifier = keywordTree.modifier();
          return modifier.equals(Modifier.PUBLIC) || modifier.equals(Modifier.PRIVATE) || modifier.equals(Modifier.PROTECTED);
        })
        .map(modifierKeywordTree -> (Tree) modifierKeywordTree)
        .findFirst()
        .orElse(tree);

      reportIssue(questionableNode, "Remove this access modifier");
    }
  }
}
