/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

/**
 * Methods marked with Spring <code>@EventListener</code>
 * should have at most one argument.
 */
@Rule(key = "S7185")
public class EventListenerMethodOneArgCheck extends IssuableSubscriptionVisitor {
  static final String EVENT_LISTENER_FQN = "org.springframework.context.event.EventListener";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;

    if (annotatedEventListener(methodTree) && methodTree.parameters().size() > 1) {
      reportIssue(methodTree.simpleName(), "Methods annotated \"@EventListener\" can have at most one argument");
    }
  }

  private static boolean annotatedEventListener(MethodTree methodTree) {
    return methodTree.modifiers().annotations().stream()
      .anyMatch(annotation -> annotation.annotationType().symbolType().is(EVENT_LISTENER_FQN));
  }
}
