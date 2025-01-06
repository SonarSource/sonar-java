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
package org.sonar.java.checks.spring;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6810")
public class AsyncMethodsReturnTypeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    var mt = (MethodTree) tree;
    if (mt.symbol().metadata().isAnnotatedWith("org.springframework.scheduling.annotation.Async")) {
      var returnType = mt.returnType();
      // returnType can only be null if the method is a constructor. Since the @Async annotation is not allowed on constructors, and since
      // we hence only visit methods, not constructors, we assume that returnType is not null.
      if (!returnType.symbolType().isVoid() && !returnType.symbolType().isSubtypeOf("java.util.concurrent.Future")) {
        reportIssue(returnType, "Async methods should return 'void' or a 'Future' type.");
      }
    }
  }
}
