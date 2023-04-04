/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.metrics.MetricsScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6541")
public class BrainMethodCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Refactor this brain method to reduce its complexity.";

  // these default property values are derived from statistics coming from code of almost a hundred projects (Java, C++)
  // "Object-oriented metrics in practice" https://link.springer.com/book/10.1007/3-540-39538-5

  // LOC high threshold for a method, is equal to half the high LOC threshold for classes (130)
  private static final int DEFAULT_LOC_THRESHOLD = 65;
  // High cyclomatic complexity is defined as a ratio of 0.24 per LOC, so DEFAULT_LOC_THRESHOLD * 0.24 = 15 rounded down
  private static final int DEFAULT_CYCLO_THRESHOLD = 15;
  // Deep nesting is defined when nesting level lies within the range 2-5, we picked 3 as default
  private static final int DEFAULT_NESTING_THRESHOLD = 3;
  // Defined as a human short-term memory numeric limit of variables that can be kept in mind
  private static final int DEFAULT_VARIABLES_THRESHOLD = 7;

  @RuleProperty(key = "locThreshold", description = "The maximum number of LOC authorized.", defaultValue = "" + DEFAULT_LOC_THRESHOLD)
  public int locThreshold = DEFAULT_LOC_THRESHOLD;

  @RuleProperty(key = "cyclomaticThreshold", description = "The maximum cyclomatic complexity allowed.", defaultValue = "" + DEFAULT_CYCLO_THRESHOLD)
  public int cyclomaticThreshold = DEFAULT_CYCLO_THRESHOLD;

  @RuleProperty(key = "nestingThreshold", description = "The maximum nesting level allowed.", defaultValue = "" + DEFAULT_NESTING_THRESHOLD)
  public int nestingThreshold = DEFAULT_NESTING_THRESHOLD;

  @RuleProperty(key = "noavThreshold", description = "The maximum number of accessed variables authorized.", defaultValue = "" + DEFAULT_VARIABLES_THRESHOLD)
  public int noavThreshold = DEFAULT_VARIABLES_THRESHOLD;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;

    var metricsComputer = ((MetricsScannerContext) context).getMetricsComputer();

    if (isExcluded(methodTree)) {
      return;
    }

    int cyclomaticComplexity = metricsComputer.getComplexityNodes(methodTree).size();
    int maxNestingLevel = metricsComputer.getMethodNestingLevel(methodTree);
    int linesOfCode = metricsComputer.getLinesOfCode(methodTree.block());
    int numberOfAccessedVariables = metricsComputer.getNumberOfAccessedVariables(methodTree);

    if (linesOfCode >= locThreshold &&
      cyclomaticComplexity >= cyclomaticThreshold &&
      maxNestingLevel >= nestingThreshold &&
      numberOfAccessedVariables >= noavThreshold) {
      reportIssue(methodTree, ISSUE_MESSAGE);
    }

  }

  private static boolean isExcluded(MethodTree methodTree) {
    return methodTree.symbol().isAbstract() || methodTree.block() == null || MethodTreeUtils.isEqualsMethod(methodTree) || MethodTreeUtils.isHashCodeMethod(methodTree);
  }

}
