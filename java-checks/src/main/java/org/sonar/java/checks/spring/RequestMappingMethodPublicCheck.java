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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3751")
public class RequestMappingMethodPublicCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  private static final List<String> CONTROLLER_ANNOTATIONS = Arrays.asList(
    SpringUtils.CONTROLLER_ANNOTATION,
    SpringUtils.REST_CONTROLLER_ANNOTATION
  );

  private static final List<String> REQUEST_ANNOTATIONS = Arrays.asList(
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping"
  );

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();

    if (isClassController(methodSymbol)
      && isRequestMappingAnnotated(methodSymbol)
      && methodSymbol.isPrivate()) {
      reportIssue(methodTree.simpleName(), "Make this method non \"private\".");
    }
  }

  private static boolean isClassController(Symbol.MethodSymbol methodSymbol) {
    return CONTROLLER_ANNOTATIONS.stream().anyMatch(methodSymbol.owner().metadata()::isAnnotatedWith);
  }

  private static boolean isRequestMappingAnnotated(Symbol.MethodSymbol methodSymbol) {
    return REQUEST_ANNOTATIONS.stream().anyMatch(methodSymbol.metadata()::isAnnotatedWith);
  }

}
