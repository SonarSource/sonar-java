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
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S7190")
public class BeforeAndAfterTransactionContractCheck extends IssuableSubscriptionVisitor {

  private static final String BEFORE_TRANSACTION = "org.springframework.test.context.transaction.BeforeTransaction";
  private static final String AFTER_TRANSACTION = "org.springframework.test.context.transaction.AfterTransaction";
  private static final List<String> TRANSACTION_ANNOTATIONS = List.of(BEFORE_TRANSACTION, AFTER_TRANSACTION);

  private static final String RETURN_VOID_MESSAGE = "%s method should return void.";
  private static final String NO_PARAMETERS_MESSAGE = "%s method should not have parameters.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTreeImpl methodTree = (MethodTreeImpl) tree;
    if (methodTree.symbol().metadata().isAnnotatedWith(BEFORE_TRANSACTION)) {
      if (!methodTree.returnType().symbolType().isVoid()) {
        reportReturnType(methodTree, String.format(RETURN_VOID_MESSAGE, "@BeforeTransaction"));
      }
      if (!methodTree.parameters().isEmpty()) {
        reportIssue(methodTree.simpleName(), String.format(NO_PARAMETERS_MESSAGE, "@BeforeTransaction"));
      }
    } else if (methodTree.symbol().metadata().isAnnotatedWith(AFTER_TRANSACTION)) {
      if (!methodTree.returnType().symbolType().isVoid()) {
        reportIssue(methodTree.returnType(), String.format(RETURN_VOID_MESSAGE, "@AfterTransaction"));
      }
      if (!methodTree.parameters().isEmpty()) {
        reportIssue(methodTree.simpleName(), String.format(NO_PARAMETERS_MESSAGE, "@AfterTransaction"));
      }
    }
  }

  private static List<JavaFileScannerContext.Location> getSecondaryLocations(MethodTreeImpl methodTree) {
    return methodTree.modifiers().annotations().stream()
      .filter(annotation -> TRANSACTION_ANNOTATIONS.contains(annotation.symbolType().fullyQualifiedName()))
      .map(annotation -> new JavaFileScannerContext.Location("Annotation", annotation))
      .toList();
  }

  private void reportReturnType(MethodTreeImpl methodTree, String message) {
    reportIssue(methodTree.returnType(), message, getSecondaryLocations(methodTree), null);
  }

  private void reportParameters(MethodTreeImpl methodTree, String message) {
    reportIssue(methodTree.parameters().get(0), message, getSecondaryLocations(methodTree), null);
  }

}
