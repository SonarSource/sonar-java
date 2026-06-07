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

import java.util.List;
import java.util.Optional;

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.UnitTestUtils.JUNIT5_TEST_ANNOTATIONS;

/**
 * Check that JUnit Jupiter (JUnit 5) tests do not use JUnit 4 assertions.
 */
@Rule(key = "S8715")
public class NoJUnit4AssertionsInJUnit5TestsCheck extends IssuableSubscriptionVisitor {
  private static final String MESSAGE = "JUnit Jupiter tests should not use JUnit 4 assertions.";

  private static final MethodMatchers JUNIT4_ASSERT = MethodMatchers.create()
    .ofTypes("org.junit.Assert")
    .anyName()
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    var mit = (MethodInvocationTree) tree;
    if (JUNIT4_ASSERT.matches(mit)) {
      Optional<AnnotationTree> testAnnotation = getJupiterTestAnnotation(mit);
      testAnnotation.ifPresent(annotationTree -> {
        var secondary = new JavaFileScannerContext.Location("Jupiter test annotation", annotationTree);
        reportIssue(mit.methodSelect(), MESSAGE, List.of(secondary), null);
      });
    }
  }

  /**
   * Return Jupiter's @Test (or similar) annotation if exists. This both tests
   * for the presence of the annotation and returns it, so we can build the secondary location.
   */
  private static Optional<AnnotationTree> getJupiterTestAnnotation(MethodInvocationTree mit) {
    return Optional.ofNullable(ExpressionUtils.getEnclosingMethod(mit))
      .stream()
      .flatMap(enclosingMethod -> enclosingMethod.modifiers().annotations().stream())
      .filter(annotationTree -> JUNIT5_TEST_ANNOTATIONS.contains(annotationTree.symbolType().fullyQualifiedName()))
      .findFirst();
  }
}
