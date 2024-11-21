/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S2094")
public class EmptyClassCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    IdentifierTree simpleName = classTree.simpleName();
    if (simpleName != null && isNotExtending(classTree) && isEmpty(classTree)) {
      reportIssue(simpleName, "Remove this empty class, write its code or make it an \"interface\".");
    }
  }

  private static boolean isNotExtending(ClassTree tree) {
    return tree.superClass() == null && tree.superInterfaces().isEmpty();
  }

  private static boolean isEmpty(ClassTree tree) {
    return tree.modifiers().annotations().isEmpty()
      && tree.recordComponents().isEmpty()
      && tree.members().stream().allMatch(member -> member.is(Tree.Kind.EMPTY_STATEMENT));
  }
}
