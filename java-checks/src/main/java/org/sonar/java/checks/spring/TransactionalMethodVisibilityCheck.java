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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.Version;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2230")
public class TransactionalMethodVisibilityCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware {

  private static final List<String> PROXY_ANNOTATIONS = List.of(
    SpringUtils.TRANSACTIONAL_ANNOTATION,
    SpringUtils.ASYNC_ANNOTATION);

  private static final Map<String, String> ANNOTATION_SHORT_NAMES = Map.of(
    SpringUtils.TRANSACTIONAL_ANNOTATION, "@Transactional",
    SpringUtils.ASYNC_ANNOTATION, "@Async");

  private boolean isSpring6OrLater = false;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    boolean handledBySpring = isSpring6OrLater ? !method.symbol().isPrivate() : method.symbol().isPublic();
    if (!handledBySpring) {
      PROXY_ANNOTATIONS.stream()
        .filter(annSymbol -> hasAnnotation(method, annSymbol))
        .forEach(annSymbol -> reportIssue(
          method.simpleName(),
          "Make this method " + requiredVisibilityMessage() + " or remove the \"" + ANNOTATION_SHORT_NAMES.get(annSymbol) + "\" annotation."));
    }
  }

  private String requiredVisibilityMessage() {
    return isSpring6OrLater ? "non-\"private\"" : "\"public\"";
  }

  private static boolean hasAnnotation(MethodTree method, String annotationSymbol) {
    for (AnnotationTree annotation : method.modifiers().annotations()) {
      if (annotation.symbolType().is(annotationSymbol)) {
        return true;
      }
    }
    return false;
  }

  /** Check that Spring transaction artifact is present, and record whether its version is before or after 6.0. */
  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    Optional<Version> springContextVersion = dependencyFinder.apply("spring-context");
    Optional<Version> springTxVersion = dependencyFinder.apply("spring-tx");
    if (springTxVersion.isEmpty() && springContextVersion.isEmpty()) {
      return false;
    }
    isSpring6OrLater = springContextVersion
      .or(() -> springTxVersion)
      .map(v -> v.isGreaterThanOrEqualTo("6.0"))
      .orElse(false);
    return true;
  }
}
