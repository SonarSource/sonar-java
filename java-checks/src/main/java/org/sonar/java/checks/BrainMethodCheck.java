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

  // this should be equal to HIGH/2, where HIGH is the statistical high threshold for LOC in classes
  private static final int DEFAULT_LOC_THRESHOLD = 59;
  private static final int DEFAULT_CYCLO_THRESHOLD = 4;
  private static final int DEFAULT_NESTING_THRESHOLD = 3;
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
    List<Tree> complexityList = metricsComputer.getComplexityNodes(methodTree);
    int cyclomaticComplexity = complexityList.size();

    int maxNestingLevel = metricsComputer.methodNestingLevel(methodTree);

    int linesOfCode = metricsComputer.linesOfCode(methodTree.block());

    int numberOfAccessedVariables = metricsComputer.getNumberOfAccessedVariables(methodTree);

    if (linesOfCode > locThreshold && cyclomaticComplexity >= cyclomaticThreshold && maxNestingLevel >= nestingThreshold && numberOfAccessedVariables >= noavThreshold) {
      reportIssue(methodTree, ISSUE_MESSAGE);
    }

  }

  private static boolean isExcluded(MethodTree methodTree) {
    return methodTree.symbol().isAbstract() || MethodTreeUtils.isEqualsMethod(methodTree) || MethodTreeUtils.isHashCodeMethod(methodTree);
  }

}
