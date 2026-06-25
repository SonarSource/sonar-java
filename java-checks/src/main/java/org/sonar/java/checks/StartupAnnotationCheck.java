/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8911")
public class StartupAnnotationCheck extends IssuableSubscriptionVisitor {
  private static final String STARTUP_FQN = "io.quarkus.runtime.Startup";
  private static final String PRODUCES_FQN = "jakarta.enterprise.inject.Produces";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    var startupAnnotations = getStartupAnnotations(methodTree);

    if (startupAnnotations.isEmpty()) {
      return;
    }

    var secondaryLocations = startupAnnotations.stream()
      .map(annotation ->
        new JavaFileScannerContext.Location("Triggered by this annotation", annotation.annotationType())
      )
      .toList();

    // Only report one issue per method, prioritizing: static > producer > parameters
    if (methodTree.symbol().isStatic()) {
      reportIssue(
        methodTree.simpleName(),
        "\"@Startup\" annotation should not be applied to static methods",
        secondaryLocations,
        null);
      return;
    }

    var producesAnnotations = getProducesAnnotations(methodTree);
    if (!producesAnnotations.isEmpty()) {
      reportIssue(
        methodTree.simpleName(),
        "\"@Startup\" annotation should not be applied to producer methods",
        secondaryLocations,
        null);
      return;
    }

    if (!methodTree.parameters().isEmpty()) {
      reportIssue(
        methodTree.simpleName(),
        "\"@Startup\" annotation should only be applied to no-arg methods",
        secondaryLocations,
        null);
    }
  }

  private static List<AnnotationTree> getStartupAnnotations(MethodTree methodTree) {
    return methodTree.modifiers().annotations().stream()
      .filter(annotation -> annotation.annotationType().symbolType().is(STARTUP_FQN))
      .toList();
  }

  private static List<AnnotationTree> getProducesAnnotations(MethodTree methodTree) {
    return methodTree.modifiers().annotations().stream()
      .filter(annotation -> annotation.annotationType().symbolType().is(PRODUCES_FQN))
      .toList();
  }
}
