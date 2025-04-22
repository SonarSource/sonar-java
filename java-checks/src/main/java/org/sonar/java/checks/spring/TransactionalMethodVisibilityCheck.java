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
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.Version;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2230")
public class TransactionalMethodVisibilityCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware {

  private static final List<String> proxyAnnotations = List.of(
    "org.springframework.transaction.annotation.Transactional",
    "org.springframework.scheduling.annotation.Async");

  private static final Map<String, String> annShortName = Map.of(
    "org.springframework.transaction.annotation.Transactional", "@Transactional",
    "org.springframework.scheduling.annotation.Async", "@Async");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    if (!method.symbol().isPublic()) {
      proxyAnnotations.stream()
        .filter(annSymbol -> hasAnnotation(method, annSymbol))
        .forEach(annSymbol -> reportIssue(
          method.simpleName(),
          "Make this method \"public\" or remove the \"" + annShortName.get(annSymbol) + "\" annotation."));
    }
  }

  private static boolean hasAnnotation(MethodTree method, String annotationSymbol) {
    for (AnnotationTree annotation : method.modifiers().annotations()) {
      if (annotation.symbolType().is(annotationSymbol)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    return dependencyFinder.apply("spring-tx").map(version -> version.isLowerThan("6.0")).orElse(false);
  }
}
