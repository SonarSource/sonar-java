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
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Methods annotated <code>@Scheduled</code> should not have any arguments,
 * because it causes a runtime exception.
 */
@Rule(key = "S7184")
public class ScheduledOnlyOnNoArgMethodCheck extends IssuableSubscriptionVisitor {
  public static final String SCHEDULED_FQN = "org.springframework.scheduling.annotation.Scheduled";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;

    if (hasParameters(methodTree)) {
      var scheduledAnnotations = getScheduledAnnotations(methodTree);
      if (!scheduledAnnotations.isEmpty()) {
        var secondaryLocations = scheduledAnnotations.stream()
          .map(annotation ->
            new JavaFileScannerContext.Location("Triggered by this annotation", annotation.annotationType()))
          .toList();
        reportIssue(
          methodTree.simpleName(),
          "\"@Scheduled\" annotation should only be applied to no-arg methods",
          secondaryLocations,
          /* remediation cost = */ null);
      }
    }
  }

  private static boolean hasParameters(MethodTree methodTree) {
    return !methodTree.parameters().isEmpty();
  }

  private static List<AnnotationTree> getScheduledAnnotations(MethodTree methodTree) {
    return methodTree.modifiers().annotations().stream()
      .filter(annotation -> annotation.annotationType().symbolType().is(SCHEDULED_FQN))
      .toList();
  }
}
