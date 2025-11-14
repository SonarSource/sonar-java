/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.metrics.MetricsScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "MethodCyclomaticComplexity", repositoryKey = "squid")
@Rule(key = "S1541")
public class MethodComplexityCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 10;

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
    MethodTree methodTree = (MethodTree) tree;
    if (isExcluded(methodTree)) {
      return;
    }
    var metricsComputer = ((MetricsScannerContext)context).getMetricsComputer();
    List<Tree> complexity = metricsComputer.getComplexityNodes(methodTree);
    int size = complexity.size();
    if (size > max) {
      List<JavaFileScannerContext.Location> flow = new ArrayList<>();
      for (Tree element : complexity) {
        flow.add(new JavaFileScannerContext.Location("+1", element));
      }
      reportIssue(
        methodTree.simpleName(),
        "The Cyclomatic Complexity of this method \"" + methodTree.simpleName().name() + "\" is " + size + " which is greater than " + max + " authorized.",
        flow,
        size - max);
    }
  }

  private static boolean isExcluded(MethodTree methodTree) {
    return MethodTreeUtils.isEqualsMethod(methodTree) || MethodTreeUtils.isHashCodeMethod(methodTree);
  }

  public void setMax(int max) {
    this.max = max;
  }
}
