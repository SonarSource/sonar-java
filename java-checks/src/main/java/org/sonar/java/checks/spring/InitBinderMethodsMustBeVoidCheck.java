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
package org.sonar.java.checks.spring;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S7183")
public class InitBinderMethodsMustBeVoidCheck extends IssuableSubscriptionVisitor {
  private static final String INIT_BINDER = "org.springframework.web.bind.annotation.InitBinder";
  private static final String ISSUE_MESSAGE = "Methods annotated with @InitBinder must return void.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    // if you want to visit constructors, you will need to verify that the returnType is not null
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;

    // returnType is null only for constructors
    TypeTree returnType = method.returnType();
    if (returnType.symbolType().isVoid()) {
      return;
    }

    boolean hasInitBinder = method.modifiers().annotations().stream()
      .anyMatch(ann -> ann.annotationType().symbolType().is(INIT_BINDER));

    if (hasInitBinder) {
      reportIssue(method.simpleName(), ISSUE_MESSAGE);
    }
  }

}
