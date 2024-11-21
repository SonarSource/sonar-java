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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1214")
public class InterfaceAsConstantContainerCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!classTree.superInterfaces().isEmpty()) {
      return;
    }
    List<JavaFileScannerContext.Location> constantsLocation = collectConstantsLocation(classTree);
    if (!constantsLocation.isEmpty()) {
      reportIssue(classTree.simpleName(), "Move constants defined in this interfaces to another class or enum.", constantsLocation, null);
    }
  }

  private static List<JavaFileScannerContext.Location> collectConstantsLocation(ClassTree tree) {
    List<JavaFileScannerContext.Location> constantLocations = new ArrayList<>();
    for (Tree member : tree.members()) {
      if (!member.is(Tree.Kind.VARIABLE, Tree.Kind.EMPTY_STATEMENT)) {
        // the interface doesn't hold only constants
        return Collections.emptyList();
      }
      if (member.is(Tree.Kind.EMPTY_STATEMENT)) {
        continue;
      }
      constantLocations.add(new JavaFileScannerContext.Location("", ((VariableTree) member).simpleName()));
    }
    return constantLocations;
  }
}
