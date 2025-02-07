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
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S7180")
public class CacheAnnotationsShouldOnlyBeAppliedToConcreteClassesCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "\"@%s\" annotation should only be applied to concrete classes.";
  private static final Set<String> CACHING_ANNOTATIONS = Set.of(
    "org.springframework.cache.annotation.CacheConfig",
    "org.springframework.cache.annotation.CacheEvict",
    "org.springframework.cache.annotation.CachePut",
    "org.springframework.cache.annotation.Cacheable",
    "org.springframework.cache.annotation.Caching");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree anInterface = (ClassTree) tree;

    // report caching annotations on the whole interface
    selectCachingAnnotations(anInterface.modifiers())
      .forEach(ann -> {
        String name = ann.symbolType().name();
        reportIssue(ann, String.format(ISSUE_MESSAGE, name));
      });

    // report caching annotations on interface methods
    Stream<MethodTree> methods = anInterface.members().stream()
      .filter(MethodTree.class::isInstance)
      .map(MethodTree.class::cast);

    methods.forEach(method -> selectCachingAnnotations(method.modifiers())
      .forEach(ann -> {
        String name = ann.symbolType().name();
        reportIssue(ann, String.format(ISSUE_MESSAGE, name));
      }));
  }

  private static Stream<AnnotationTree> selectCachingAnnotations(ModifiersTree m) {
    return m.annotations().stream()
      .filter(ann -> {
        String fullyQualifiedName = ann.annotationType().symbolType().fullyQualifiedName();
        return CACHING_ANNOTATIONS.contains(fullyQualifiedName);
      });
  }

}
