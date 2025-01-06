/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S1165")
public class ExceptionsShouldBeImmutableCheck extends IssuableSubscriptionVisitor {

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (isException(classTree)) {
      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.VARIABLE) && !isFinal((VariableTree) member)) {
          IdentifierTree simpleName = ((VariableTree) member).simpleName();
          reportIssue(simpleName, "Make this \"" + simpleName.name() + "\" field final.");
        }
      }
    }
  }

  private static boolean isFinal(VariableTree member) {
    return ModifiersUtils.hasModifier(member.modifiers(), Modifier.FINAL);
  }

  private static boolean isException(ClassTree classTree) {
    return classTree.simpleName() != null && classTree.symbol().type().isSubtypeOf("java.lang.Throwable");
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }
}
