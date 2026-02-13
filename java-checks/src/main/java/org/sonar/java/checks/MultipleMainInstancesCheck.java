/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8446")
public class MultipleMainInstancesCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.RECORD, Tree.Kind.IMPLICIT_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree ct = (ClassTree) tree;
    List<MethodTree> mainMethods = ct.members().stream()
      .filter(MethodTree.class::isInstance)
      .map(MethodTree.class::cast)
      .filter(this::isMainMethod)
      .toList();
    if (mainMethods.size() > 1) {
      var firstMainMethod = mainMethods.get(0);
      var firstMainMethodToken = firstMainMethod.simpleName();
      reportIssue(firstMainMethodToken, "At most one main method should be defined in a class.");
    }
  }

  private boolean isMainMethod(MethodTree tree) {
    return MethodTreeUtils.isMainMethod(tree, context.getJavaVersion());
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }
}
