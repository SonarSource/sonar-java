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

import java.util.Arrays;
import java.util.List;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Shared entry point for checks that inspect {@code Comparable.compareTo} and {@code Comparator.compare}.
 */
abstract class AbstractComparisonMethodCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers COMPARE_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("java.lang.Comparable")
      .names("compareTo")
      .addParametersMatcher(MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.Comparator")
      .names("compare")
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
      .build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      if (COMPARE_METHODS.matches(methodTree) && methodTree.block() != null) {
        visitComparisonMethod(methodTree);
      }
    } else {
      LambdaExpressionTree lambda = (LambdaExpressionTree) tree;
      if (lambda.symbolType().isSubtypeOf("java.util.Comparator")) {
        visitComparatorLambda(lambda);
      }
    }
  }

  abstract void visitComparisonMethod(MethodTree methodTree);

  abstract void visitComparatorLambda(LambdaExpressionTree lambda);

  abstract static class IgnoreNestedTypesVisitor extends BaseTreeVisitor {
    @Override
    public void visitClass(ClassTree tree) {
      // Do not visit inner classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree tree) {
      // Do not visit nested lambdas
    }
  }
}
