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
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;


@Rule(key = "S7186")
public class UsePageableParameterForPagedQueryCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Add a \"Pageable\" parameter to this method to support pagination.";
  private static final String SPRING_REPOSITORY_FQN = "org.springframework.data.repository.Repository";
  private static final String SPRING_PAGE_FQN = "org.springframework.data.domain.Page";
  private static final String SPRING_SLICE_FQN = "org.springframework.data.domain.Slice";
  private static final String SPRING_PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTreeImpl methodTree = (MethodTreeImpl) tree;
    Symbol.TypeSymbol enclosingClass = methodTree.symbol().enclosingClass();
    if (!enclosingClass.isInterface()) {
      return;
    }
    if (isPageableMethod(methodTree, enclosingClass.type()) && !hasPageableParameter(methodTree)) {
      reportIssue(methodTree, ISSUE_MESSAGE);
    }
  }

  private static boolean hasPageableParameter(MethodTreeImpl method) {
    return method.parameters().stream()
      .map(param -> param.symbol().type())
      .anyMatch(type -> isTypeOrDescendantOf(type, SPRING_PAGEABLE_FQN));
  }

  private static boolean isPageableMethod(MethodTreeImpl method, Type enclosingClassType) {
    return returnsPageOrSlice(method) && isTypeOrDescendantOf(enclosingClassType, SPRING_REPOSITORY_FQN);
  }

  private static boolean returnsPageOrSlice(MethodTreeImpl method) {
    TypeTree returnType = method.returnType();
    // Could not reproduce case where returnType is null
    return returnType.symbolType().is(SPRING_PAGE_FQN) || returnType.symbolType().is(SPRING_SLICE_FQN);
  }

  private static boolean isTypeOrDescendantOf(Type type, String fullyQualifiedName) {
    if (type.is(fullyQualifiedName)) {
      return true;
    }
    for (Type superType : type.symbol().superTypes()) {
      if (isTypeOrDescendantOf(superType, fullyQualifiedName)) {
        return true;
      }
    }
    return false;
  }

}
