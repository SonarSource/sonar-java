/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.design;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.metrics.MetricsScannerContext;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6541")
public class BrainMethodCheck extends IssuableSubscriptionVisitor implements EndOfAnalysis {

  private static final String ISSUE_MESSAGE = "A \"Brain Method\" was detected. Refactor it to reduce at least one of the following metrics: "
    + "LOC from %d to %d, Complexity from %d to %d, Nesting Level from %d to %d, Number of Variables from %d to %d.";

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

  @RuleProperty(key = "locThreshold", description = "The maximum number of LOC allowed.", defaultValue = "" + DEFAULT_LOC_THRESHOLD)
  public int locThreshold = DEFAULT_LOC_THRESHOLD;

  @RuleProperty(key = "cyclomaticThreshold", description = "The maximum cyclomatic complexity allowed.", defaultValue = "" + DEFAULT_CYCLO_THRESHOLD)
  public int cyclomaticThreshold = DEFAULT_CYCLO_THRESHOLD;

  @RuleProperty(key = "nestingThreshold", description = "The maximum nesting level allowed.", defaultValue = "" + DEFAULT_NESTING_THRESHOLD)
  public int nestingThreshold = DEFAULT_NESTING_THRESHOLD;

  @RuleProperty(key = "noavThreshold", description = "The maximum number of accessed variables allowed.", defaultValue = "" + DEFAULT_VARIABLES_THRESHOLD)
  public int noavThreshold = DEFAULT_VARIABLES_THRESHOLD;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD);
  }

  @VisibleForTesting
  int numberOfIssuesToReport = 10;
  private final List<IssueFound> issuesFound = new ArrayList<>();

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

      int brainScore = numberOfAccessedVariables + cyclomaticComplexity + maxNestingLevel * linesOfCode;
      String issueMessage = String.format(ISSUE_MESSAGE,
        linesOfCode, locThreshold - 1,
        cyclomaticComplexity, cyclomaticThreshold - 1,
        maxNestingLevel, nestingThreshold - 1,
        numberOfAccessedVariables, noavThreshold - 1);

      AnalyzerMessage analyzerMessage = new AnalyzerMessage(this, context.getInputFile(),
        AnalyzerMessage.textSpanFor(methodTree.simpleName()), issueMessage, 0);
      issuesFound.add(new IssueFound(brainScore, analyzerMessage));
    }

  }

  private static boolean isExcluded(MethodTree methodTree) {
    return methodTree.symbol().isAbstract() || methodTree.block() == null || MethodTreeUtils.isEqualsMethod(methodTree) || MethodTreeUtils.isHashCodeMethod(methodTree);
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    if (issuesFound.size() > numberOfIssuesToReport) {
      numberOfIssuesToReport += issuesFound.size() / 10;
      issuesFound.sort((a, b) -> b.brainScore - a.brainScore);
    } else {
      numberOfIssuesToReport = issuesFound.size();
    }
    var defaultContext = (DefaultModuleScannerContext) context;
    for (int i = 0; i < numberOfIssuesToReport; i++) {
      IssueFound issueFound = issuesFound.get(i);
      defaultContext.reportIssue(issueFound.analyzerMessage);
    }
  }

  private static class IssueFound {

    int brainScore;
    AnalyzerMessage analyzerMessage;

    public IssueFound(int brainScore, AnalyzerMessage analyzerMessage) {
      this.brainScore = brainScore;
      this.analyzerMessage = analyzerMessage;
    }

  }

}
