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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitors.CognitiveComplexityVisitor;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.metrics.MetricsScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S3776")
public class CognitiveComplexityMethodCheck  extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 15;

  @RuleProperty(
          key = "Threshold",
          description = "The maximum authorized complexity.",
          defaultValue = "" + DEFAULT_MAX)
  private int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    if (isExcluded(method)) {
      return;
    }
    var metricsComputer = ((MetricsScannerContext)context).getMetricsComputer();
    CognitiveComplexityVisitor.Result result = metricsComputer.getMethodComplexity(method);
    int total = result.complexity;
    if (total > max) {
      reportIssue(method.simpleName(),
        "Refactor this method to reduce its Cognitive Complexity from " + total + " to the " + max + " allowed.", result.locations, total - max);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }

  private static boolean isExcluded(MethodTree methodTree) {
    return MethodTreeUtils.isEqualsMethod(methodTree) || MethodTreeUtils.isHashCodeMethod(methodTree);
  }
}
