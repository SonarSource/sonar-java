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
package org.sonar.java.checks.tests;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Rule(key = "S8745")
public class OneTestLifecycleAnnotationCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> LIFECYCLE_ANNOTATIONS = Set.of(
    "org.junit.jupiter.api.BeforeEach",
    "org.junit.jupiter.api.AfterEach",
    "org.junit.jupiter.api.BeforeAll",
    "org.junit.jupiter.api.AfterAll",
    "org.junit.Before",
    "org.junit.After",
    "org.junit.BeforeClass",
    "org.junit.AfterClass"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    reportIssues(analyzeMethods((ClassTree) tree));
  }

  /**
   * Returns a map from lifecycle method annotations to a set of method names that are annotated with it.
   */
  private static Map<String, List<IdentifierTree>> analyzeMethods(ClassTree classTree) {
    Map<String, List<IdentifierTree>> lifecycleMethods = new LinkedHashMap<>();
    for (Tree member : classTree.members()) {
      if (member instanceof MethodTree methodTree) {
        for (SymbolMetadata.AnnotationInstance annotation: methodTree.symbol().metadata().annotations()) {
          String fqn = annotation.symbol().type().fullyQualifiedName();
          if (LIFECYCLE_ANNOTATIONS.contains(fqn)) {
            lifecycleMethods
              .computeIfAbsent(fqn, k -> new ArrayList<>())
              .add(methodTree.simpleName());
          }
        }
      }
    }
    return lifecycleMethods;
  }

  /**
   * Report issues on methods if lifecycle annotations that are used more than once.
   */
  private void reportIssues(Map<String, List<IdentifierTree>> lifecycleMethods) {
    for (Map.Entry<String, List<IdentifierTree>> ams: lifecycleMethods.entrySet()) {
      if (ams.getValue().size() > 1) {
        String shortAnnotation = ams.getKey().substring(ams.getKey().lastIndexOf('.') + 1);
        String message = "Only one method in a class should be annotated @" + shortAnnotation + ".";

        // If a lifecycle annotation that is used more than once,
        // the first method name is used as primary location, and the others are secondary locations.
        List<IdentifierTree> methods = ams.getValue();

        IdentifierTree primaryLocation = methods.get(0);

        List<JavaFileScannerContext.Location> secondaryLocations = methods.stream()
          .skip(1)
          .map(methodName -> new JavaFileScannerContext.Location("same annotation", methodName))
          .toList();

        reportIssue(primaryLocation, message, secondaryLocations, null);
      }
    }
  }
}
