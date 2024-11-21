/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.UnitTestUtils.COMMON_ASSERTION_MATCHER;

@Rule(key = "S2186")
public class AssertionInThreadRunCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    BlockTree block = methodTree.block();
    if (block != null && isRunMethod(methodTree)) {
      block.accept(new AssertionsVisitor());
    }
  }

  private static boolean isRunMethod(MethodTree methodTree) {
    return methodTree.symbol().owner().type().isSubtypeOf("java.lang.Runnable") && "run".equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty();
  }

  private class AssertionsVisitor extends BaseTreeVisitor {
    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (COMMON_ASSERTION_MATCHER.matches(tree)) {
        reportIssue(ExpressionUtils.methodName(tree), "Remove this assertion.");
      }
      super.visitMethodInvocation(tree);
    }
  }
}
