/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
import org.sonar.java.metrics.MetricsScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S138")
public class MethodTooBigCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 75;

  @RuleProperty(description = "Maximum authorized lines in a method", defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    BlockTree block = methodTree.block();
    if (block != null) {
      var metricsComputer = ((MetricsScannerContext)context).getMetricsComputer();
      int lines = metricsComputer.getLinesOfCode(block);
      if (lines > max) {
        reportIssue(methodTree.simpleName(),
          "This method has " + lines + " lines, which is greater than the " + max + " lines authorized. Split it into smaller methods.");
      }
    }
  }
}
