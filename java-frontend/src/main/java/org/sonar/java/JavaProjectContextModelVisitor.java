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
package org.sonar.java;

import java.util.List;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaProjectContextModelVisitor extends IssuableSubscriptionVisitor {

  private final ProjectContextModel projectContextModel;

  public JavaProjectContextModelVisitor(ProjectContextModel projectContextModel) {
    this.projectContextModel = projectContextModel;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof ClassTreeImpl classTree) {
      visitClass(classTree);
    }
  }

  private void visitClass(ClassTreeImpl classTree) {
    if (classTree.modifiers().annotations().stream()
      .anyMatch(a -> a.symbolType().is("org.springframework.stereotype.Component"))) {
      projectContextModel.springComponents.add(classTree.symbol().type().fullyQualifiedName());
    }
  }

}
